package com.example.gpgrocery.ui.sale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpgrocery.AppContainer
import com.example.gpgrocery.data.db.ProductEntity
import com.example.gpgrocery.data.model.Payment
import com.example.gpgrocery.domain.Barcodes
import com.example.gpgrocery.domain.CartLine
import com.example.gpgrocery.domain.CartMath
import com.example.gpgrocery.domain.CartTotals
import com.example.gpgrocery.domain.Money
import com.example.gpgrocery.domain.Quantity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.time.Duration

data class SaleState(
    val loading: Boolean = true,
    val query: String = "",
    val frequent: List<ProductEntity> = emptyList(),
    val results: List<ProductEntity> = emptyList(),
    val lines: List<CartLine> = emptyList(),
    /** productId to quantity, for the badges on the product tiles. */
    val inCart: Map<Long, Long> = emptyMap(),
    val totals: CartTotals = CartTotals(0, 0, 0, 0),
    val taxRateBasisPoints: Int = 1300,
    val payment: Payment = Payment.CARD,
    val cashGiven: String = "",
) {
    val cashGivenCents: Long? get() = Money.parse(cashGiven)
    val change: Long? get() = CartMath.change(totals.totalCents, cashGivenCents)
    val canCharge: Boolean get() = lines.isNotEmpty() && (payment == Payment.CARD || change != null)
}

private data class Tender(val payment: Payment, val cashGiven: String)

class NewSaleViewModel(private val container: AppContainer, preselectedProductId: Long) : ViewModel() {
    private val query = MutableStateFlow("")
    private val cart = MutableStateFlow<Map<Long, Long>>(emptyMap())
    private val tender = MutableStateFlow(Tender(Payment.CARD, ""))

    /** A weighed product waiting for its weight before it goes in the sale. */
    val weighing = MutableStateFlow<ProductEntity?>(null)
    val charging = MutableStateFlow(false)

    private val frequentIds = run {
        val now = container.clock.millis()
        container.sales.linesBetween(now - Duration.ofDays(14).toMillis(), now + 1)
            .map { lines ->
                lines.mapNotNull { it.line.productId }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .map { it.key }
            }
    }

    val state: StateFlow<SaleState> = combine(
        container.products.products,
        frequentIds,
        query,
        cart,
        combine(tender, container.settings.settings) { t, s -> t to s.taxRateBasisPoints },
    ) { products, popular, text, quantities, (tenderNow, taxRate) ->
        val byId = products.associateBy { it.id }
        val lines = quantities.mapNotNull { (id, quantity) -> byId[id]?.toLine(quantity) }
        val needle = fold(text)
        val frequent = (popular.mapNotNull { byId[it] } + products).distinctBy { it.id }.take(FREQUENT)
        SaleState(
            loading = false,
            query = text,
            frequent = frequent,
            results = if (needle.isEmpty()) emptyList() else products.filter {
                fold(it.name).contains(needle) || it.barcode?.contains(Barcodes.normalize(text)) == true
            }.take(40),
            lines = lines,
            inCart = quantities,
            totals = CartMath.totals(lines, taxRate),
            taxRateBasisPoints = taxRate,
            payment = tenderNow.payment,
            cashGiven = tenderNow.cashGiven,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SaleState())

    init {
        if (preselectedProductId != 0L) {
            viewModelScope.launch { container.products.get(preselectedProductId)?.let(::add) }
        }
    }

    fun search(text: String) {
        query.value = text
    }

    /** A tap on a product: one more of it, or the weight dialog for weighed goods. */
    fun add(product: ProductEntity) {
        if (product.soldBy.isWeighed) {
            weighing.value = product
        } else {
            cart.update { it.plusQuantity(product.id, Quantity.ONE) }
        }
    }

    fun addWeighed(product: ProductEntity, weightMilli: Long) {
        weighing.value = null
        if (weightMilli > 0) cart.update { it.plusQuantity(product.id, weightMilli) }
    }

    fun cancelWeighing() {
        weighing.value = null
    }

    fun increase(productId: Long) = cart.update { it.plusQuantity(productId, Quantity.ONE) }

    fun decrease(productId: Long) = cart.update { it.plusQuantity(productId, -Quantity.ONE) }

    fun remove(productId: Long) = cart.update { it - productId }

    fun clear() {
        cart.value = emptyMap()
        tender.value = Tender(Payment.CARD, "")
    }

    fun pay(payment: Payment) = tender.update { it.copy(payment = payment) }

    fun cashGiven(text: String) = tender.update { it.copy(cashGiven = text) }

    /** A scanned code: added if it is in the store, otherwise [onUnknown] is told which code. */
    fun scanned(barcode: String, onAdded: (String) -> Unit, onUnknown: (String) -> Unit) {
        viewModelScope.launch {
            val product = container.products.findByBarcode(barcode)
            if (product == null) {
                onUnknown(barcode)
            } else {
                add(product)
                if (!product.soldBy.isWeighed) onAdded(product.name)
            }
        }
    }

    fun charge(onSold: (Long) -> Unit) {
        val current = state.value
        if (!current.canCharge || charging.value) return
        charging.value = true
        viewModelScope.launch {
            try {
                val saleId = container.sales.checkout(
                    lines = current.lines,
                    payment = current.payment,
                    cashGivenCents = current.cashGivenCents,
                    taxRateBasisPoints = current.taxRateBasisPoints,
                )
                cart.value = emptyMap()
                onSold(saleId)
            } finally {
                charging.value = false
            }
        }
    }

    private fun Map<Long, Long>.plusQuantity(productId: Long, delta: Long): Map<Long, Long> {
        val next = (this[productId] ?: 0) + delta
        return if (next <= 0) this - productId else this + (productId to next)
    }

    private fun ProductEntity.toLine(quantity: Long) = CartLine(
        productId = id,
        name = name,
        category = category,
        soldBy = soldBy,
        unitPriceCents = priceCents,
        unitCostCents = costCents,
        taxable = taxable,
        quantityMilli = quantity,
    )

    private fun fold(text: String): String =
        Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    private companion object {
        const val FREQUENT = 9
    }
}
