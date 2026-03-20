package com.example.grocerycompare.util

import kotlin.math.max
import kotlin.math.min

object FuzzyMatcher {
    private val STORE_NAMES = listOf("coles", "woolworths", "woolies", "aldi", "costco")

    fun getSimilarityScore(s1: String, s2: String): Double {
        val n1 = normalizeGroceryName(s1)
        val n2 = normalizeGroceryName(s2)
        
        if (n1 == n2) return 1.0
        
        val maxLength = max(n1.length, n2.length)
        if (maxLength == 0) return 1.0
        
        val distance = levenshteinDistance(n1, n2)
        return 1.0 - (distance.toDouble() / maxLength.toDouble())
    }

    private fun normalizeGroceryName(name: String): String {
        var clean = name.lowercase().trim()
        
        // Remove store names from the start/end to allow cross-store matching
        STORE_NAMES.forEach { store ->
            clean = clean.replace(Regex("^$store\\s+"), "")
            clean = clean.replace(Regex("\\s+$store$"), "")
        }
        
        // Standardize common units to prevent matching 1L with 3L
        clean = clean.replace(" litres", "l")
            .replace(" litre", "l")
            .replace(" liter", "l")
            .replace(" milliliters", "ml")
            .replace(" millilitre", "ml")
            .replace(" grams", "g")
            .replace(" gram", "g")
            .replace(" kilograms", "kg")
            .replace(" kilogram", "kg")
        
        return clean.replace(Regex("[^a-z0-9]"), "")
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) {
            for (j in 0..s2.length) {
                when {
                    i == 0 -> dp[i][j] = j
                    j == 0 -> dp[i][j] = i
                    else -> {
                        dp[i][j] = min(
                            min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + if (s1[i - 1] == s2[j - 1]) 0 else 1
                        )
                    }
                }
            }
        }
        return dp[s1.length][s2.length]
    }
}
