package com.example.grocerycompare.data.source.remote

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.grocerycompare.GroceryApplication
import com.example.grocerycompare.data.local.entity.MasterProductEntity
import com.example.grocerycompare.util.Categorizer
import timber.log.Timber

/**
 * Replaces PdfDownloadWorker.
 * Calls the GroceryCompare backend API and stores results in Room.
 * One worker handles all three retailers in a single pass.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val rawSuburb = inputData.getString("SUBURB") ?: "Bentleigh"
        val rawPostcode = inputData.getString("POSTCODE") ?: "3204"
        val suburb = if (rawSuburb.matches(Regex("[a-zA-Z ]{1,50}"))) rawSuburb else "Bentleigh"
        val postcode = if (rawPostcode.matches(Regex("\\d{4}"))) rawPostcode else "3204"

        val container = (applicationContext as GroceryApplication).container

        return try {
            setProgress(workDataOf("STATUS" to "Connecting to server...", "PROGRESS" to 5))

            // Liveness check first — give a clear error if the server is down
            val healthy = container.apiService.isHealthy()
            if (!healthy) {
                Timber.w("SyncWorker: Backend is unreachable")
                return Result.failure(
                    workDataOf("STATUS" to "Server unreachable — check your connection")
                )
            }

            setProgress(workDataOf("STATUS" to "Fetching specials...", "PROGRESS" to 20))

            val products = container.apiService.fetchSpecials(suburb = suburb, postcode = postcode)

            if (products.isEmpty()) {
                Timber.w("SyncWorker: API returned 0 products")
                return Result.failure(
                    workDataOf("STATUS" to "No specials returned — try again later")
                )
            }

            setProgress(workDataOf(
                "STATUS" to "Saving ${products.size} specials...",
                "PROGRESS" to 80
            ))

            val entities = products.map { p ->
                MasterProductEntity(
                    universalName = p.name,
                    category = p.category.ifBlank { Categorizer.categorize(p.name) },
                    colesPrice = p.colesPrice ?: 0.0,
                    wooliesPrice = p.wooliesPrice ?: 0.0,
                    aldiPrice = p.aldiPrice ?: 0.0,
                    colesWasPrice = p.colesWasPrice ?: 0.0,
                    wooliesWasPrice = p.wooliesWasPrice ?: 0.0,
                    aldiWasPrice = p.aldiWasPrice ?: 0.0,
                    isSpecial = true,
                    imageUrl = p.imageUrl ?: "",
                    lastUpdated = System.currentTimeMillis()
                )
            }

            container.repository.replaceAll(entities)
            Timber.d("SyncWorker: Saved ${entities.size} products to DB")

            setProgress(workDataOf("STATUS" to "Complete", "PROGRESS" to 100))
            Result.success(workDataOf("COUNT" to products.size))

        } catch (e: Exception) {
            Timber.e(e, "SyncWorker: Fatal error — ${e.message}")
            Result.failure(workDataOf("STATUS" to "Sync failed — try again"))
        }
    }
}
