package com.example.grocerycompare.data.repository

import com.example.grocerycompare.data.local.dao.MasterCatalogueDao
import com.example.grocerycompare.data.local.entity.MasterProductEntity
import com.example.grocerycompare.data.local.entity.UserEntity
import com.example.grocerycompare.util.FuzzyMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber

class MasterCatalogueRepository(private val dao: MasterCatalogueDao) {

    val products: Flow<List<MasterProductEntity>> = dao.getAllProducts()

    suspend fun initializeUserIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.getUserEntitySync() == null) {
            dao.setUserEntity(UserEntity())
            Timber.d("Initialized default UserEntity")
        }
    }

    /**
     * Saves products to the database using an improved fuzzy matching algorithm.
     * This merges prices for identical products found across different retailers.
     */
    suspend fun saveScrapedProducts(scrapedProducts: List<MasterProductEntity>) = withContext(Dispatchers.IO) {
        try {
            val dbProducts = dao.getAllProductsList()
            val mergedCatalogue = dbProducts.associateBy { it.universalName }.toMutableMap()
            
            Timber.d("Syncing ${scrapedProducts.size} live items with DB")
            
            scrapedProducts.forEach { scraped ->
                // Unit-aware fuzzy matching: products with different units are never merged
                // (e.g. "Milk 1L" and "Milk 2L" must remain separate)
                val scrapedUnit = extractUnit(scraped.universalName)
                val matchKey = mergedCatalogue.keys.find { existing ->
                    val existingUnit = extractUnit(existing)
                    // Units must both be absent OR both present and equal.
                    // "Milk" vs "Milk 2L" → one has unit, other doesn't → never merge.
                    val unitCompatible = when {
                        scrapedUnit == null && existingUnit == null -> true
                        scrapedUnit != null && existingUnit != null -> scrapedUnit == existingUnit
                        else -> false
                    }
                    unitCompatible && FuzzyMatcher.getSimilarityScore(existing, scraped.universalName) > 0.78
                }

                if (matchKey != null) {
                    val existing = mergedCatalogue[matchKey]!!
                    mergedCatalogue[matchKey] = existing.copy(
                        colesPrice = if (scraped.colesPrice > 0) scraped.colesPrice else existing.colesPrice,
                        wooliesPrice = if (scraped.wooliesPrice > 0) scraped.wooliesPrice else existing.wooliesPrice,
                        aldiPrice = if (scraped.aldiPrice > 0) scraped.aldiPrice else existing.aldiPrice,
                        isSpecial = scraped.isSpecial || existing.isSpecial,
                        imageUrl = if (scraped.imageUrl.isNotEmpty()) scraped.imageUrl else existing.imageUrl,
                        pageNumber = scraped.pageNumber ?: existing.pageNumber,
                        catalogueDate = scraped.catalogueDate ?: existing.catalogueDate,
                        lastUpdated = System.currentTimeMillis()
                    )
                } else {
                    mergedCatalogue[scraped.universalName] = scraped
                }
            }
            
            val finalBatch = mergedCatalogue.values.toList()
            if (finalBatch.isNotEmpty()) {
                dao.insertProducts(finalBatch)
                Timber.d("Successfully synced ${finalBatch.size} products to database")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error syncing products to database")
        }
    }
    
    suspend fun seedSampleData() {
        // Mock suppression active as per requirements.
    }
    
    suspend fun updatePreferences(city: String, postcode: String, suburb: String) {
        val current = dao.getUserEntitySync() ?: UserEntity()
        val updated = current.copy(city = city, postcode = postcode, suburb = suburb)
        dao.setUserEntity(updated)
    }
    
    fun getUserEntity() = dao.getUserEntity()

    /**
     * Replace all products in the DB with a fresh API result.
     * Deletes stale data first so old products don't linger after a retailer removes them.
     */
    suspend fun replaceAll(products: List<MasterProductEntity>) = withContext(Dispatchers.IO) {
        try {
            dao.deleteAllProducts()
            dao.insertProducts(products)
            Timber.d("Replaced DB with ${products.size} products")
        } catch (e: Exception) {
            Timber.e(e, "replaceAll failed")
        }
    }

    // Extract normalised unit string from a product name, e.g. "Milk 2L" → "2l", "Cheese 500g" → "500g"
    private fun extractUnit(name: String): String? {
        val match = Regex("""(\d+(?:\.\d+)?)\s*(kg|g|mg|l|ml|pk|pack|ea)""", RegexOption.IGNORE_CASE).find(name)
        return match?.value?.replace("\\s".toRegex(), "")?.lowercase()
    }
}
