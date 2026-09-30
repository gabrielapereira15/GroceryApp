package com.example.gpgrocery.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpgrocery.AppContainer
import com.example.gpgrocery.data.db.LastRestock
import com.example.gpgrocery.data.db.ProductEntity
import com.example.gpgrocery.data.db.SupplierEntity
import com.example.gpgrocery.domain.Quantity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class DaySold(val date: LocalDate, val quantityMilli: Long)

data class ProductState(
    val loading: Boolean = true,
    val product: ProductEntity? = null,
    val supplier: SupplierEntity? = null,
    val lastWeek: List<DaySold> = emptyList(),
    val lastRestock: LastRestock? = null,
) {
    val soldThisWeekMilli: Long get() = lastWeek.sumOf { it.quantityMilli }
}

class ProductViewModel(private val container: AppContainer, private val productId: Long) : ViewModel() {
    private val clock = container.clock
    private val today = LocalDate.now(clock)
    private val weekStart = today.minusDays(6)

    val state: StateFlow<ProductState> = combine(
        container.products.product(productId),
        container.suppliers.suppliers,
        container.sales.productLinesSince(productId, weekStart.atStartOfDay(clock.zone).toInstant().toEpochMilli()),
        container.restocks.lastFor(productId),
    ) { product, suppliers, lines, lastRestock ->
        val byDay = lines.groupBy { Instant.ofEpochMilli(it.soldAt).atZone(clock.zone).toLocalDate() }
        ProductState(
            loading = false,
            product = product,
            supplier = suppliers.firstOrNull { it.id == product?.supplierId },
            lastWeek = (0L..6L).map { offset ->
                val day = weekStart.plusDays(offset)
                DaySold(day, byDay[day].orEmpty().sumOf { it.line.quantityMilli })
            },
            lastRestock = lastRestock,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductState())

    /** One unit on or off the shelf: a carton, a kilo, a pound. */
    fun nudge(up: Boolean) {
        viewModelScope.launch { container.products.adjustStock(productId, if (up) Quantity.ONE else -Quantity.ONE) }
    }

    fun delete(onDeleted: (String) -> Unit) {
        val product = state.value.product ?: return
        viewModelScope.launch {
            container.products.delete(product.id)
            container.photos.remove(product.photoPath)
            onDeleted(product.name)
        }
    }
}
