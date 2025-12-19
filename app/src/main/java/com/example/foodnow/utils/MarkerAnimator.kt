package com.example.foodnow.utils

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Utility class for smooth marker animations on OSMDroid maps
 */
object MarkerAnimator {
    
    private const val ANIMATION_DURATION = 1000L // 1 second
    
    /**
     * Animate marker from current position to new position
     * @param marker The marker to animate
     * @param targetPosition Target GeoPoint
     * @param mapView MapView to invalidate
     * @param duration Animation duration in milliseconds
     */
    fun animateMarkerToPosition(
        marker: Marker,
        targetPosition: GeoPoint,
        mapView: MapView,
        duration: Long = ANIMATION_DURATION
    ) {
        val startPosition = marker.position
        
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = duration
        animator.interpolator = LinearInterpolator()
        
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            
            val lat = startPosition.latitude + (targetPosition.latitude - startPosition.latitude) * fraction
            val lon = startPosition.longitude + (targetPosition.longitude - startPosition.longitude) * fraction
            
            marker.position = GeoPoint(lat, lon)
            mapView.invalidate()
        }
        
        animator.start()
    }
    
    /**
     * Animate camera to center on a position
     * @param mapView MapView to animate
     * @param targetPosition Target GeoPoint
     * @param targetZoom Target zoom level (optional)
     * @param duration Animation duration in milliseconds
     */
    fun animateCameraToPosition(
        mapView: MapView,
        targetPosition: GeoPoint,
        targetZoom: Double? = null,
        duration: Long = ANIMATION_DURATION
    ) {
        val startPosition = mapView.mapCenter as GeoPoint
        val startZoom = mapView.zoomLevelDouble
        val endZoom = targetZoom ?: startZoom
        
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = duration
        animator.interpolator = LinearInterpolator()
        
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            
            val lat = startPosition.latitude + (targetPosition.latitude - startPosition.latitude) * fraction
            val lon = startPosition.longitude + (targetPosition.longitude - startPosition.longitude) * fraction
            val zoom = startZoom + (endZoom - startZoom) * fraction
            
            mapView.controller.setCenter(GeoPoint(lat, lon))
            mapView.controller.setZoom(zoom)
        }
        
        animator.start()
    }
    
    /**
     * Calculate bearing between two points
     * @param start Starting point
     * @param end Ending point
     * @return Bearing in degrees
     */
    fun calculateBearing(start: GeoPoint, end: GeoPoint): Float {
        val startLat = Math.toRadians(start.latitude)
        val startLon = Math.toRadians(start.longitude)
        val endLat = Math.toRadians(end.latitude)
        val endLon = Math.toRadians(end.longitude)
        
        val dLon = endLon - startLon
        
        val y = Math.sin(dLon) * Math.cos(endLat)
        val x = Math.cos(startLat) * Math.sin(endLat) - 
                Math.sin(startLat) * Math.cos(endLat) * Math.cos(dLon)
        
        var bearing = Math.toDegrees(Math.atan2(y, x))
        bearing = (bearing + 360) % 360
        
        return bearing.toFloat()
    }
    
    /**
     * Rotate marker icon based on bearing
     * @param marker Marker to rotate
     * @param bearing Bearing in degrees
     */
    fun rotateMarker(marker: Marker, bearing: Float) {
        marker.rotation = bearing
    }
}
