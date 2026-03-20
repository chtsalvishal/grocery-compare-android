package com.example.grocerycompare

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.grocerycompare.data.local.AppDatabase
import com.example.grocerycompare.data.local.entity.MasterProductEntity
import com.example.grocerycompare.data.local.entity.UserEntity
import com.example.grocerycompare.data.repository.MasterCatalogueRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented integration tests for MasterCatalogueRepository against an
 * in-memory Room database. Tests the real DAO + repository contract.
 */
@RunWith(AndroidJUnit4::class)
class RepositoryIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: MasterCatalogueRepository

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = MasterCatalogueRepository(db.masterCatalogueDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun products_emptyOnFreshDb() = runTest {
        val products = repo.products.first()
        assertTrue("Fresh DB should have no products", products.isEmpty())
    }

    @Test
    fun replaceAll_storesAndRetrievesProducts() = runTest {
        val entities = listOf(
            MasterProductEntity(
                universalName = "Tim Tam Original 200g",
                category = "Biscuits & Crackers",
                colesPrice = 3.50,
                wooliesPrice = 3.70,
                aldiPrice = 0.0,
                lastUpdated = System.currentTimeMillis()
            ),
            MasterProductEntity(
                universalName = "Full Cream Milk 2L",
                category = "Dairy",
                colesPrice = 2.90,
                wooliesPrice = 3.00,
                aldiPrice = 2.79,
                lastUpdated = System.currentTimeMillis()
            )
        )

        repo.replaceAll(entities)

        val stored = repo.products.first()
        assertEquals("Should have 2 products after replaceAll", 2, stored.size)
        val timTam = stored.find { it.universalName == "Tim Tam Original 200g" }
        assertNotNull("Tim Tam should be stored", timTam)
        assertEquals("Coles price should match", 3.50, timTam!!.colesPrice, 0.001)
    }

    @Test
    fun replaceAll_replacesExistingData() = runTest {
        val first = listOf(
            MasterProductEntity("Old Product", "Other", colesPrice = 1.0, lastUpdated = 0L)
        )
        val second = listOf(
            MasterProductEntity("New Product", "Other", colesPrice = 2.0, lastUpdated = 0L)
        )

        repo.replaceAll(first)
        repo.replaceAll(second)

        val stored = repo.products.first()
        assertEquals("replaceAll should replace all old data", 1, stored.size)
        assertEquals("New Product", stored[0].universalName)
    }

    @Test
    fun initializeUserIfEmpty_createsDefaultUser() = runTest {
        repo.initializeUserIfEmpty()
        val user = repo.getUserEntity().first()
        assertNotNull("User entity should exist after init", user)
        assertEquals("Default postcode should be 3204", "3204", user!!.postcode)
        assertEquals("Default city should be Melbourne", "Melbourne", user.city)
    }

    @Test
    fun updatePreferences_persistsNewValues() = runTest {
        repo.initializeUserIfEmpty()
        repo.updatePreferences(city = "Sydney", postcode = "2000", suburb = "CBD")

        val user = repo.getUserEntity().first()
        assertNotNull(user)
        assertEquals("Sydney", user!!.city)
        assertEquals("2000", user.postcode)
        assertEquals("CBD", user.suburb)
    }
}
