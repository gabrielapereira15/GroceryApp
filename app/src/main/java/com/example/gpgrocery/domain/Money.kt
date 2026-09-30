package com.example.gpgrocery.domain

import com.example.gpgrocery.data.model.SoldBy
import com.example.gpgrocery.data.model.StockStatus
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Amounts are whole cents in a Long everywhere; only the screen turns them
 * into dollars. Floating point never touches a total, so $0.10 + $0.20 is
 * always $0.30 and a day's takings add up to the cent.
 */
object Money {
    private val currency = ThreadLocal.withInitial {
        NumberFormat.getCurrencyInstance(Locale.CANADA).apply { currency = java.util.Currency.getInstance("CAD") }
    }

    fun format(cents: Long): String = currency.get()!!.format(BigDecimal.valueOf(cents, 2))

    /** "$1.3k" for chart labels, where a full amount would not fit. */
    fun compact(cents: Long): String {
        val dollars = cents / 100.0
        return when {
            dollars >= 1_000_000 -> "$" + trim(dollars / 1_000_000) + "M"
            dollars >= 1_000 -> "$" + trim(dollars / 1_000) + "k"
            else -> "$" + dollars.toLong()
        }
    }

    private fun trim(value: Double): String =
        BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    /**
     * Reads what someone typed in a price field: "3.99", "$3.99", " 3,99 ".
     * Null for anything that is not a non-negative amount with at most two
     * decimals.
     */
    fun parse(text: String): Long? {
        val cleaned = text.trim().removePrefix("$").trim().replace(',', '.')
        if (cleaned.isEmpty() || !cleaned.matches(Regex("""\d+(\.\d{0,2})?"""))) return null
        return BigDecimal(cleaned).movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
    }

    /** Price as it is typed back into a field: 399 -> "3.99". */
    fun plain(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString()
}

/** Quantities are thousandths of a unit (see [SoldBy]). */
object Quantity {
    const val ONE: Long = 1_000

    fun ofUnits(units: Long): Long = units * ONE

    /** "3", "2.40", as a number only; the unit is added by the caller's words. */
    fun number(milli: Long, soldBy: SoldBy): String =
        if (soldBy.isWeighed) BigDecimal.valueOf(milli, 3).setScale(2, RoundingMode.HALF_UP).toPlainString()
        else BigDecimal.valueOf(milli, 3).setScale(0, RoundingMode.DOWN).toPlainString()

    /** Reads a typed quantity: whole units for counted products, up to three decimals for weighed ones. */
    fun parse(text: String, soldBy: SoldBy): Long? {
        val cleaned = text.trim().replace(',', '.')
        val pattern = if (soldBy.isWeighed) Regex("""\d+(\.\d{0,3})?""") else Regex("""\d+""")
        if (cleaned.isEmpty() || !cleaned.matches(pattern)) return null
        return BigDecimal(cleaned).movePointRight(3).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
    }
}

object Pricing {
    /** What a quantity costs at a unit price, to the nearest cent (half up). */
    fun lineTotal(unitPriceCents: Long, quantityMilli: Long): Long =
        BigDecimal.valueOf(unitPriceCents)
            .multiply(BigDecimal.valueOf(quantityMilli))
            .divide(BigDecimal.valueOf(Quantity.ONE), 0, RoundingMode.HALF_UP)
            .longValueExact()

    /**
     * Tax on the taxable part of a sale, in basis points (1300 = 13% HST),
     * rounded once on the total rather than line by line, the way a till does.
     */
    fun tax(taxableCents: Long, rateBasisPoints: Int): Long =
        BigDecimal.valueOf(taxableCents)
            .multiply(BigDecimal.valueOf(rateBasisPoints.toLong()))
            .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.HALF_UP)
            .longValueExact()

    /** Gross margin as a percentage of the selling price, or null when there is no price. */
    fun marginPercent(priceCents: Long, costCents: Long): Double? =
        if (priceCents <= 0) null else (priceCents - costCents) * 100.0 / priceCents

    fun stockStatus(stockMilli: Long, alertBelowMilli: Long): StockStatus = when {
        stockMilli <= 0 -> StockStatus.OUT
        stockMilli < alertBelowMilli -> StockStatus.LOW
        else -> StockStatus.IN_STOCK
    }
}
