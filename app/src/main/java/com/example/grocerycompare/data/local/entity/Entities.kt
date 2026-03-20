package com.example.grocerycompare.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "master_catalogue")
data class MasterProductEntity(
    @PrimaryKey val universalName: String,
    val category: String,
    val colesPrice: Double = 0.0,
    val wooliesPrice: Double = 0.0,
    val aldiPrice: Double = 0.0,
    val colesWasPrice: Double = 0.0,
    val wooliesWasPrice: Double = 0.0,
    val aldiWasPrice: Double = 0.0,
    val isSpecial: Boolean = false,
    val imageUrl: String = "",
    val pageNumber: Int? = null,
    val catalogueDate: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_entity")
data class UserEntity(
    @PrimaryKey val id: Int = 0, // Singleton
    val city: String = "Melbourne",
    val postcode: String = "3204",
    val suburb: String = "Bentleigh",
    val preferredStoreId: String? = null,
    val priceAlertThreshold: Double = 0.50
)
