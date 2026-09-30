package com.example.gpgrocery.domain

import com.example.gpgrocery.data.db.ProductEntity
import com.example.gpgrocery.data.db.SaleLineEntity
import com.example.gpgrocery.data.db.SoldLine
import com.example.gpgrocery.data.model.Category
import com.example.gpgrocery.data.model.SoldBy
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class CsvTest {
    private val q = '"'

    @Test
    fun `plain fields are left alone`() {
        assertEquals("Bananas", Csv.field("Bananas"))
    }

    @Test
    fun `commas, quotes and line breaks are quoted`() {
        assertEquals("${q}Eggs, large$q", Csv.field("Eggs, large"))
        assertEquals("${q}12$q$q pizza$q", Csv.field("12$q pizza"))
        assertEquals("${q}two${System.lineSeparator()}lines$q", Csv.field("two${System.lineSeparator()}lines"))
    }

    @Test
    fun `products export one row each, with the supplier by name`() {
        val bananas = ProductEntity(
            name = "Bananas",
            category = Category.PRODUCE,
            soldBy = SoldBy.LB,
            priceCents = 79,
            costCents = 35,
            taxable = false,
            stockMilli = 12_500,
            alertBelowMilli = 15_000,
            barcode = "4011",
            supplierId = 3,
            createdAt = 0,
            updatedAt = 0,
        )
        val chips = bananas.copy(name = "Chips, sea salt", category = Category.SNACKS, soldBy = SoldBy.EACH, taxable = true, barcode = null, supplierId = null)

        val rows = Csv.products(listOf(bananas, chips), mapOf(3L to "Fresh Fields Produce")).lines()

        assertEquals("name,aisle,sold_by,price,cost,taxable,stock,alert_below,barcode,supplier", rows[0])
        assertEquals("Bananas,produce,lb,0.79,0.35,false,12.5,15,4011,Fresh Fields Produce", rows[1])
        assertEquals("${q}Chips, sea salt$q,snacks,each,0.79,0.35,true,12.5,15,,", rows[2])
    }

    @Test
    fun `sales export in local time, oldest first, with the receipt number`() {
        val zone = ZoneId.of("America/Toronto")
        fun at(hour: Int, minute: Int) = LocalDateTime.of(2026, 9, 30, hour, minute, 7, 500_000_000).atZone(zone).toInstant().toEpochMilli()
        fun line(saleId: Long, name: String, soldBy: SoldBy, milli: Long, price: Long) = SaleLineEntity(
            saleId = saleId,
            productId = 1,
            name = name,
            category = Category.PRODUCE,
            soldBy = soldBy,
            quantityMilli = milli,
            unitPriceCents = price,
            unitCostCents = price / 2,
            taxable = false,
            lineTotalCents = Pricing.lineTotal(price, milli),
        )
        val lines = listOf(
            SoldLine(line(10, "Apples", SoldBy.EACH, 3_000, 99), at(11, 5)),
            SoldLine(line(9, "Bananas", SoldBy.LB, 2_400, 79), at(10, 42)),
        )

        val rows = Csv.sales(lines, mapOf(9L to 1047L, 10L to 1048L), zone).lines()

        assertEquals("sold_at,sale,product,aisle,quantity,sold_by,unit_price,line_total,taxable", rows[0])
        assertEquals("2026-09-30T10:42:07,1047,Bananas,produce,2.4,lb,0.79,1.90,false", rows[1])
        assertEquals("2026-09-30T11:05:07,1048,Apples,produce,3,each,0.99,2.97,false", rows[2])
    }
}
