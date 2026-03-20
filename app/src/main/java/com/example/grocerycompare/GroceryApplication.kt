package com.example.grocerycompare

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.Constraints
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.grocerycompare.data.source.remote.PreWarmWorker
import java.util.concurrent.TimeUnit
import com.example.grocerycompare.BuildConfig
import com.example.grocerycompare.data.local.AppDatabase
import com.example.grocerycompare.data.remote.ApiService
import com.example.grocerycompare.data.repository.MasterCatalogueRepository
import timber.log.Timber

class GroceryApplication : Application(), Configuration.Provider {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        try {
            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            }
            container = AppContainer(this)
            // Keep Render free-tier warm — ping health endpoint every 15 minutes
            val preWarmRequest = PeriodicWorkRequestBuilder<PreWarmWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "pre_warm",
                ExistingPeriodicWorkPolicy.KEEP,
                preWarmRequest
            )
            Timber.i("Application container initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize application container")
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}

// Migrate v6 → v7: drop and recreate master_catalogue (scrape data, safely regenerated).
// user_entity is preserved as-is — schema unchanged between these versions.
private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS `master_catalogue`")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `master_catalogue` (
                `universalName` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `colesPrice` REAL NOT NULL DEFAULT 0.0,
                `wooliesPrice` REAL NOT NULL DEFAULT 0.0,
                `aldiPrice` REAL NOT NULL DEFAULT 0.0,
                `isSpecial` INTEGER NOT NULL DEFAULT 0,
                `imageUrl` TEXT NOT NULL DEFAULT '',
                `pageNumber` INTEGER,
                `catalogueDate` TEXT,
                `lastUpdated` INTEGER NOT NULL,
                PRIMARY KEY(`universalName`)
            )
        """.trimIndent())
    }
}

// Migrate v7 → v8: add wasPrice columns to master_catalogue.
private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `master_catalogue` ADD COLUMN `colesWasPrice` REAL NOT NULL DEFAULT 0.0")
        database.execSQL("ALTER TABLE `master_catalogue` ADD COLUMN `wooliesWasPrice` REAL NOT NULL DEFAULT 0.0")
        database.execSQL("ALTER TABLE `master_catalogue` ADD COLUMN `aldiWasPrice` REAL NOT NULL DEFAULT 0.0")
    }
}

class AppContainer(private val context: android.content.Context) {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "grocery_db"
        )
        .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
        .fallbackToDestructiveMigration()
        .build()
    }

    val repository: MasterCatalogueRepository by lazy {
        MasterCatalogueRepository(database.masterCatalogueDao())
    }

    val apiService: ApiService by lazy {
        ApiService(BuildConfig.BACKEND_URL)
    }
}
