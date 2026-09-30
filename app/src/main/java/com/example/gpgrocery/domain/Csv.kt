package com.example.gpgrocery.domain

import com.example.gpgrocery.data.db.ProductEntity
import com.example.gpgrocery.data.db.SoldLine
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Spreadsheet exports, for the accountant or a backup. RFC 4180 quoting. */
object Csv {
    fun field(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

    private fun row(vararg values: String) = values.joinToString(",") { field(it) }

    private fun dollars(cents: Long) = BigDecimal.valueOf(cents, 2).toPlainString()

    private fun quantity(milli: Long) = BigDecimal.valueOf(milli, 3).stripTrailingZeros().toPlainString()

    fun products(products: List<ProductEntity>, supplierNames: Map<Long, String>): String = buildString {
        appendLine(row("name", "aisle", "sold_by", "price", "cost", "taxable", "stock", "alert_below", "barcode", "supplier"))
        products.forEach {
            appendLine(
                row(
                    it.name,
                    it.category.name.lowercase(),
                    it.soldBy.name.lowercase(),
                    dollars(it.priceCents),
                    dollars(it.costCents),
                    it.taxable.toString(),
                    quantity(it.stockMilli),
                    quantity(it.alertBelowMilli),
                    it.barcode.orEmpty(),
                    it.supplierId?.let(supplierNames::get).orEmpty(),
                ),
            )
        }
    }

    fun sales(lines: List<SoldLine>, receiptNumbers: Map<Long, Long>, zone: ZoneId): String = buildString {
        val format = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        appendLine(row("sold_at", "sale", "product", "aisle", "quantity", "sold_by", "unit_price", "line_total", "taxable"))
        lines.sortedBy { it.soldAt }.forEach {
            appendLine(
                row(
                    Instant.ofEpochMilli(it.soldAt).atZone(zone).toLocalDateTime().withNano(0).format(format),
                    receiptNumbers[it.line.saleId]?.toString().orEmpty(),
                    it.line.name,
                    it.line.category.name.lowercase(),
                    quantity(it.line.quantityMilli),
                    it.line.soldBy.name.lowercase(),
                    dollars(it.line.unitPriceCents),
                    dollars(it.line.lineTotalCents),
                    it.line.taxable.toString(),
                ),
            )
        }
    }
}
