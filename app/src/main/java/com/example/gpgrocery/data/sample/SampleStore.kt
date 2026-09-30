package com.example.gpgrocery.data.sample

import androidx.room.withTransaction
import com.example.gpgrocery.data.db.CrateDatabase
import com.example.gpgrocery.data.db.ProductEntity
import com.example.gpgrocery.data.db.RestockEntity
import com.example.gpgrocery.data.db.RestockLineEntity
import com.example.gpgrocery.data.db.SaleEntity
import com.example.gpgrocery.data.db.SaleLineEntity
import com.example.gpgrocery.data.db.SupplierEntity
import com.example.gpgrocery.data.model.Category
import com.example.gpgrocery.data.model.Category.BAKERY
import com.example.gpgrocery.data.model.Category.DAIRY
import com.example.gpgrocery.data.model.Category.DRINKS
import com.example.gpgrocery.data.model.Category.FROZEN
import com.example.gpgrocery.data.model.Category.HOUSEHOLD
import com.example.gpgrocery.data.model.Category.PANTRY
import com.example.gpgrocery.data.model.Category.PRODUCE
import com.example.gpgrocery.data.model.Category.SNACKS
import com.example.gpgrocery.data.model.Payment
import com.example.gpgrocery.data.model.SoldBy
import com.example.gpgrocery.data.model.SoldBy.EACH
import com.example.gpgrocery.data.model.SoldBy.LB
import com.example.gpgrocery.domain.CartLine
import com.example.gpgrocery.domain.CartMath
import com.example.gpgrocery.domain.Pricing
import com.example.gpgrocery.domain.Quantity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * "Maple Street Market": a corner store with a few weeks of history, so
 * every screen has something true to show the moment the app is opened.
 *
 * The random numbers are seeded, so the store has the same shape on every
 * phone; its dates follow today's, so "today" is always today.
 */
class SampleStore(private val db: CrateDatabase) {

    suspend fun load(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()) {
        val random = Random(SEED)
        // Room clears the tables in a transaction of its own, before the one below fills them.
        db.clearAllTables()
        db.withTransaction {
            val supplierIds = db.supplierDao().insertAll(SUPPLIERS.map { it.entity })
            val supplierIdByKey = SUPPLIERS.map { it.key }.zip(supplierIds).toMap()

            val time = now.toEpochMilli()
            val products = PRODUCTS.map { it.entity(supplierIdByKey.getValue(it.supplier), time) }
            val productIds = db.productDao().insertAll(products)
            val stocked = products.zip(productIds) { product, id -> product.copy(id = id) }

            seedSales(stocked, now, zone, random)
            seedRestocks(stocked, supplierIdByKey, now, zone, random)
        }
    }

    private suspend fun seedSales(products: List<ProductEntity>, now: Instant, zone: ZoneId, random: Random) {
        val weights = products.associate { product -> product.id to PRODUCTS.first { it.name == product.name }.popularity }
        val totalWeight = weights.values.sum()
        val today = now.atZone(zone).toLocalDate()
        val sales = mutableListOf<SaleEntity>()
        val baskets = mutableListOf<List<CartLine>>()
        var number = FIRST_RECEIPT

        for (daysAgo in HISTORY_DAYS downTo 0) {
            val day = today.minusDays(daysAgo.toLong())
            val opens = day.atTime(OPENS_AT, 0).atZone(zone).toInstant().toEpochMilli()
            val closes = day.atTime(CLOSES_AT, 0).atZone(zone).toInstant().toEpochMilli()
            val base = when (day.dayOfWeek) {
                DayOfWeek.SATURDAY -> 64
                DayOfWeek.SUNDAY -> 48
                DayOfWeek.FRIDAY -> 55
                else -> 44
            }
            val count = (base * (0.85 + random.nextDouble() * 0.3)).toInt()
            val times = List(count) { opens + (random.nextDouble() * (closes - opens)).toLong() }
                .filter { it <= now.toEpochMilli() }
                .sorted()

            for (soldAt in times) {
                val basket = pickBasket(products, weights, totalWeight, random)
                val totals = CartMath.totals(basket, TAX_RATE)
                val payment = if (random.nextDouble() < 0.72) Payment.CARD else Payment.CASH
                sales += SaleEntity(
                    number = ++number,
                    createdAt = soldAt,
                    payment = payment,
                    subtotalCents = totals.subtotalCents,
                    taxCents = totals.taxCents,
                    taxRateBasisPoints = TAX_RATE,
                    totalCents = totals.totalCents,
                    itemCount = totals.itemCount,
                    cashGivenCents = if (payment == Payment.CASH) roundUpCash(totals.totalCents) else null,
                )
                baskets += basket
            }
        }

        val saleIds = db.saleDao().insertAll(sales)
        db.saleDao().insertLines(
            saleIds.zip(baskets).flatMap { (saleId, basket) ->
                basket.map {
                    SaleLineEntity(
                        saleId = saleId,
                        productId = it.productId,
                        name = it.name,
                        category = it.category,
                        soldBy = it.soldBy,
                        quantityMilli = it.quantityMilli,
                        unitPriceCents = it.unitPriceCents,
                        unitCostCents = it.unitCostCents,
                        taxable = it.taxable,
                        lineTotalCents = it.lineTotalCents,
                    )
                }
            },
        )
    }

    private fun pickBasket(
        products: List<ProductEntity>,
        weights: Map<Long, Int>,
        totalWeight: Int,
        random: Random,
    ): List<CartLine> {
        val size = BASKET_SIZES[random.nextInt(BASKET_SIZES.size)]
        val chosen = linkedSetOf<ProductEntity>()
        while (chosen.size < size) {
            var roll = random.nextInt(totalWeight)
            val pick = products.first { product ->
                roll -= weights.getValue(product.id)
                roll < 0
            }
            chosen += pick
        }
        return chosen.map { product ->
            val quantity = if (product.soldBy.isWeighed) {
                // 0.50 to 3.20, to the hundredth.
                (50 + random.nextInt(271)) * 10L
            } else {
                Quantity.ofUnits(
                    when (random.nextInt(100)) {
                        in 0..79 -> 1
                        in 80..96 -> 2
                        else -> 3
                    },
                )
            }
            CartLine(
                productId = product.id,
                name = product.name,
                category = product.category,
                soldBy = product.soldBy,
                unitPriceCents = product.priceCents,
                unitCostCents = product.costCents,
                taxable = product.taxable,
                quantityMilli = quantity,
            )
        }
    }

    private suspend fun seedRestocks(
        products: List<ProductEntity>,
        supplierIdByKey: Map<String, Long>,
        now: Instant,
        zone: ZoneId,
        random: Random,
    ) {
        val today = now.atZone(zone).toLocalDate()
        for (supplier in SUPPLIERS) {
            val supplierId = supplierIdByKey.getValue(supplier.key)
            val theirs = products.filter { it.supplierId == supplierId }
            for (weeksAgo in 5 downTo 0) {
                val day = today.minusWeeks(weeksAgo.toLong()).with(supplier.deliveryDay)
                val receivedAt = day.atTime(8, 10 + random.nextInt(40)).atZone(zone).toInstant()
                if (!day.isBefore(today) || receivedAt.isAfter(now)) continue
                val lines = theirs.shuffled(random).take(minOf(theirs.size, 3 + random.nextInt(4))).map { product ->
                    val quantity = if (product.soldBy.isWeighed) (40 + random.nextInt(80)) * Quantity.ONE else (6 + random.nextInt(19)) * Quantity.ONE
                    RestockLineEntity(
                        restockId = 0,
                        productId = product.id,
                        name = product.name,
                        soldBy = product.soldBy,
                        quantityMilli = quantity,
                        unitCostCents = product.costCents,
                        lineTotalCents = Pricing.lineTotal(product.costCents, quantity),
                    )
                }
                val restockId = db.restockDao().insert(
                    RestockEntity(
                        supplierId = supplierId,
                        supplierName = supplier.entity.name,
                        invoiceNumber = supplier.invoicePrefix + "-" + (20_000 + random.nextInt(9_000)),
                        receivedAt = receivedAt.toEpochMilli(),
                        totalCostCents = lines.sumOf { it.lineTotalCents },
                        lineCount = lines.size,
                    ),
                )
                db.restockDao().insertLines(lines.map { it.copy(restockId = restockId) })
            }
        }
    }

    /** Cash customers hand over the next round amount. */
    private fun roundUpCash(totalCents: Long): Long {
        val step = if (totalCents < 2_000) 500L else 1_000L
        return ((totalCents + step - 1) / step) * step
    }

    private class SampleSupplier(val key: String, name: String, deliveryDays: String, val deliveryDay: DayOfWeek, val invoicePrefix: String) {
        val entity = SupplierEntity(name = name, deliveryDays = deliveryDays)
    }

    private class SampleProduct(
        val name: String,
        val category: Category,
        val soldBy: SoldBy,
        val price: Long,
        val cost: Long,
        val taxable: Boolean,
        val stock: Long,
        val alertBelow: Long,
        val supplier: String,
        val barcodeBase: String?,
        val popularity: Int,
    ) {
        fun entity(supplierId: Long, now: Long) = ProductEntity(
            name = name,
            category = category,
            soldBy = soldBy,
            priceCents = price,
            costCents = cost,
            taxable = taxable,
            stockMilli = stock * Quantity.ONE,
            alertBelowMilli = alertBelow * Quantity.ONE,
            barcode = barcodeBase?.let(::upcA),
            supplierId = supplierId,
            createdAt = now,
            updatedAt = now,
        )
    }

    companion object {
        const val STORE_NAME = "Maple Street Market"
        const val OWNER_NAME = "Gabriela"
        const val PIN = "1234"
        const val TAX_RATE = 1300

        private const val SEED = 7
        private const val HISTORY_DAYS = 35
        private const val FIRST_RECEIPT = 1000L
        private const val OPENS_AT = 8
        private const val CLOSES_AT = 20
        private val BASKET_SIZES = intArrayOf(1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 5, 6, 7)

        /** Adds the UPC-A check digit to an 11-digit base. */
        fun upcA(base: String): String {
            require(base.length == 11 && base.all(Char::isDigit))
            val sum = base.mapIndexed { index, c -> (c - '0') * if (index % 2 == 0) 3 else 1 }.sum()
            return base + ((10 - sum % 10) % 10)
        }

        private val SUPPLIERS = listOf(
            SampleSupplier("fields", "Fresh Fields Produce", "Mon, Wed and Fri", DayOfWeek.MONDAY, "FF"),
            SampleSupplier("harmony", "Harmony Dairy Co.", "Tue and Fri", DayOfWeek.TUESDAY, "HD"),
            SampleSupplier("stonebridge", "Stonebridge Bakery", "Every morning", DayOfWeek.WEDNESDAY, "SB"),
            SampleSupplier("northbay", "North Bay Beverages", "Thursdays", DayOfWeek.THURSDAY, "NB"),
            SampleSupplier("pantry", "Pantry Wholesale", "Mondays", DayOfWeek.MONDAY, "PW"),
            SampleSupplier("frost", "Northern Frost Foods", "Wednesdays", DayOfWeek.WEDNESDAY, "NF"),
        )

        // Prices in cents. Basic groceries are zero-rated for HST; snacks,
        // sweets, fizzy drinks and household goods are taxable.
        private val PRODUCTS = listOf(
            SampleProduct("Bananas", PRODUCE, LB, 79, 42, false, 12, 20, "fields", "40111000011", 60),
            SampleProduct("Honeycrisp apples", PRODUCE, LB, 299, 165, false, 60, 20, "fields", "40111000028", 34),
            SampleProduct("Avocados", PRODUCE, EACH, 149, 78, false, 34, 12, "fields", "40111000035", 30),
            SampleProduct("Roma tomatoes", PRODUCE, LB, 199, 95, false, 28, 10, "fields", "40111000042", 26),
            SampleProduct("Baby spinach 142g", PRODUCE, EACH, 449, 240, false, 16, 6, "fields", "06282000142", 18),
            SampleProduct("Yellow onions, 3 lb bag", PRODUCE, EACH, 349, 160, false, 22, 8, "fields", "06282000303", 20),
            SampleProduct("Lemons", PRODUCE, EACH, 89, 38, false, 48, 15, "fields", "40111000059", 16),
            SampleProduct("Carrots, 2 lb bag", PRODUCE, EACH, 299, 130, false, 18, 6, "fields", "06282000202", 17),
            SampleProduct("Whole milk 2L", DAIRY, EACH, 549, 380, false, 4, 10, "harmony", "06263930012", 58),
            SampleProduct("Greek yogurt 750g", DAIRY, EACH, 599, 410, false, 0, 6, "harmony", "06263930750", 24),
            SampleProduct("Free-range eggs, dozen", DAIRY, EACH, 649, 435, false, 5, 12, "harmony", "06263931200", 40),
            SampleProduct("Old cheddar 400g", DAIRY, EACH, 899, 525, false, 22, 6, "harmony", "06263934000", 19),
            SampleProduct("Salted butter 454g", DAIRY, EACH, 679, 460, false, 15, 6, "harmony", "06263934540", 22),
            SampleProduct("Oat milk 1.89L", DAIRY, EACH, 499, 310, false, 0, 6, "harmony", "06263931890", 18),
            SampleProduct("Cream cheese 250g", DAIRY, EACH, 429, 260, false, 11, 4, "harmony", "06263932500", 10),
            SampleProduct("Sourdough loaf", BAKERY, EACH, 699, 320, false, 3, 6, "stonebridge", "06271100017", 40),
            SampleProduct("Multigrain bread", BAKERY, EACH, 449, 210, false, 14, 5, "stonebridge", "06271100024", 22),
            SampleProduct("Croissants, 4 pack", BAKERY, EACH, 599, 290, false, 9, 4, "stonebridge", "06271100031", 16),
            SampleProduct("Bagels, 6 pack", BAKERY, EACH, 499, 230, false, 12, 4, "stonebridge", "06271100048", 14),
            SampleProduct("Basmati rice 2kg", PANTRY, EACH, 899, 540, false, 18, 6, "pantry", "06500120020", 12),
            SampleProduct("Extra virgin olive oil 1L", PANTRY, EACH, 1399, 890, false, 15, 5, "pantry", "06500130010", 10),
            SampleProduct("Spaghetti 900g", PANTRY, EACH, 299, 140, false, 30, 10, "pantry", "06500140900", 18),
            SampleProduct("Chickpeas 540ml", PANTRY, EACH, 179, 85, false, 42, 12, "pantry", "06500150540", 16),
            SampleProduct("Peanut butter 1kg", PANTRY, EACH, 649, 400, false, 14, 5, "pantry", "06500161000", 11),
            SampleProduct("Coffee beans 340g", PANTRY, EACH, 1599, 960, false, 20, 6, "pantry", "06500170340", 13),
            SampleProduct("Maple syrup 540ml", PANTRY, EACH, 1299, 820, false, 10, 4, "pantry", "06500180540", 7),
            SampleProduct("Sparkling water, 12 pack", DRINKS, EACH, 749, 420, true, 4, 8, "northbay", "06744012000", 26),
            SampleProduct("Orange juice 1.75L", DRINKS, EACH, 599, 360, false, 16, 6, "northbay", "06744017500", 18),
            SampleProduct("Cold brew coffee 473ml", DRINKS, EACH, 399, 210, true, 24, 8, "northbay", "07208000123", 20),
            SampleProduct("Kombucha 414ml", DRINKS, EACH, 449, 240, true, 18, 6, "northbay", "06744004140", 12),
            SampleProduct("Frozen peas 750g", FROZEN, EACH, 349, 180, false, 16, 6, "frost", "06790107500", 12),
            SampleProduct("Vanilla ice cream 1.5L", FROZEN, EACH, 699, 390, false, 9, 4, "frost", "06790115000", 11),
            SampleProduct("Margherita pizza", FROZEN, EACH, 799, 450, false, 11, 4, "frost", "06790120000", 12),
            SampleProduct("Tortilla chips 300g", SNACKS, EACH, 449, 230, true, 25, 8, "pantry", "06500203000", 19),
            SampleProduct("Dark chocolate 100g", SNACKS, EACH, 349, 160, true, 40, 10, "pantry", "06500211000", 17),
            SampleProduct("Trail mix 400g", SNACKS, EACH, 799, 460, true, 12, 4, "pantry", "06500224000", 8),
            SampleProduct("Paper towels, 6 rolls", HOUSEHOLD, EACH, 999, 620, true, 12, 4, "pantry", "06500300006", 9),
            SampleProduct("Dish soap 740ml", HOUSEHOLD, EACH, 399, 210, true, 17, 6, "pantry", "06500317400", 8),
            SampleProduct("Laundry detergent 2.95L", HOUSEHOLD, EACH, 1299, 810, true, 8, 3, "pantry", "06500322950", 6),
            SampleProduct("Reusable produce bags", HOUSEHOLD, EACH, 599, 250, true, 20, 5, "pantry", null, 3),
        )
    }
}
