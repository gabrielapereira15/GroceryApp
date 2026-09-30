package com.example.gpgrocery.data

import androidx.room.withTransaction
import com.example.gpgrocery.data.db.CrateDatabase
import com.example.gpgrocery.data.db.LastRestock
import com.example.gpgrocery.data.db.ProductEntity
import com.example.gpgrocery.data.db.RestockEntity
import com.example.gpgrocery.data.db.RestockLineEntity
import com.example.gpgrocery.data.db.RestockWithLines
import com.example.gpgrocery.data.db.SaleEntity
import com.example.gpgrocery.data.db.SaleLineEntity
import com.example.gpgrocery.data.db.SaleWithLines
import com.example.gpgrocery.data.db.SoldLine
import com.example.gpgrocery.data.db.SupplierEntity
import com.example.gpgrocery.data.model.Payment
import com.example.gpgrocery.data.model.SoldBy
import com.example.gpgrocery.domain.Barcodes
import com.example.gpgrocery.domain.CartLine
import com.example.gpgrocery.domain.CartMath
import com.example.gpgrocery.domain.Pricing
import kotlinx.coroutines.flow.Flow

class DuplicateBarcodeException(val existingName: String) : Exception()

class ProductRepository(private val db: CrateDatabase, private val now: () -> Long) {
    private val dao = db.productDao()

    val products: Flow<List<ProductEntity>> = dao.observeAll()

    fun product(id: Long): Flow<ProductEntity?> = dao.observe(id)

    suspend fun get(id: Long): ProductEntity? = dao.get(id)

    suspend fun findByBarcode(barcode: String): ProductEntity? = dao.findByBarcode(Barcodes.normalize(barcode))

    /** Inserts a new product (id 0) or updates an existing one. Returns its id. */
    suspend fun save(product: ProductEntity): Long {
        val barcode = product.barcode?.let(Barcodes::normalize)?.takeIf { it.isNotEmpty() }
        if (barcode != null) {
            val owner = dao.findByBarcode(barcode)
            if (owner != null && owner.id != product.id) throw DuplicateBarcodeException(owner.name)
        }
        val time = now()
        val clean = product.copy(name = product.name.trim(), barcode = barcode, updatedAt = time)
        return if (product.id == 0L) {
            dao.insert(clean.copy(createdAt = time))
        } else {
            dao.update(clean)
            product.id
        }
    }

    suspend fun adjustStock(id: Long, deltaMilli: Long) = dao.adjustStock(id, deltaMilli, now())

    /** Sets the stock to what a count found on the shelf. */
    suspend fun setStock(id: Long, stockMilli: Long) = dao.setStock(id, stockMilli, now())

    suspend fun delete(id: Long) = dao.delete(id)
}

class SupplierRepository(db: CrateDatabase) {
    private val dao = db.supplierDao()

    val suppliers: Flow<List<SupplierEntity>> = dao.observeAll()

    suspend fun add(name: String, deliveryDays: String?): Long =
        dao.insert(SupplierEntity(name = name.trim(), deliveryDays = deliveryDays?.trim()?.takeIf { it.isNotEmpty() }))
}

class SalesRepository(private val db: CrateDatabase, private val now: () -> Long) {
    private val saleDao = db.saleDao()
    private val productDao = db.productDao()

    fun salesBetween(from: Long, to: Long): Flow<List<SaleEntity>> = saleDao.observeBetween(from, to)

    fun linesBetween(from: Long, to: Long): Flow<List<SoldLine>> = saleDao.observeLinesBetween(from, to)

    fun productLinesSince(productId: Long, from: Long): Flow<List<SoldLine>> =
        saleDao.observeProductLinesSince(productId, from)

    fun sale(id: Long): Flow<SaleWithLines?> = saleDao.observeWithLines(id)

    /**
     * Records a sale and takes what was sold off the shelf, in one
     * transaction: a sale is never saved without its stock change, or the
     * other way round. Returns the new sale's id.
     */
    suspend fun checkout(lines: List<CartLine>, payment: Payment, cashGivenCents: Long?, taxRateBasisPoints: Int): Long {
        require(lines.isNotEmpty()) { "A sale needs at least one product" }
        val time = now()
        val totals = CartMath.totals(lines, taxRateBasisPoints)
        return db.withTransaction {
            val saleId = saleDao.insert(
                SaleEntity(
                    number = saleDao.lastNumber() + 1,
                    createdAt = time,
                    payment = payment,
                    subtotalCents = totals.subtotalCents,
                    taxCents = totals.taxCents,
                    taxRateBasisPoints = taxRateBasisPoints,
                    totalCents = totals.totalCents,
                    itemCount = totals.itemCount,
                    cashGivenCents = cashGivenCents.takeIf { payment == Payment.CASH },
                ),
            )
            saleDao.insertLines(
                lines.map {
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
                },
            )
            lines.forEach { productDao.adjustStock(it.productId, -it.quantityMilli, time) }
            saleId
        }
    }
}

/** A product and quantity on a delivery being received. */
data class RestockLine(
    val productId: Long,
    val name: String,
    val soldBy: SoldBy,
    val quantityMilli: Long,
    val unitCostCents: Long,
) {
    val lineTotalCents: Long get() = Pricing.lineTotal(unitCostCents, quantityMilli)
}

class RestockRepository(private val db: CrateDatabase, private val now: () -> Long) {
    private val restockDao = db.restockDao()
    private val productDao = db.productDao()

    fun restocksBetween(from: Long, to: Long): Flow<List<RestockEntity>> = restockDao.observeBetween(from, to)

    fun restock(id: Long): Flow<RestockWithLines?> = restockDao.observeWithLines(id)

    fun lastFor(productId: Long): Flow<LastRestock?> = restockDao.observeLastFor(productId)

    /**
     * Records a delivery, puts it on the shelves and remembers what each
     * product cost this time, so margins follow the latest invoice.
     */
    suspend fun receive(supplier: SupplierEntity, invoiceNumber: String?, lines: List<RestockLine>): Long {
        val wanted = lines.filter { it.quantityMilli > 0 }
        require(wanted.isNotEmpty()) { "A delivery needs at least one product" }
        val time = now()
        return db.withTransaction {
            val restockId = restockDao.insert(
                RestockEntity(
                    supplierId = supplier.id,
                    supplierName = supplier.name,
                    invoiceNumber = invoiceNumber?.trim()?.takeIf { it.isNotEmpty() },
                    receivedAt = time,
                    totalCostCents = wanted.sumOf { it.lineTotalCents },
                    lineCount = wanted.size,
                ),
            )
            restockDao.insertLines(
                wanted.map {
                    RestockLineEntity(
                        restockId = restockId,
                        productId = it.productId,
                        name = it.name,
                        soldBy = it.soldBy,
                        quantityMilli = it.quantityMilli,
                        unitCostCents = it.unitCostCents,
                        lineTotalCents = it.lineTotalCents,
                    )
                },
            )
            wanted.forEach {
                productDao.adjustStock(it.productId, it.quantityMilli, time)
                productDao.updateCost(it.productId, it.unitCostCents, time)
            }
            restockId
        }
    }
}
