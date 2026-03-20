package com.example.grocerycompare

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.grocerycompare.data.repository.GroceryRepository
import com.example.grocerycompare.data.source.remote.VisualPriceScanner
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
class IntegrationPriceTest {

    @Test
    fun verifyOCRStability() = runTest(timeout = 5.minutes) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scanner = VisualPriceScanner(context)
        val testUrl = "https://www.coles.com.au/search?q=tim+tam"
        
        // Start the visual scan
        val result = scanner.scan(testUrl)
        
        assertNotNull("OCR Engine failed to initialize or scan", result)
        assertTrue("OCR detected invalid price format: $${result?.price}", result!!.price > 0.1)
        
        // Final Proof: Logging for Logcat audit
        println("OCR Success: Found \$${result.price} from visual render.")
    }

    @Test
    fun finalLiveProductionSync() = runTest(timeout = 10.minutes) {
        val repo = GroceryRepository()
        val stores = listOf("Coles", "Woolworths", "Aldi", "Costco")
        val testQuery = "Tim Tam Double Coat"
        
        stores.forEach { store ->
            println("PRODUCTION_SYNC_START: Fetching '$testQuery' from $store")
            try {
                val result = repo.fetchPrice(store, testQuery)
                
                // Zero-Trust Validations
                assertNotNull("CRITICAL: Network call for $store returned null", result)
                assertNotEquals("STALE_DATA_DETECTED: Price is the old value (3.20)", 3.20, result.price, 0.001)
                assertNotEquals("ZERO_PRICE_ERROR: Price is 0.0", 0.0, result.price, 0.001)
                
                // Real-world variance check
                assertTrue("UNREALISTIC_PRICE: $${result.price} for $store", result.price > 1.0 && result.price < 30.0)
                
                println("LIVE_DATA_VERIFIED: $store returned \$${result.price} [Img: ${result.imageUrl?.take(20)}...]")
            } catch (e: Exception) {
                println("PRODUCTION_SYNC_FAILURE: $store failed. Error: ${e.message}")
                throw e
            }
        }
    }
}
