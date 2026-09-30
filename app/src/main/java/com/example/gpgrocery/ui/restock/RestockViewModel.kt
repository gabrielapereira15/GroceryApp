package com.example.gpgrocery.ui.restock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpgrocery.AppContainer
import com.example.gpgrocery.data.RestockLine
import com.example.gpgrocery.data.db.ProductEntity
import com.example.gpgrocery.data.db.SupplierEntity
import com.example.gpgrocery.data.model.StockStatus
import com.example.gpgrocery.domain.Money
import com.example.gpgrocery.domain.Pricing
import com.example.gpgrocery.domain.Quantity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A line being filled in: what was typed, not yet parsed. */
data class Draft(val quantity: String, val unitCost: String)

data class DeliveryLine(val product: ProductEntity, val draft: Draft) {
    val quantityMilli: Long? get() = Quantity.parse(draft.quantity, product.soldBy)
    val unitCostCents: Long? get() = Money.parse(draft.unitCost)
    val lineTotalCents: Long get() = Pricing.lineTotal(unitCostCents ?: 0, quantityMilli ?: 0)
}

data class RestockState(
    val loading: Boolean = true,
    val suppliers: List<SupplierEntity> = emptyList(),
    val supplier: SupplierEntity? = null,
    val lines: List<DeliveryLine> = emptyList(),
    val fromSupplier: List<ProductEntity> = emptyList(),
    val everythingElse: List<ProductEntity> = emptyList(),
    val invoice: String = "",
) {
    val totalCents: Long get() = lines.sumOf { it.lineTotalCents }
    val canReceive: Boolean
        get() = supplier != null && lines.isNotEmpty() &&
            lines.all { (it.quantityMilli ?: 0) > 0 && it.unitCostCents != null }
}

class RestockViewModel(
    private val container: AppContainer,
    private val requestedSupplierId: Long,
    private val requestedProductId: Long,
) : ViewModel() {
    private val supplierId = MutableStateFlow<Long?>(null)
    private val drafts = MutableStateFlow<Map<Long, Draft>>(emptyMap())
    private val invoice = MutableStateFlow("")
    private val ready = MutableStateFlow(false)
    val receiving = MutableStateFlow(false)

    val state: StateFlow<RestockState> = combine(
        container.suppliers.suppliers,
        container.products.products,
        combine(supplierId, drafts, ready) { id, lines, isReady -> Triple(id, lines, isReady) },
        invoice,
    ) { suppliers, products, (chosenId, lines, isReady), invoiceText ->
        val byId = products.associateBy { it.id }
        val supplier = suppliers.firstOrNull { it.id == chosenId }
        val onDelivery = lines.mapNotNull { (id, draft) -> byId[id]?.let { DeliveryLine(it, draft) } }
        val notYet = products.filter { it.id !in lines.keys }
        RestockState(
            loading = !isReady,
            suppliers = suppliers,
            supplier = supplier,
            lines = onDelivery,
            fromSupplier = notYet.filter { supplier != null && it.supplierId == supplier.id },
            everythingElse = notYet.filter { supplier == null || it.supplierId != supplier.id },
            invoice = invoiceText,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RestockState())

    init {
        viewModelScope.launch {
            val suppliers = container.suppliers.suppliers.first()
            val products = container.products.products.first()
            val requested = products.firstOrNull { it.id == requestedProductId }
            // The supplier asked for, else the one for the product asked for, else whoever has the most running low.
            val chosen = suppliers.firstOrNull { it.id == requestedSupplierId }
                ?: suppliers.firstOrNull { it.id == requested?.supplierId }
                ?: suppliers.maxByOrNull { supplier ->
                    products.count { it.supplierId == supplier.id && it.status != StockStatus.IN_STOCK }
                }
            chosen?.let { pick(it, products, requested) }
            ready.value = true
        }
    }

    fun choose(supplier: SupplierEntity) {
        viewModelScope.launch { pick(supplier, container.products.products.first(), null) }
    }

    private fun pick(supplier: SupplierEntity, products: List<ProductEntity>, alsoInclude: ProductEntity?) {
        supplierId.value = supplier.id
        val suggested = products.filter { it.supplierId == supplier.id && it.status != StockStatus.IN_STOCK }
        drafts.value = (suggested + listOfNotNull(alsoInclude)).distinctBy { it.id }.associate { it.id to suggestion(it) }
    }

    /** Enough to bring the shelf back to twice its alert level, and never less than one. */
    private fun suggestion(product: ProductEntity): Draft {
        val target = product.alertBelowMilli * 2 - product.stockMilli
        val units = ((target + Quantity.ONE - 1) / Quantity.ONE).coerceAtLeast(1)
        return Draft(units.toString(), Money.plain(product.costCents))
    }

    fun add(product: ProductEntity) = drafts.update { it + (product.id to suggestion(product)) }

    fun remove(productId: Long) = drafts.update { it - productId }

    fun setQuantity(productId: Long, text: String) = drafts.update { lines ->
        lines[productId]?.let { lines + (productId to it.copy(quantity = text)) } ?: lines
    }

    fun setCost(productId: Long, text: String) = drafts.update { lines ->
        lines[productId]?.let { lines + (productId to it.copy(unitCost = text)) } ?: lines
    }

    fun step(line: DeliveryLine, up: Boolean) {
        val now = line.quantityMilli ?: 0
        val next = (now + if (up) Quantity.ONE else -Quantity.ONE).coerceAtLeast(0)
        setQuantity(line.product.id, Quantity.number(next, line.product.soldBy))
    }

    fun setInvoice(text: String) {
        invoice.value = text
    }

    fun addSupplier(name: String, deliveryDays: String) {
        viewModelScope.launch {
            val id = container.suppliers.add(name, deliveryDays)
            val supplier = container.suppliers.suppliers.first().first { it.id == id }
            pick(supplier, container.products.products.first(), null)
        }
    }

    fun receive(onReceived: (restockId: Long, supplierName: String) -> Unit) {
        val current = state.value
        val supplier = current.supplier ?: return
        if (!current.canReceive || receiving.value) return
        receiving.value = true
        viewModelScope.launch {
            try {
                val id = container.restocks.receive(
                    supplier = supplier,
                    invoiceNumber = current.invoice,
                    lines = current.lines.map {
                        RestockLine(
                            productId = it.product.id,
                            name = it.product.name,
                            soldBy = it.product.soldBy,
                            quantityMilli = it.quantityMilli!!,
                            unitCostCents = it.unitCostCents!!,
                        )
                    },
                )
                onReceived(id, supplier.name)
            } finally {
                receiving.value = false
            }
        }
    }
}
