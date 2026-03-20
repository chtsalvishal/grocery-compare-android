package com.example.grocerycompare.data.source.remote

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import com.example.grocerycompare.GroceryApplication
import com.example.grocerycompare.data.local.entity.MasterProductEntity
import com.example.grocerycompare.data.remote.ApiProduct
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

            // Poll until server is up — handles Render cold-start (~30s warm-up)
            val pollIntervalMs = 5_000L
            val maxWaitMs      = 90_000L
            var waited         = 0L
            var serverReady    = false

            while (waited <= maxWaitMs) {
                if (container.apiService.isHealthy()) {
                    serverReady = true
                    break
                }
                val secs = (waited / 1000).toInt()
                setProgress(workDataOf(
                    "STATUS"   to "Server warming up… (${secs}s)",
                    "PROGRESS" to 5
                ))
                delay(pollIntervalMs)
                waited += pollIntervalMs
            }

            if (!serverReady) {
                Timber.w("SyncWorker: Backend unreachable after ${maxWaitMs / 1000}s")
                return Result.failure(
                    workDataOf("STATUS" to "Server unreachable after 90s — try again later")
                )
            }

            // Poll fetchSpecials until the backend scrape finishes populating the DB.
            // On a cold Render start the initial scrape takes ~3-4 min, so we wait up to 5 min.
            val fetchIntervalMs  = 15_000L
            val maxFetchWaitMs   = 300_000L   // 5 minutes
            var fetchWaited      = 0L
            var products         = emptyList<ApiProduct>()

            while (true) {
                val elapsed = fetchWaited / 1000
                setProgress(workDataOf(
                    "STATUS"   to if (fetchWaited == 0L) "Fetching specials…"
                                  else "Syncing data… (${elapsed}s)",
                    "PROGRESS" to (20 + (fetchWaited.toFloat() / maxFetchWaitMs * 55).toInt()).coerceAtMost(75)
                ))

                products = container.apiService.fetchSpecials(suburb = suburb, postcode = postcode)
                if (products.isNotEmpty()) break

                fetchWaited += fetchIntervalMs
                if (fetchWaited > maxFetchWaitMs) break
                Timber.d("SyncWorker: 0 products returned, backend scrape still running — retrying in ${fetchIntervalMs / 1000}s")
                delay(fetchIntervalMs)
            }

            if (products.isEmpty()) {
                Timber.w("SyncWorker: API returned 0 products after ${maxFetchWaitMs / 1000}s")
                return Result.failure(
                    workDataOf("STATUS" to "No specials available — try again later")
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
