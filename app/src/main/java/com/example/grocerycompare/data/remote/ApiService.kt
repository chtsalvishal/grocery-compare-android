package com.example.grocerycompare.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import timber.log.Timber

@Serializable
data class ApiProduct(
    val name: String,
    val category: String = "Weekly Specials",
    val colesPrice: Double? = null,
    val wooliesPrice: Double? = null,
    val aldiPrice: Double? = null,
    val colesWasPrice: Double? = null,
    val wooliesWasPrice: Double? = null,
    val aldiWasPrice: Double? = null,
    val unit: String = "ea",
    val imageUrl: String? = null,
    val lastUpdated: String = ""
)

class ApiService(val baseUrl: String) {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val client = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
        }
    }

    /**
     * Fetch all current specials from the backend.
     * Returns an empty list on any error — the caller decides whether to surface it.
     */
    suspend fun fetchSpecials(
        suburb: String = "",
        postcode: String = "",
        category: String? = null,
        query: String? = null
    ): List<ApiProduct> {
        return try {
            val text = client.get("$baseUrl/api/specials") {
                if (suburb.isNotBlank()) parameter("suburb", suburb)
                if (postcode.isNotBlank()) parameter("postcode", postcode)
                if (!category.isNullOrBlank()) parameter("category", category)
                if (!query.isNullOrBlank()) parameter("q", query)
            }.bodyAsText()
            json.decodeFromString(ListSerializer(ApiProduct.serializer()), text)
        } catch (e: Exception) {
            Timber.e(e, "ApiService.fetchSpecials failed")
            emptyList()
        }
    }

    suspend fun isHealthy(): Boolean {
        return try {
            client.get("$baseUrl/api/health").status.value in 200..299
        } catch (e: Exception) {
            false
        }
    }

    fun close() = client.close()
}
