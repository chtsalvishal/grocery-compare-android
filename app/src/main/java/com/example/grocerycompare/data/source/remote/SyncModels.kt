package com.example.grocerycompare.data.source.remote

data class StoreStatus(
    val name: String,
    val isDone: Boolean = false,
    val error: Boolean = false,
    val itemsFound: Int = 0
)

data class ScrapeProgress(
    val message: String,
    val current: Int,
    val total: Int,
    val storeStatuses: List<StoreStatus> = emptyList(),
    val isComplete: Boolean = false
)
