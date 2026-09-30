package com.example.gpgrocery.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.gpgrocery.data.model.Category
import com.example.gpgrocery.data.model.Payment
import com.example.gpgrocery.data.model.SoldBy
import com.example.gpgrocery.data.model.StockStatus
import com.example.gpgrocery.domain.Pricing

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** When they come, in the store's own words: "Tue and Fri". */
    val deliveryDays: String? = null,
    val phone: String? = null,
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    // Unique, so a scan finds one product; SQLite lets many products have none.
    indices = [Index("barcode", unique = true), Index("supplierId"), Index("category")],
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: Category,
    val soldBy: SoldBy,
    /** Per unit: each, kg or lb. */
    val priceCents: Long,
    val costCents: Long,
    val taxable: Boolean,
    val stockMilli: Long,
    /** Below this, the product counts as running low. */
    val alertBelowMilli: Long,
    val barcode: String? = null,
    val supplierId: Long? = null,
    val photoPath: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val status: StockStatus get() = Pricing.stockStatus(stockMilli, alertBelowMilli)
}

@Entity(tableName = "sales", indices = [Index("createdAt"), Index(value = ["number"], unique = true)])
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The receipt number the customer sees. */
    val number: Long,
    val createdAt: Long,
    val payment: Payment,
    val subtotalCents: Long,
    val taxCents: Long,
    /** The rate charged, so an old receipt still reads right after the rate changes. */
    val taxRateBasisPoints: Int,
    val totalCents: Long,
    val itemCount: Int,
    val cashGivenCents: Long? = null,
)

/**
 * A product as it was sold: its name, price and cost are copied in, so a sale
 * reads the same after the product is renamed, repriced or deleted.
 */
@Entity(
    tableName = "sale_lines",
    foreignKeys = [
        ForeignKey(entity = SaleEntity::class, parentColumns = ["id"], childColumns = ["saleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("saleId"), Index("productId")],
)
data class SaleLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long?,
    val name: String,
    val category: Category,
    val soldBy: SoldBy,
    val quantityMilli: Long,
    val unitPriceCents: Long,
    val unitCostCents: Long,
    val taxable: Boolean,
    val lineTotalCents: Long,
)

@Entity(
    tableName = "restocks",
    foreignKeys = [
        ForeignKey(entity = SupplierEntity::class, parentColumns = ["id"], childColumns = ["supplierId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("receivedAt"), Index("supplierId")],
)
data class RestockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: Long?,
    val supplierName: String,
    val invoiceNumber: String?,
    val receivedAt: Long,
    val totalCostCents: Long,
    /** Lines received, for the activity list. */
    val lineCount: Int,
)

@Entity(
    tableName = "restock_lines",
    foreignKeys = [
        ForeignKey(entity = RestockEntity::class, parentColumns = ["id"], childColumns = ["restockId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("restockId"), Index("productId")],
)
data class RestockLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val restockId: Long,
    val productId: Long?,
    val name: String,
    val soldBy: SoldBy,
    val quantityMilli: Long,
    val unitCostCents: Long,
    val lineTotalCents: Long,
)

data class SaleWithLines(
    @Embedded val sale: SaleEntity,
    @Relation(parentColumn = "id", entityColumn = "saleId") val lines: List<SaleLineEntity>,
)

data class RestockWithLines(
    @Embedded val restock: RestockEntity,
    @Relation(parentColumn = "id", entityColumn = "restockId") val lines: List<RestockLineEntity>,
)

/** A sale line with the time of its sale, for charts and best sellers. */
data class SoldLine(
    @Embedded val line: SaleLineEntity,
    val soldAt: Long,
)

data class LastRestock(
    val receivedAt: Long,
    val quantityMilli: Long,
)
