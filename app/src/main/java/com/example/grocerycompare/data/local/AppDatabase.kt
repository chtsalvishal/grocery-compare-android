package com.example.grocerycompare.data.local

import androidx.room.*
import com.example.grocerycompare.data.local.dao.MasterCatalogueDao
import com.example.grocerycompare.data.local.entity.MasterProductEntity
import com.example.grocerycompare.data.local.entity.UserEntity

@Database(
    entities = [MasterProductEntity::class, UserEntity::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun masterCatalogueDao(): MasterCatalogueDao
}
