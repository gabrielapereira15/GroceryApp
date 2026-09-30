package com.example.gpgrocery.domain

import com.example.gpgrocery.data.model.Category
import com.example.gpgrocery.data.model.SoldBy

/** One product in the sale being rung up. */
data class CartLine(
    val productId: Long,
    val name: String,
    val category: Category,
    val soldBy: SoldBy,
    val unitPriceCents: Long,
    val unitCostCents: Long,
    val taxable: Boolean,
    val quantityMilli: Long,
) {
    val lineTotalCents: Long get() = Pricing.lineTotal(unitPriceCents, quantityMilli)
}

data class CartTotals(
    val subtotalCents: Long,
    val taxCents: Long,
    val totalCents: Long,
    /** How many things are in the bag: a weighed product counts once, however much of it. */
    val itemCount: Int,
)

object CartMath {
    fun totals(lines: List<CartLine>, taxRateBasisPoints: Int): CartTotals {
        val subtotal = lines.sumOf { it.lineTotalCents }
        val taxable = lines.filter { it.taxable }.sumOf { it.lineTotalCents }
        val tax = Pricing.tax(taxable, taxRateBasisPoints)
        val items = lines.sumOf { if (it.soldBy.isWeighed) 1 else (it.quantityMilli / Quantity.ONE).toInt() }
        return CartTotals(subtotal, tax, subtotal + tax, items)
    }

    /** Change to hand back, or null while the cash given does not cover the total. */
    fun change(totalCents: Long, cashGivenCents: Long?): Long? =
        cashGivenCents?.takeIf { it >= totalCents }?.minus(totalCents)
}
