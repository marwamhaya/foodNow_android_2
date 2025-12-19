package com.example.foodnow.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline

/**
 * Helper class for navigation and routing functionality
 */
object NavigationHelper {
    
    private const val TAG = "NavigationHelper"
    
    /**
     * Calculate route between two points using OSRM
     * @param context Application context
     * @param startPoint Starting location
     * @param endPoint Destination location
     * @return Road object containing route information, or null if routing fails
     */
    suspend fun calculateRoute(
        context: Context,
        startPoint: GeoPoint,
        endPoint: GeoPoint
    ): Road? = withContext(Dispatchers.IO) {
        try {
            val roadManager: RoadManager = OSRMRoadManager(context, "FoodNow/1.0")
            val waypoints = arrayListOf(startPoint, endPoint)
            
            Log.d(TAG, "Calculating route from ${startPoint.latitude},${startPoint.longitude} to ${endPoint.latitude},${endPoint.longitude}")
            
            val road = roadManager.getRoad(waypoints)
            
            if (road.mStatus == Road.STATUS_OK) {
                Log.d(TAG, "Route calculated successfully: ${road.mLength}km, ${road.mDuration/60}min")
                road
            } else {
                Log.e(TAG, "Route calculation failed with status: ${road.mStatus}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating route", e)
            null
        }
    }
    
    /**
     * Create a polyline overlay from a road
     * @param road Road object from routing
     * @param color Color for the polyline
     * @param width Width of the polyline in pixels
     * @return Polyline overlay ready to be added to map
     */
    fun createRoutePolyline(road: Road, color: Int, width: Float = 10f): Polyline {
        val polyline = Polyline()
        polyline.setPoints(road.mRouteHigh)
        polyline.outlinePaint.color = color
        polyline.outlinePaint.strokeWidth = width
        return polyline
    }
    
    /**
     * Calculate distance between two points in kilometers
     * @param start Starting point
     * @param end Ending point
     * @return Distance in kilometers
     */
    fun calculateDistance(start: GeoPoint, end: GeoPoint): Double {
        return start.distanceToAsDouble(end) / 1000.0 // Convert meters to km
    }
    
    /**
     * Calculate ETA based on distance and average speed
     * @param distanceKm Distance in kilometers
     * @param averageSpeedKmh Average speed in km/h (default 30 km/h for city driving)
     * @return ETA in minutes
     */
    fun calculateETA(distanceKm: Double, averageSpeedKmh: Double = 30.0): Int {
        return ((distanceKm / averageSpeedKmh) * 60).toInt()
    }
    
    /**
     * Format distance for display
     * @param distanceKm Distance in kilometers
     * @return Formatted string (e.g., "2.5 km" or "850 m")
     */
    fun formatDistance(distanceKm: Double): String {
        return if (distanceKm < 1.0) {
            "${(distanceKm * 1000).toInt()} m"
        } else {
            String.format("%.1f km", distanceKm)
        }
    }
    
    /**
     * Format ETA for display
     * @param minutes ETA in minutes
     * @return Formatted string (e.g., "5 min" or "1h 15min")
     */
    fun formatETA(minutes: Int): String {
        return if (minutes < 60) {
            "$minutes min"
        } else {
            val hours = minutes / 60
            val mins = minutes % 60
            "${hours}h ${mins}min"
        }
    }
    
    /**
     * Get next turn instruction from road
     * @param road Road object
     * @param currentNodeIndex Current position on route
     * @return Next turn instruction or null
     */
    fun getNextInstruction(road: Road, currentNodeIndex: Int): String? {
        if (currentNodeIndex >= 0 && currentNodeIndex < road.mNodes.size) {
            return road.mNodes[currentNodeIndex].mInstructions
        }
        return null
    }
    
    /**
     * Check if driver has deviated from route
     * @param currentPosition Current GPS position
     * @param routePoints List of route points
     * @param thresholdMeters Maximum allowed deviation in meters
     * @return True if driver has deviated significantly
     */
    fun hasDeviatedFromRoute(
        currentPosition: GeoPoint,
        routePoints: List<GeoPoint>,
        thresholdMeters: Double = 50.0
    ): Boolean {
        if (routePoints.isEmpty()) return false
        
        // Find closest point on route
        val minDistance = routePoints.minOfOrNull { point ->
            currentPosition.distanceToAsDouble(point)
        } ?: Double.MAX_VALUE
        
        return minDistance > thresholdMeters
    }
}
