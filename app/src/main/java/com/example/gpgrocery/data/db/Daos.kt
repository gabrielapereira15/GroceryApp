package com.example.gpgrocery.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun get(id: Long): SupplierEntity?

    @Insert
    suspend fun insert(supplier: SupplierEntity): Long

    @Insert
    suspend fun insertAll(suppliers: List<SupplierEntity>): List<Long>
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observe(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun get(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): ProductEntity?

    @Insert
    suspend fun insert(product: ProductEntity): Long

    @Insert
    suspend fun insertAll(products: List<ProductEntity>): List<Long>

    @Update
    suspend fun update(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun delete(id: Long)

    /** Relative, so two changes at once both count; never below zero. */
    @Query("UPDATE products SET stockMilli = MAX(0, stockMilli + :deltaMilli), updatedAt = :now WHERE id = :id")
    suspend fun adjustStock(id: Long, deltaMilli: Long, now: Long)

    @Query("UPDATE products SET stockMilli = MAX(0, :stockMilli), updatedAt = :now WHERE id = :id")
    suspend fun setStock(id: Long, stockMilli: Long, now: Long)

    @Query("UPDATE products SET costCents = :costCents, updatedAt = :now WHERE id = :id")
    suspend fun updateCost(id: Long, costCents: Long, now: Long)
}

@Dao
interface SaleDao {
    @Insert
    suspend fun insert(sale: SaleEntity): Long

    @Insert
    suspend fun insertLines(lines: List<SaleLineEntity>)

    @Insert
    suspend fun insertAll(sales: List<SaleEntity>): List<Long>

    @Query("SELECT COALESCE(MAX(number), 1000) FROM sales")
    suspend fun lastNumber(): Long

    @Query("SELECT * FROM sales WHERE createdAt >= :from AND createdAt < :to ORDER BY createdAt DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<SaleEntity>>

    @Query(
        "SELECT sale_lines.*, sales.createdAt AS soldAt FROM sale_lines " +
            "JOIN sales ON sales.id = sale_lines.saleId " +
            "WHERE sales.createdAt >= :from AND sales.createdAt < :to",
    )
    fun observeLinesBetween(from: Long, to: Long): Flow<List<SoldLine>>

    @Query(
        "SELECT sale_lines.*, sales.createdAt AS soldAt FROM sale_lines " +
            "JOIN sales ON sales.id = sale_lines.saleId " +
            "WHERE sale_lines.productId = :productId AND sales.createdAt >= :from",
    )
    fun observeProductLinesSince(productId: Long, from: Long): Flow<List<SoldLine>>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :id")
    fun observeWithLines(id: Long): Flow<SaleWithLines?>
}

@Dao
interface RestockDao {
    @Insert
    suspend fun insert(restock: RestockEntity): Long

    @Insert
    suspend fun insertLines(lines: List<RestockLineEntity>)

    @Query("SELECT * FROM restocks WHERE receivedAt >= :from AND receivedAt < :to ORDER BY receivedAt DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<RestockEntity>>

    @Transaction
    @Query("SELECT * FROM restocks WHERE id = :id")
    fun observeWithLines(id: Long): Flow<RestockWithLines?>

    @Query(
        "SELECT restocks.receivedAt AS receivedAt, restock_lines.quantityMilli AS quantityMilli " +
            "FROM restock_lines JOIN restocks ON restocks.id = restock_lines.restockId " +
            "WHERE restock_lines.productId = :productId ORDER BY restocks.receivedAt DESC LIMIT 1",
    )
    fun observeLastFor(productId: Long): Flow<LastRestock?>
}
