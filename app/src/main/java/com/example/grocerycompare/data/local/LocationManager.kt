package com.example.grocerycompare.data.local

import android.content.Context
import android.content.SharedPreferences

class LocationManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_POSTCODE = "postcode"
        private const val DEFAULT_POSTCODE = "3000"
    }

    fun getPostcode(): String {
        return prefs.getString(KEY_POSTCODE, DEFAULT_POSTCODE) ?: DEFAULT_POSTCODE
    }

    fun setPostcode(postcode: String) {
        prefs.edit().putString(KEY_POSTCODE, postcode).apply()
    }
}
