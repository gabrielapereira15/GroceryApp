package com.example.gpgrocery.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpgrocery.AppContainer
import com.example.gpgrocery.data.db.ProductEntity
import com.example.gpgrocery.data.db.SaleEntity
import com.example.gpgrocery.data.db.SoldLine
import com.example.gpgrocery.data.model.Category
import com.example.gpgrocery.data.model.SoldBy
import com.example.gpgrocery.data.model.StockStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

data class BestSeller(
    val productId: Long?,
    val name: String,
    val category: Category,
    val soldBy: SoldBy,
    val quantityMilli: Long,
    val revenueCents: Long,
)

data class HomeState(
    val loading: Boolean = true,
    val todayCents: Long = 0,
    val salesCount: Int = 0,
    val itemsSold: Int = 0,
    /** Against the same weekday last week, up to the same time of day. Null with nothing to compare. */
    val changePercent: Double? = null,
    val comparedDay: DayOfWeek = DayOfWeek.MONDAY,
    val sparkline: List<Long> = emptyList(),
    val lowStock: List<ProductEntity> = emptyList(),
    val bestSellers: List<BestSeller> = emptyList(),
    val productCount: Int = 0,
) {
    val averageBasketCents: Long get() = if (salesCount == 0) 0 else todayCents / salesCount
}

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val clock: Clock = container.clock
    private val today = LocalDate.now(clock)
    private val dayStart = today.atStartOfDay(clock.zone).toInstant().toEpochMilli()
    private val dayEnd = today.plusDays(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()
    private val lastWeekStart = today.minusWeeks(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()
    private val lastWeekEnd = today.minusWeeks(1).plusDays(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()

    val state: StateFlow<HomeState> = combine(
        container.sales.salesBetween(dayStart, dayEnd),
        container.sales.linesBetween(dayStart, dayEnd),
        container.sales.salesBetween(lastWeekStart, lastWeekEnd),
        container.products.products,
    ) { todaySales, todayLines, lastWeekSales, products ->
        val now = clock.millis()
        val todayCents = todaySales.sumOf { it.totalCents }
        val lastWeekSoFar = lastWeekSales.filter { it.createdAt <= now - WEEK_MILLIS }.sumOf { it.totalCents }
        HomeState(
            loading = false,
            todayCents = todayCents,
            salesCount = todaySales.size,
            itemsSold = todaySales.sumOf { it.itemCount },
            changePercent = if (lastWeekSoFar > 0) (todayCents - lastWeekSoFar) * 100.0 / lastWeekSoFar else null,
            comparedDay = today.dayOfWeek,
            sparkline = sparkline(todaySales, now),
            lowStock = products
                .filter { it.status != StockStatus.IN_STOCK }
                .sortedWith(compareBy<ProductEntity> { it.status != StockStatus.OUT }.thenBy { it.stockMilli.toDouble() / it.alertBelowMilli.coerceAtLeast(1) }),
            bestSellers = bestSellers(todayLines),
            productCount = products.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    /** Running total every half hour from opening until now, for the hero card's line. */
    private fun sparkline(sales: List<SaleEntity>, now: Long): List<Long> {
        val opening = today.atTime(OPENS_AT).atZone(clock.zone).toInstant().toEpochMilli()
        if (now <= opening) return listOf(0, 0)
        val step = Duration.ofMinutes(30).toMillis()
        val sorted = sales.sortedBy { it.createdAt }
        val points = mutableListOf<Long>()
        var cursor = opening
        while (cursor < now) {
            points += sorted.filter { it.createdAt <= cursor }.sumOf { it.totalCents }
            cursor += step
        }
        points += sorted.sumOf { it.totalCents }
        return if (points.size < 2) listOf(0L) + points else points
    }

    private fun bestSellers(lines: List<SoldLine>): List<BestSeller> =
        lines.groupBy { it.line.productId ?: -it.line.name.hashCode().toLong() }
            .map { (_, group) ->
                val first = group.first().line
                BestSeller(
                    productId = first.productId,
                    name = first.name,
                    category = first.category,
                    soldBy = first.soldBy,
                    quantityMilli = group.sumOf { it.line.quantityMilli },
                    revenueCents = group.sumOf { it.line.lineTotalCents },
                )
            }
            .sortedByDescending { it.revenueCents }
            .take(3)

    /** Looks a scanned barcode up and says which product it belongs to, if any. */
    fun lookup(barcode: String, onResult: (Long?) -> Unit) {
        viewModelScope.launch { onResult(container.products.findByBarcode(barcode)?.id) }
    }

    fun lock() = container.session.lock()

    private companion object {
        val OPENS_AT: LocalTime = LocalTime.of(8, 0)
        val WEEK_MILLIS = Duration.ofDays(7).toMillis()
    }
}
