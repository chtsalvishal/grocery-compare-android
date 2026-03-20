package com.example.grocerycompare.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.grocerycompare.data.local.entity.MasterProductEntity
import com.example.grocerycompare.data.local.entity.UserEntity
import com.example.grocerycompare.data.repository.MasterCatalogueRepository
import com.example.grocerycompare.data.source.remote.ScrapeProgress
import com.example.grocerycompare.data.source.remote.StoreStatus
import com.example.grocerycompare.data.source.remote.SyncWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*

data class HomeUiState(
    val products: List<MasterProductEntity> = emptyList(),
    val categories: List<String> = listOf("All"),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val progress: ScrapeProgress? = null,
    val postcode: String = "3204",
    val city: String = "Melbourne",
    val suburb: String = "Bentleigh",
    val selectedStores: Set<String> = setOf("Coles", "Woolies", "Aldi"),
    val sortOrder: String = "multi_store",
    val syncError: String? = null
)

class HomeViewModel(
    application: Application,
    private val repository: MasterCatalogueRepository
) : AndroidViewModel(application) {

    private val _isRefreshing = MutableStateFlow(false)
    private val _scrapeProgress = MutableStateFlow<ScrapeProgress?>(null)
    private val _selectedCategory = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")
    private val _selectedStores = MutableStateFlow(setOf("Coles", "Woolies", "Aldi"))
    private val _sortOrder = MutableStateFlow("multi_store")
    private val _syncError = MutableStateFlow<String?>(null)

    private val _userEntity = repository.getUserEntity()
        .map { it ?: UserEntity() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserEntity())

    val uiState: StateFlow<HomeUiState> = combine(
        repository.products,
        _isRefreshing,
        _scrapeProgress,
        _selectedCategory,
        _searchQuery,
        _userEntity,
        _selectedStores,
        _sortOrder,
        _syncError
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val products = args[0] as List<MasterProductEntity>
        val refreshing = args[1] as Boolean
        val progress = args[2] as? ScrapeProgress
        val selectedCat = args[3] as String
        val query = args[4] as String
        val user = args[5] as UserEntity
        @Suppress("UNCHECKED_CAST")
        val stores = args[6] as Set<String>
        val sort = args[7] as String
        val error = args[8] as? String

        val categoriesList = listOf("All") + products.map { it.category }.distinct().sorted()

        val filtered = products.filter { product ->
            val matchesCategory = (selectedCat == "All" || product.category == selectedCat)
            val matchesQuery = (query.isEmpty() || product.universalName.contains(query, ignoreCase = true))
            val hasSelectedStorePrice = (
                (stores.contains("Coles") && product.colesPrice > 0) ||
                (stores.contains("Woolies") && product.wooliesPrice > 0) ||
                (stores.contains("Aldi") && product.aldiPrice > 0)
            )
            matchesCategory && matchesQuery && hasSelectedStorePrice
        }

        // Only consider prices from stores the user has selected
        fun activePrices(p: MasterProductEntity): List<Double> = buildList {
            if (stores.contains("Coles") && p.colesPrice > 0) add(p.colesPrice)
            if (stores.contains("Woolies") && p.wooliesPrice > 0) add(p.wooliesPrice)
            if (stores.contains("Aldi") && p.aldiPrice > 0) add(p.aldiPrice)
        }
        fun minPrice(p: MasterProductEntity) = activePrices(p).minOrNull() ?: Double.MAX_VALUE
        fun maxSaving(p: MasterProductEntity): Double {
            val pairs = buildList {
                if (stores.contains("Coles") && p.colesPrice > 0) add(p.colesPrice to p.colesWasPrice)
                if (stores.contains("Woolies") && p.wooliesPrice > 0) add(p.wooliesPrice to p.wooliesWasPrice)
                if (stores.contains("Aldi") && p.aldiPrice > 0) add(p.aldiPrice to p.aldiWasPrice)
            }
            return pairs.filter { (now, was) -> now > 0 && was > now }
                .maxOfOrNull { (now, was) -> was - now } ?: 0.0
        }
        fun storeCount(p: MasterProductEntity) = activePrices(p).size

        val sorted = when (sort) {
            "price_asc" -> filtered.sortedWith(compareBy({ minPrice(it) }, { it.universalName.lowercase() }))
            "price_desc" -> filtered.sortedWith(compareByDescending<MasterProductEntity> { minPrice(it) }.thenBy { it.universalName.lowercase() })
            "savings" -> filtered.sortedWith(compareByDescending<MasterProductEntity> { maxSaving(it) }.thenBy { it.universalName.lowercase() })
            "az" -> filtered.sortedBy { it.universalName.lowercase() }
            else -> filtered.sortedWith(compareByDescending<MasterProductEntity> { storeCount(it) }.thenBy { it.universalName.lowercase() })
        }

        HomeUiState(
            products = sorted,
            categories = categoriesList,
            selectedCategory = selectedCat,
            searchQuery = query,
            isRefreshing = refreshing,
            progress = progress,
            postcode = user.postcode,
            city = user.city,
            suburb = user.suburb,
            selectedStores = stores,
            sortOrder = sort,
            syncError = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        viewModelScope.launch {
            repository.initializeUserIfEmpty()
        }
    }

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }

    fun onCategorySelected(category: String) { _selectedCategory.value = category }

    fun setSortOrder(order: String) { _sortOrder.value = order }

    fun toggleStoreSelection(store: String) {
        val current = _selectedStores.value.toMutableSet()
        if (current.contains(store)) {
            if (current.size > 1) current.remove(store)
        } else {
            current.add(store)
        }
        _selectedStores.value = current
    }

    fun triggerScrape() {
        val workManager = WorkManager.getInstance(getApplication())
        val user = _userEntity.value

        _isRefreshing.value = true
        _syncError.value = null

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(workDataOf(
                "SUBURB" to user.suburb,
                "POSTCODE" to user.postcode,
                "CITY" to user.city
            ))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("grocery_sync")
            .build()

        workManager.enqueueUniqueWork(
            "grocery_sync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )

        viewModelScope.launch {
            // 10-minute hard timeout so the UI never stays frozen
            withTimeoutOrNull(10 * 60 * 1000L) {
                workManager.getWorkInfosByTagLiveData("grocery_sync").asFlow()
                    .collect { workInfos ->
                        val info = workInfos.firstOrNull() ?: return@collect

                        val statusText = info.progress.getString("STATUS")
                            ?: info.outputData.getString("STATUS")
                            ?: "Syncing..."
                        val progressPct = info.progress.getInt("PROGRESS", 0)
                        val count = info.outputData.getInt("COUNT", 0)

                        _scrapeProgress.value = ScrapeProgress(
                            message = statusText,
                            current = progressPct,
                            total = 100,
                            storeStatuses = listOf(
                                StoreStatus(name = "Coles", isDone = info.state == WorkInfo.State.SUCCEEDED, itemsFound = count / 3),
                                StoreStatus(name = "Woolworths", isDone = info.state == WorkInfo.State.SUCCEEDED, itemsFound = count / 3),
                                StoreStatus(name = "Aldi", isDone = info.state == WorkInfo.State.SUCCEEDED, itemsFound = count / 3)
                            ),
                            isComplete = info.state.isFinished
                        )

                        if (info.state == WorkInfo.State.SUCCEEDED) {
                            _isRefreshing.value = false
                        } else if (info.state == WorkInfo.State.FAILED) {
                            _isRefreshing.value = false
                            _syncError.value = info.outputData.getString("STATUS") ?: "Sync failed"
                        }
                    }
            }
            // Timeout hit — unblock the UI
            _isRefreshing.value = false
        }
    }

    class Factory(
        private val application: Application,
        private val repository: MasterCatalogueRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(application, repository) as T
    }
}
