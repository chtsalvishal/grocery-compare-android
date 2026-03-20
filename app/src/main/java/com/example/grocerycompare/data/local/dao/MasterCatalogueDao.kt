package com.example.grocerycompare.data.local.dao

import androidx.room.*
import com.example.grocerycompare.data.local.entity.MasterProductEntity
import com.example.grocerycompare.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterCatalogueDao {
    @Query("SELECT * FROM master_catalogue ORDER BY lastUpdated DESC")
    fun getAllProducts(): Flow<List<MasterProductEntity>>

    @Query("SELECT * FROM master_catalogue")
    suspend fun getAllProductsList(): List<MasterProductEntity>

    @Query("SELECT * FROM master_catalogue WHERE universalName = :name")
    suspend fun getProductByName(name: String): MasterProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: MasterProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<MasterProductEntity>)

    @Query("SELECT * FROM user_entity LIMIT 1")
    fun getUserEntity(): Flow<UserEntity?>

    @Query("SELECT * FROM user_entity LIMIT 1")
    suspend fun getUserEntitySync(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setUserEntity(user: UserEntity)

    @Query("DELETE FROM master_catalogue")
    suspend fun deleteAllProducts()
}
