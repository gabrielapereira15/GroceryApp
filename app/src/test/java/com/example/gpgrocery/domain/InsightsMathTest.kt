package com.example.gpgrocery.domain

import com.example.gpgrocery.data.db.SaleLineEntity
import com.example.gpgrocery.data.db.SoldLine
import com.example.gpgrocery.data.model.Category
import com.example.gpgrocery.data.model.SoldBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class InsightsMathTest {
    private val zone = ZoneId.of("America/Toronto")
    private val today = LocalDate.of(2026, 9, 30)

    private fun sold(
        day: LocalDate,
        saleId: Long,
        productId: Long,
        name: String,
        category: Category,
        soldBy: SoldBy,
        quantityMilli: Long,
        price: Long,
        cost: Long,
        time: LocalTime = LocalTime.of(10, 30),
    ) = SoldLine(
        SaleLineEntity(
            saleId = saleId,
            productId = productId,
            name = name,
            category = category,
            soldBy = soldBy,
            quantityMilli = quantityMilli,
            unitPriceCents = price,
            unitCostCents = cost,
            taxable = false,
            lineTotalCents = Pricing.lineTotal(price, quantityMilli),
        ),
        day.atTime(time).atZone(zone).toInstant().toEpochMilli(),
    )

    private fun bananas(day: LocalDate, saleId: Long, milli: Long = 2_400) =
        sold(day, saleId, 1, "Bananas", Category.PRODUCE, SoldBy.LB, milli, price = 79, cost = 35)

    private fun milk(day: LocalDate, saleId: Long, milli: Long) =
        sold(day, saleId, 2, "Whole milk 2L", Category.DAIRY, SoldBy.EACH, milli, price = 549, cost = 390)

    @Test
    fun `a week is the last seven days including today`() {
        val window = InsightsMath.window(Period.WEEK, today)
        assertEquals(LocalDate.of(2026, 9, 24), window.start)
        assertEquals(LocalDate.of(2026, 10, 1), window.endExclusive)
        assertEquals(LocalDate.of(2026, 9, 17), window.previousStart)
    }

    @Test
    fun `a month is thirty days, compared with the thirty before`() {
        val window = InsightsMath.window(Period.MONTH, today)
        assertEquals(LocalDate.of(2026, 9, 1), window.start)
        assertEquals(LocalDate.of(2026, 8, 2), window.previousStart)
    }

    @Test
    fun `a year is twelve whole months ending with this one`() {
        val window = InsightsMath.window(Period.YEAR, today)
        assertEquals(LocalDate.of(2025, 10, 1), window.start)
        assertEquals(LocalDate.of(2024, 10, 1), window.previousStart)
    }

    @Test
    fun `sums a week of sales`() {
        val lines = listOf(
            bananas(today, saleId = 1),
            milk(today, saleId = 1, milli = 2_000),
            milk(today.minusDays(2), saleId = 2, milli = 1_000),
        )
        val previous = listOf(milk(today.minusDays(10), saleId = 3, milli = 2_000))

        val summary = InsightsMath.summarize(Period.WEEK, today, zone, lines, previous)

        assertEquals(190L + 1_098L + 549L, summary.revenueCents)
        assertEquals(84L + 780L + 390L, summary.costCents)
        // The bananas count once however much they weighed.
        assertEquals(4L, summary.itemsSold)
        assertEquals(2, summary.salesCount)
        assertEquals((1_837 - 1_098) * 100.0 / 1_098, summary.changePercent!!, 0.0001)
        assertEquals((1_837 - 1_254) * 100.0 / 1_837, summary.marginPercent!!, 0.0001)
    }

    @Test
    fun `one bar per day, empty days included`() {
        val lines = listOf(bananas(today, 1), milk(today.minusDays(2), 2, 1_000))
        val buckets = InsightsMath.summarize(Period.WEEK, today, zone, lines, emptyList()).buckets

        assertEquals(7, buckets.size)
        assertEquals(LocalDate.of(2026, 9, 24), buckets.first().start)
        assertEquals(listOf(0L, 0L, 0L, 0L, 549L, 0L, 190L), buckets.map { it.revenueCents })
    }

    @Test
    fun `a sale late in the evening counts on its local day`() {
        // 11:30 PM in Toronto is already tomorrow in UTC.
        val late = sold(today.minusDays(1), 1, 1, "Bananas", Category.PRODUCE, SoldBy.LB, 1_000, 79, 35, LocalTime.of(23, 30))
        val buckets = InsightsMath.summarize(Period.WEEK, today, zone, listOf(late), emptyList()).buckets
        assertEquals(79L, buckets[5].revenueCents)
        assertEquals(0L, buckets[6].revenueCents)
    }

    @Test
    fun `a year has one bar per month`() {
        val lines = listOf(bananas(LocalDate.of(2025, 10, 3), 1), milk(today, 2, 1_000))
        val buckets = InsightsMath.summarize(Period.YEAR, today, zone, lines, emptyList()).buckets

        assertEquals(12, buckets.size)
        assertEquals(LocalDate.of(2025, 10, 1), buckets.first().start)
        assertEquals(LocalDate.of(2026, 9, 1), buckets.last().start)
        assertEquals(190L, buckets.first().revenueCents)
        assertEquals(549L, buckets.last().revenueCents)
    }

    @Test
    fun `aisles and products are ranked by revenue`() {
        val lines = listOf(bananas(today, 1), milk(today, 1, 2_000), milk(today, 2, 1_000))
        val summary = InsightsMath.summarize(Period.WEEK, today, zone, lines, emptyList())

        assertEquals(listOf(Category.DAIRY to 1_647L, Category.PRODUCE to 190L), summary.byCategory)
        assertEquals(listOf("Whole milk 2L", "Bananas"), summary.topProducts.map { it.name })
        assertEquals(3_000L, summary.topProducts.first().quantityMilli)
    }

    @Test
    fun `no change is shown without an earlier period to compare with`() {
        val summary = InsightsMath.summarize(Period.WEEK, today, zone, listOf(bananas(today, 1)), emptyList())
        assertNull(summary.changePercent)
    }

    @Test
    fun `no margin is shown without sales`() {
        val summary = InsightsMath.summarize(Period.WEEK, today, zone, emptyList(), emptyList())
        assertNull(summary.marginPercent)
        assertEquals(0, summary.salesCount)
    }
}
