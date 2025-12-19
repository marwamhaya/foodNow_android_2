package com.example.foodnow.service

import android.util.Log
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Nominatim Geocoding Service
 * Uses OpenStreetMap's Nominatim API to convert addresses to coordinates
 * 
 * Rate Limit: Max 1 request per second
 * Documentation: https://nominatim.org/release-docs/develop/api/Search/
 */
object NominatimGeocodingService {

    private const val TAG = "NominatimGeocoding"
    private const val BASE_URL = "https://nominatim.openstreetmap.org"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    // Simple cache to avoid repeated requests for same address
    private val cache = mutableMapOf<String, GeoLocation?>()
    
    data class GeoLocation(
        val latitude: Double,
        val longitude: Double,
        val displayName: String
    )
    
    /**
     * Geocode an address to coordinates
     * @param address The address to geocode (e.g., "123 Main St, Casablanca, Morocco")
     * @return GeoLocation with lat/lon or null if not found
     */
    suspend fun geocode(address: String): GeoLocation? = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            if (cache.containsKey(address)) {
                Log.d(TAG, "Cache hit for: $address")
                return@withContext cache[address]
            }
            
            // Rate limiting - wait 1 second between requests
            Thread.sleep(1000)
            
            val encodedAddress = URLEncoder.encode(address, "UTF-8")
            val url = "$BASE_URL/search?q=$encodedAddress&format=json&limit=1"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FoodNowApp/1.0")
                .build()
            
            Log.d(TAG, "Geocoding request: $url")
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                Log.e(TAG, "Geocoding failed: ${response.code}")
                cache[address] = null
                return@withContext null
            }
            
            val jsonArray = JSONArray(responseBody)
            if (jsonArray.length() == 0) {
                Log.w(TAG, "No results for address: $address")
                cache[address] = null
                return@withContext null
            }
            
            val result = jsonArray.getJSONObject(0)
            val lat = result.getDouble("lat")
            val lon = result.getDouble("lon")
            val displayName = result.getString("display_name")
            
            val location = GeoLocation(lat, lon, displayName)
            cache[address] = location
            
            Log.d(TAG, "Geocoded: $address -> ($lat, $lon)")
            return@withContext location
            
        } catch (e: Exception) {
            Log.e(TAG, "Geocoding error for '$address'", e)
            cache[address] = null
            return@withContext null
        }
    }
    
    /**
     * Reverse geocode coordinates to address
     * @param latitude Latitude
     * @param longitude Longitude
     * @return Address string or null if not found
     */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        try {
            Thread.sleep(1000) // Rate limiting
            
            val url = "$BASE_URL/reverse?lat=$latitude&lon=$longitude&format=json"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FoodNowApp/1.0")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                Log.e(TAG, "Reverse geocoding failed: ${response.code}")
                return@withContext null
            }
            
            val json = org.json.JSONObject(responseBody)
            val displayName = json.optString("display_name", null)
            
            Log.d(TAG, "Reverse geocoded: ($latitude, $longitude) -> $displayName")
            return@withContext displayName
            
        } catch (e: Exception) {
            Log.e(TAG, "Reverse geocoding error", e)
            return@withContext null
        }
    }
    
    /**
     * Clear the geocoding cache
     */
    fun clearCache() {
        cache.clear()
        Log.d(TAG, "Cache cleared")
    }
}
