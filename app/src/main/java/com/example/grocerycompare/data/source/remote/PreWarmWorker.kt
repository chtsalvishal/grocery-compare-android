package com.example.grocerycompare.data.source.remote

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.grocerycompare.GroceryApplication
import timber.log.Timber

/**
 * Lightweight periodic worker that pings the backend health endpoint.
 * Keeps the Render free-tier server warm so users rarely hit a cold-start delay.
 * Runs every 15 minutes when the device has network — always returns success
 * since this is best-effort.
 */
class PreWarmWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val api = (applicationContext as GroceryApplication).container.apiService
            val alive = api.isHealthy()
            Timber.d("PreWarmWorker: server alive=$alive")
            Result.success()
        } catch (e: Exception) {
            Timber.w("PreWarmWorker: ping failed — ${e.message}")
            Result.success() // always succeed — this is best-effort only
        }
    }
}
