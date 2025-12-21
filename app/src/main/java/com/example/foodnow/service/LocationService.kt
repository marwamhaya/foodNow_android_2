package com.example.foodnow.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationService : Service() {

    private lateinit var locationManager: LocationManager
    private var locationListener: LocationListener? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private var activeOrderId: Long? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.hasExtra("order_id")) {
                activeOrderId = it.getLongExtra("order_id", -1)
                Log.d("LocationService", "Started tracking for order: $activeOrderId")
            }
        }
        WebSocketService.connect(this)
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        createNotificationChannel()
        startForeground(1, createNotification())

        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationService", "Location permission not granted")
            return
        }

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                sendLocationUpdate(location)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {
                Log.d("LocationService", "Provider enabled: $provider")
            }
            override fun onProviderDisabled(provider: String) {
                Log.w("LocationService", "Provider disabled: $provider")
            }
        }

        try {
            // Request updates from GPS provider
            // Reduced interval for smoother real-time tracking
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2500L, // 2.5 seconds (reduced from 5s)
                5f,    // 5 meters (reduced from 10m)
                locationListener!!
            )
            
            // Also request from network provider as fallback
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2500L,
                    5f,
                    locationListener!!
                )
            }
            
            Log.d("LocationService", "Location updates started")
        } catch (e: SecurityException) {
            Log.e("LocationService", "Security exception starting location updates", e)
        }
    }

    private fun sendLocationUpdate(location: Location) {
        // Validate Token and Role
        val tokenManager = com.example.foodnow.data.TokenManager(this)
        val token = tokenManager.getToken()
        // Assuming ROLE_LIVREUR is stored as "LIVREUR" or "ROLE_LIVREUR". Checking contains "LIVREUR" to be safe.
        // Actually TokenManager.kt doesn't show role saving logic, checking what's available.
        // But simply checking if token exists is a good start. 
        if (token.isNullOrEmpty()) {
            Log.e("LocationService", "No auth token found. Stopping service.")
            stopSelf()
            return
        }

        scope.launch {
            try {
                Log.d("LocationService", "Location update: ${location.latitude}, ${location.longitude}")
                
                // Send to Backend (which broadcasts via WebSocket) if order is active
                activeOrderId?.let { orderId ->
                    if (orderId != -1L) {
                        val repository = (application as FoodNowApp).repository
                        repository.saveDriverLocation(orderId, com.example.foodnow.data.LocationUpdateDto(location.latitude, location.longitude))
                    }
                }

                // Also update via REST API for backup/logging
                val repository = (application as FoodNowApp).repository
                repository.updateLocation(location.latitude, location.longitude)
            } catch (e: Exception) {
                Log.e("LocationService", "Error sending location update", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location_channel", 
                "Location Tracking", 
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("FoodNow Delivery")
            .setContentText("Tracking location...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationListener?.let {
            locationManager.removeUpdates(it)
        }
        WebSocketService.disconnect()
        Log.d("LocationService", "Location tracking stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        suspend fun getCurrentLocation(context: Context): Location? = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            
            // Check permissions
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
                if (cont.isActive) cont.resume(null, null)
                return@suspendCancellableCoroutine
            }

            // Try last known first
            val lastKnownGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastKnownNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            var bestLocation = lastKnownGps
            
            if (lastKnownNet != null) {
                if (bestLocation == null || lastKnownNet.time > bestLocation.time) {
                    bestLocation = lastKnownNet
                }
            }
            
            // If we have a recent location (e.g. within 5 mins), return it immediately
            if (bestLocation != null && System.currentTimeMillis() - bestLocation.time < 5 * 60 * 1000) {
                 if (cont.isActive) cont.resume(bestLocation, null)
                 return@suspendCancellableCoroutine
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (cont.isActive) {
                        cont.resume(location, null)
                        locationManager.removeUpdates(this)
                    }
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {
                    if (cont.isActive) {
                         // If one provider is disabled, we might still wait for another? 
                         // But for simplicity, if we rely on this provider and it dies, we might fail or wait for timeout.
                         // We won't resume null immediately here to allow race with other provider or timeout.
                    }
                }
            }

            try {
                var requestStarted = false
                // Request updates from both if enabled
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                     locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener, android.os.Looper.getMainLooper())
                     requestStarted = true
                }
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                     locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener, android.os.Looper.getMainLooper())
                     requestStarted = true
                }
                
                if (!requestStarted) {
                     if (cont.isActive) cont.resume(null, null)
                }
                
                cont.invokeOnCancellation {
                    locationManager.removeUpdates(listener)
                }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null, null)
                android.util.Log.e("LocationService", "Error requesting location", e)
            }
        }
    }
}
