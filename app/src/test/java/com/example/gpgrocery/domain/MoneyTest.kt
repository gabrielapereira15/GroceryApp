package com.example.gpgrocery.domain

import com.example.gpgrocery.data.model.SoldBy
import com.example.gpgrocery.data.model.StockStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun `formats cents as Canadian dollars`() {
        assertEquals("$28.33", Money.format(2833))
        assertEquals("$0.05", Money.format(5))
        assertEquals("$1,234.56", Money.format(123_456))
    }

    @Test
    fun `compact amounts fit under a chart bar`() {
        assertEquals("$999", Money.compact(99_999))
        assertEquals("$1k", Money.compact(100_000))
        assertEquals("$1.3k", Money.compact(130_000))
        assertEquals("$2.5M", Money.compact(250_000_000))
    }

    @Test
    fun `reads prices the way people type them`() {
        assertEquals(399L, Money.parse("3.99"))
        assertEquals(399L, Money.parse("$3.99"))
        assertEquals(399L, Money.parse(" 3,99 "))
        assertEquals(300L, Money.parse("3"))
        assertEquals(350L, Money.parse("3.5"))
    }

    @Test
    fun `rejects what is not a price`() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("-1"))
        assertNull(Money.parse("3.999"))
        assertNull(Money.parse("1.2.3"))
    }

    @Test
    fun `plain amounts go back into a field`() {
        assertEquals("3.99", Money.plain(399))
        assertEquals("0.05", Money.plain(5))
    }
}

class QuantityTest {
    @Test
    fun `weighed quantities show two decimals, counted ones none`() {
        assertEquals("2.40", Quantity.number(2_400, SoldBy.LB))
        assertEquals("0.35", Quantity.number(345, SoldBy.KG))
        assertEquals("3", Quantity.number(3_000, SoldBy.EACH))
    }

    @Test
    fun `reads a typed quantity`() {
        assertEquals(2_400L, Quantity.parse("2.4", SoldBy.LB))
        assertEquals(1_235L, Quantity.parse("1,235", SoldBy.KG))
        assertEquals(3_000L, Quantity.parse("3", SoldBy.EACH))
        assertNull(Quantity.parse("2.4", SoldBy.EACH))
        assertNull(Quantity.parse("1.2345", SoldBy.KG))
        assertNull(Quantity.parse("", SoldBy.EACH))
    }
}

class PricingTest {
    @Test
    fun `line totals round to the nearest cent`() {
        // 2.40 lb of bananas at $0.79/lb is $1.896.
        assertEquals(190L, Pricing.lineTotal(79, 2_400))
        assertEquals(1_098L, Pricing.lineTotal(549, 2_000))
        assertEquals(1L, Pricing.lineTotal(1, 500))
        assertEquals(0L, Pricing.lineTotal(1, 499))
    }

    @Test
    fun `thirteen percent HST, rounded half up`() {
        assertEquals(97L, Pricing.tax(749, 1_300))
        assertEquals(65L, Pricing.tax(500, 1_300))
        assertEquals(0L, Pricing.tax(0, 1_300))
        assertEquals(0L, Pricing.tax(749, 0))
    }

    @Test
    fun `margin is on the selling price`() {
        assertEquals(40.0, Pricing.marginPercent(1_000, 600)!!, 0.0001)
        assertEquals(-10.0, Pricing.marginPercent(1_000, 1_100)!!, 0.0001)
        assertNull(Pricing.marginPercent(0, 100))
    }

    @Test
    fun `stock status follows the alert level`() {
        assertEquals(StockStatus.OUT, Pricing.stockStatus(0, 5_000))
        assertEquals(StockStatus.OUT, Pricing.stockStatus(-250, 0))
        assertEquals(StockStatus.LOW, Pricing.stockStatus(4_999, 5_000))
        assertEquals(StockStatus.IN_STOCK, Pricing.stockStatus(5_000, 5_000))
        assertEquals(StockStatus.IN_STOCK, Pricing.stockStatus(1, 0))
    }
}
