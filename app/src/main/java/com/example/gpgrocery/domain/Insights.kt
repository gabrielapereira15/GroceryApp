package com.example.gpgrocery.domain

import com.example.gpgrocery.data.db.SoldLine
import com.example.gpgrocery.data.model.Category
import com.example.gpgrocery.data.model.SoldBy
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class Period { WEEK, MONTH, YEAR }

/** The days a period covers, ending today, and the same span just before it. */
data class Window(val start: LocalDate, val endExclusive: LocalDate, val previousStart: LocalDate)

data class TopProduct(
    val productId: Long?,
    val name: String,
    val category: Category,
    val soldBy: SoldBy,
    val quantityMilli: Long,
    val revenueCents: Long,
)

data class Bucket(val start: LocalDate, val revenueCents: Long)

data class InsightsSummary(
    val revenueCents: Long,
    val previousRevenueCents: Long,
    val costCents: Long,
    val itemsSold: Long,
    val salesCount: Int,
    val buckets: List<Bucket>,
    val byCategory: List<Pair<Category, Long>>,
    val topProducts: List<TopProduct>,
) {
    /** Up or down on the period before, or null when there was nothing to compare with. */
    val changePercent: Double? get() =
        if (previousRevenueCents > 0) (revenueCents - previousRevenueCents) * 100.0 / previousRevenueCents else null

    val marginPercent: Double? get() =
        if (revenueCents > 0) (revenueCents - costCents) * 100.0 / revenueCents else null
}

/**
 * Revenue here is before tax: HST is collected for the government, not
 * earned. Cost is what the goods cost when they were sold, so the margin is
 * the store's gross margin on what actually left the shelves.
 */
object InsightsMath {
    fun window(period: Period, today: LocalDate): Window = when (period) {
        Period.WEEK -> Window(today.minusDays(6), today.plusDays(1), today.minusDays(13))
        Period.MONTH -> Window(today.minusDays(29), today.plusDays(1), today.minusDays(59))
        Period.YEAR -> {
            val start = YearMonth.from(today).minusMonths(11).atDay(1)
            Window(start, today.plusDays(1), start.minusMonths(12))
        }
    }

    fun summarize(
        period: Period,
        today: LocalDate,
        zone: ZoneId,
        lines: List<SoldLine>,
        previousLines: List<SoldLine>,
    ): InsightsSummary {
        val window = window(period, today)
        fun dayOf(line: SoldLine) = Instant.ofEpochMilli(line.soldAt).atZone(zone).toLocalDate()

        val buckets = when (period) {
            Period.YEAR -> (0L..11L).map { offset ->
                val month = YearMonth.from(window.start).plusMonths(offset)
                Bucket(month.atDay(1), lines.filter { YearMonth.from(dayOf(it)) == month }.sumOf { it.line.lineTotalCents })
            }
            else -> {
                val byDay = lines.groupBy(::dayOf)
                generateSequence(window.start) { it.plusDays(1) }
                    .takeWhile { it.isBefore(window.endExclusive) }
                    .map { day -> Bucket(day, byDay[day].orEmpty().sumOf { it.line.lineTotalCents }) }
                    .toList()
            }
        }

        val top = lines.groupBy { it.line.productId ?: -it.line.name.hashCode().toLong() }
            .map { (_, group) ->
                val first = group.first().line
                TopProduct(
                    productId = first.productId,
                    name = first.name,
                    category = first.category,
                    soldBy = first.soldBy,
                    quantityMilli = group.sumOf { it.line.quantityMilli },
                    revenueCents = group.sumOf { it.line.lineTotalCents },
                )
            }
            .sortedByDescending { it.revenueCents }
            .take(5)

        return InsightsSummary(
            revenueCents = lines.sumOf { it.line.lineTotalCents },
            previousRevenueCents = previousLines.sumOf { it.line.lineTotalCents },
            costCents = lines.sumOf { Pricing.lineTotal(it.line.unitCostCents, it.line.quantityMilli) },
            itemsSold = lines.sumOf { if (it.line.soldBy.isWeighed) 1L else it.line.quantityMilli / Quantity.ONE },
            salesCount = lines.map { it.line.saleId }.distinct().size,
            buckets = buckets,
            byCategory = lines.groupBy { it.line.category }
                .map { (category, group) -> category to group.sumOf { it.line.lineTotalCents } }
                .sortedByDescending { it.second },
            topProducts = top,
        )
    }
}
