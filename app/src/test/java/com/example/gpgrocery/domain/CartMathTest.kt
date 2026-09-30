package com.example.gpgrocery.domain

import com.example.gpgrocery.data.model.Category
import com.example.gpgrocery.data.model.SoldBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CartMathTest {
    private fun line(
        name: String,
        soldBy: SoldBy,
        price: Long,
        quantityMilli: Long,
        taxable: Boolean = false,
        category: Category = Category.PANTRY,
    ) = CartLine(
        productId = name.hashCode().toLong(),
        name = name,
        category = category,
        soldBy = soldBy,
        unitPriceCents = price,
        unitCostCents = price / 2,
        taxable = taxable,
        quantityMilli = quantityMilli,
    )

    /** The sale on the receipt in the design mockups. */
    private val receipt = listOf(
        line("Bananas", SoldBy.LB, 79, 2_400, category = Category.PRODUCE),
        line("Whole milk 2L", SoldBy.EACH, 549, 2_000, category = Category.DAIRY),
        line("Sourdough loaf", SoldBy.EACH, 699, 1_000, category = Category.BAKERY),
        line("Sparkling water 12pk", SoldBy.EACH, 749, 1_000, taxable = true, category = Category.DRINKS),
    )

    @Test
    fun `adds up the mockup receipt to the cent`() {
        val totals = CartMath.totals(receipt, 1_300)
        assertEquals(2_736L, totals.subtotalCents)
        assertEquals(97L, totals.taxCents)
        assertEquals(2_833L, totals.totalCents)
    }

    @Test
    fun `a weighed product counts as one item`() {
        assertEquals(5, CartMath.totals(receipt, 1_300).itemCount)
    }

    @Test
    fun `tax is only charged on taxable lines`() {
        val groceries = receipt.filterNot { it.taxable }
        assertEquals(0L, CartMath.totals(groceries, 1_300).taxCents)
    }

    @Test
    fun `tax is rounded once on the taxable total, not per line`() {
        // Three taxable lines at 10 cents: 3 x 1.3 cents rounds to 4 cents, not 3 x 1.
        val lines = List(3) { line("Gum $it", SoldBy.EACH, 10, 1_000, taxable = true) }
        assertEquals(4L, CartMath.totals(lines, 1_300).taxCents)
    }

    @Test
    fun `an empty cart is all zeros`() {
        assertEquals(CartTotals(0, 0, 0, 0), CartMath.totals(emptyList(), 1_300))
    }

    @Test
    fun `change is what the cash covers beyond the total`() {
        assertEquals(167L, CartMath.change(2_833, 3_000))
        assertEquals(0L, CartMath.change(2_833, 2_833))
        assertNull(CartMath.change(2_833, 2_000))
        assertNull(CartMath.change(2_833, null))
    }
}
