package com.example.gpgrocery.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SupplierEntity::class,
        ProductEntity::class,
        SaleEntity::class,
        SaleLineEntity::class,
        RestockEntity::class,
        RestockLineEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class CrateDatabase : RoomDatabase() {
    abstract fun supplierDao(): SupplierDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun restockDao(): RestockDao

    companion object {
        fun build(context: Context): CrateDatabase =
            Room.databaseBuilder(context, CrateDatabase::class.java, "crate.db").build()
    }
}
