package com.example.foodnow.ui.orders

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.coroutineScope
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.data.DeliveryResponse
import com.example.foodnow.service.NominatimGeocodingService
import com.example.foodnow.ui.ViewModelFactory
import com.example.foodnow.ui.livreur.LivreurViewModel
import com.example.foodnow.utils.MarkerAnimator
import com.example.foodnow.utils.NavigationHelper
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent

class TrackOrderFragment : Fragment(R.layout.fragment_track_order) {

    private lateinit var mapView: MapView
    private lateinit var tvDriverInfo: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvETA: TextView
    private lateinit var tvConnectionStatus: TextView
    private lateinit var cardConnectionStatus: CardView
    private lateinit var timelineStep1: View
    private lateinit var timelineStep2: View
    private lateinit var timelineStep3: View
    
    private var stompClient: StompClient? = null
    private val gson = Gson()
    private var driverMarker: Marker? = null
    private var clientMarker: Marker? = null
    private var routeLine: Polyline? = null
    private var clientLocation: GeoPoint? = null
    private var lastDriverLocation: GeoPoint? = null
    private var updateJob: Job? = null
    private var reconnectAttempts = 0
    
    private val viewModel: LivreurViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1002
        private const val TAG = "TrackOrder"
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val RECONNECT_DELAY = 3000L
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        initializeMap()
        
        // Check permissions
        checkAndRequestLocationPermission()
        
        // Fetch delivery details to get client address
        val orderId = arguments?.getLong("orderId") ?: 1L
        fetchDeliveryDetails(orderId)
        
        connectStomp()
        
        // Start periodic UI updates
        startPeriodicUpdates()
    }
    
    private fun initializeViews(view: View) {
        mapView = view.findViewById(R.id.mapView)
        tvDriverInfo = view.findViewById(R.id.tvDriverInfo)
        tvDistance = view.findViewById(R.id.tvDistance)
        tvETA = view.findViewById(R.id.tvETA)
        tvConnectionStatus = view.findViewById(R.id.tvConnectionStatus)
        cardConnectionStatus = view.findViewById(R.id.cardConnectionStatus)
        timelineStep1 = view.findViewById(R.id.timelineStep1)
        timelineStep2 = view.findViewById(R.id.timelineStep2)
        timelineStep3 = view.findViewById(R.id.timelineStep3)
    }
    
    private fun initializeMap() {
        Configuration.getInstance().userAgentValue = requireContext().packageName
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(GeoPoint(33.5731, -7.5898))
    }

    private fun connectStomp() {
        showConnectionStatus("Connecting...", "#FF9800")
        
        // Get auth token from TokenManager
        val ctx = context ?: return
        val tokenManager = com.example.foodnow.data.TokenManager(ctx)
        val token = tokenManager.getToken()
        
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "No auth token available for WebSocket connection")
            showConnectionStatus("Authentication Required", "#F44336")
            Toast.makeText(context, "Please login again", Toast.LENGTH_LONG).show()
            return
        }
        
        val url = com.example.foodnow.utils.Constants.WS_URL
        
        // Create OkHttpClient with auth headers
        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
                chain.proceed(request)
            }
            .build()
        
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, url, null, client)
        
        stompClient?.lifecycle()?.subscribe({ lifecycleEvent ->
            when (lifecycleEvent.type) {
                LifecycleEvent.Type.OPENED -> {
                    activity?.runOnUiThread { 
                        showConnectionStatus("Connected", "#4CAF50")
                        reconnectAttempts = 0
                        
                        // Hide status after 2 seconds
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(2000)
                            cardConnectionStatus.visibility = View.GONE
                        }
                        
                        Log.d(TAG, "WebSocket connected")
                    }
                }
                LifecycleEvent.Type.ERROR -> {
                    Log.e(TAG, "WebSocket error", lifecycleEvent.exception)
                    activity?.runOnUiThread { 
                        showConnectionStatus("Connection Error", "#F44336")
                        handleConnectionLoss()
                    }
                }
                LifecycleEvent.Type.CLOSED -> {
                    Log.d(TAG, "WebSocket closed")
                    activity?.runOnUiThread { 
                        showConnectionStatus("Disconnected", "#F44336")
                        handleConnectionLoss()
                    }
                }
                else -> {}
            }
        }, { error ->
            Log.e(TAG, "Lifecycle subscription error", error)
        })

        // Subscribe to location updates
        val orderId = arguments?.getLong("orderId") ?: 1L
        
        stompClient?.topic("/topic/delivery/$orderId/location")?.subscribe({ topicMessage ->
            val payload = topicMessage.payload
            Log.d(TAG, "Location update received: $payload")
            
            try {
                val location = gson.fromJson(payload, LocationUpdateDto::class.java)
                requireActivity().runOnUiThread {
                    updateDriverLocation(location)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing location update", e)
            }
        }, { error ->
            Log.e(TAG, "Location subscription error", error)
        })
        
        // Subscribe to status updates
        stompClient?.topic("/topic/delivery/$orderId/status")?.subscribe({ topicMessage ->
             requireActivity().runOnUiThread {
                 try {
                     // Payload is a JSON object (DeliveryResponse), not just a string
                     val deliveryResponse = gson.fromJson(topicMessage.payload, DeliveryResponse::class.java)
                     updateDeliveryTimeline(deliveryResponse.status)
                     Log.d(TAG, "Status update: ${deliveryResponse.status}")
                 } catch (e: Exception) {
                     // Fallback: in case it IS just a string (backward compatibility)
                     updateDeliveryTimeline(topicMessage.payload)
                     Log.e(TAG, "Error parsing status update", e)
                 }
             }
        }, { error ->
            Log.e(TAG, "Status subscription error", error)
        })

        stompClient?.connect()
    }
    
    private fun showConnectionStatus(message: String, color: String) {
        cardConnectionStatus.visibility = View.VISIBLE
        tvConnectionStatus.text = message
        tvConnectionStatus.setBackgroundColor(Color.parseColor(color))
    }
    
    private fun handleConnectionLoss() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++
            Toast.makeText(context, "Connection lost. Reconnecting... (${reconnectAttempts}/$MAX_RECONNECT_ATTEMPTS)", Toast.LENGTH_SHORT).show()
            
            CoroutineScope(Dispatchers.IO).launch {
                delay(RECONNECT_DELAY)
                withContext(Dispatchers.Main) {
                    try {
                        stompClient?.disconnect()
                        connectStomp()
                    } catch (e: Exception) {
                        Log.e(TAG, "Reconnection failed", e)
                    }
                }
            }
        } else {
            Toast.makeText(context, "Unable to connect to server. Please check your connection.", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun checkAndRequestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Location permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun fetchDeliveryDetails(orderId: Long) {
        // Use LifecycleScope to fetch Client orders directly
        // Use LifecycleScope to fetch Client orders directly
        viewLifecycleOwner.lifecycle.coroutineScope.launch {
            try {
                val repository = (requireActivity().application as FoodNowApp).repository
                
                // 1. Get fundamental order details (address, status)
                val response = repository.getMyOrders()
                if (response.isSuccessful && response.body() != null) {
                    val orders = response.body()!!
                    val order = orders.find { it.id == orderId }
                    
                    if (order != null) {
                        val address = order.deliveryAddress ?: "Unknown Address"
                        tvDriverInfo.text = "Delivery to: Me\n$address"
                        updateDeliveryTimeline(order.status)
                        
                        // 2. Try to get precise GPS location
                        try {
                            val locResponse = repository.getOrderLocation(orderId)
                            if (locResponse.isSuccessful && locResponse.body() != null) {
                                val loc = locResponse.body()!!
                                val geoPoint = GeoPoint(loc.clientLatitude, loc.clientLongitude)
                                clientLocation = geoPoint
                                addClientMarkerAt(geoPoint, address)
                                Log.d(TAG, "Using stored GPS location: ${loc.clientLatitude}, ${loc.clientLongitude}")
                            } else {
                                // Fallback to geocoding
                                Log.w(TAG, "No stored GPS found, using geocoding")
                                geocodeAndAddMarker(address)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching location, falling back to geocoding", e)
                            geocodeAndAddMarker(address)
                        }
                    } else {
                        Toast.makeText(context, "Order not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                     Log.e(TAG, "Failed to fetch orders: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching order details", e)
            }
        }
    }
    
    private fun addClientMarkerAt(geoPoint: GeoPoint, address: String) {
        clientMarker = Marker(mapView)
        clientMarker?.position = geoPoint
        clientMarker?.title = "Delivery Location"
        clientMarker?.snippet = address
        clientMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        clientMarker?.icon = resources.getDrawable(android.R.drawable.ic_dialog_map, null)
        mapView.overlays.add(clientMarker)
        
        // Center on client location initially
        mapView.controller.setCenter(geoPoint)
        mapView.invalidate()
    }

    private fun geocodeAndAddMarker(address: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Geocoding client address: $address")
                val location = NominatimGeocodingService.geocode(address)
                
                if (location != null) {
                    withContext(Dispatchers.Main) {
                        val geoPoint = GeoPoint(location.latitude, location.longitude)
                        clientLocation = geoPoint
                        addClientMarkerAt(geoPoint, address)
                        Log.d(TAG, "Client marker added via geocoding")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Unable to locate delivery address", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Geocoding failed", e)
            }
        }
    }

    private fun updateDriverLocation(location: LocationUpdateDto) {
        val newPosition = GeoPoint(location.latitude, location.longitude)
        
        if (driverMarker == null) {
            // Create marker on first update
            driverMarker = Marker(mapView)
            driverMarker?.title = "Driver"
            driverMarker?.snippet = "Your delivery is on the way"
            driverMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(driverMarker)
            driverMarker?.position = newPosition
        } else {
            // Animate marker to new position
            driverMarker?.let { marker ->
                MarkerAnimator.animateMarkerToPosition(marker, newPosition, mapView, 1000L)
            }
        }
        
        lastDriverLocation = newPosition
        
        // Update route line
        updateRouteLine(newPosition)
        
        // Update distance and ETA
        updateDistanceAndETA(newPosition)
        
        // Auto-follow driver with smooth camera movement
        MarkerAnimator.animateCameraToPosition(mapView, newPosition, duration = 1500L)
        
        Log.d(TAG, "Driver location updated: ${location.latitude}, ${location.longitude}")
    }
    
    private fun updateRouteLine(driverPosition: GeoPoint) {
        clientLocation?.let { clientGeo ->
            if (routeLine == null) {
                routeLine = Polyline()
                routeLine?.outlinePaint?.color = Color.BLUE
                routeLine?.outlinePaint?.strokeWidth = 10f
                mapView.overlays.add(0, routeLine) // Add at index 0 so it's below markers
            }
            
            // Update route points (simple straight line for now)
            val points = listOf(driverPosition, clientGeo)
            routeLine?.setPoints(points)
            
            mapView.invalidate()
        }
    }
    
    private fun updateDistanceAndETA(driverPosition: GeoPoint) {
        clientLocation?.let { clientGeo ->
            val distance = NavigationHelper.calculateDistance(driverPosition, clientGeo)
            val eta = NavigationHelper.calculateETA(distance)
            
            tvDistance.text = NavigationHelper.formatDistance(distance)
            tvETA.text = NavigationHelper.formatETA(eta)
            
            // Update driver info with proximity message
            if (distance < 0.5) {
                tvDriverInfo.text = "Driver is arriving soon!"
            } else if (distance < 2.0) {
                tvDriverInfo.text = "Driver is ${NavigationHelper.formatDistance(distance)} away"
            } else {
                tvDriverInfo.text = "Driver is on the way"
            }
        }
    }
    
    private fun updateDeliveryTimeline(status: String) {
        // Reset all steps
        timelineStep1.setBackgroundColor(Color.parseColor("#E0E0E0"))
        timelineStep2.setBackgroundColor(Color.parseColor("#E0E0E0"))
        timelineStep3.setBackgroundColor(Color.parseColor("#E0E0E0"))
        
        // Update based on status
        when (status.uppercase()) {
            "PENDING", "DELIVERY_ACCEPTED", "READY_FOR_PICKUP" -> {
                timelineStep1.setBackgroundColor(Color.parseColor("#4CAF50"))
            }
            "PICKED_UP", "IN_DELIVERY", "ON_THE_WAY" -> {
                timelineStep1.setBackgroundColor(Color.parseColor("#4CAF50"))
                timelineStep2.setBackgroundColor(Color.parseColor("#4CAF50"))
            }
            "DELIVERED", "COMPLETED" -> {
                timelineStep1.setBackgroundColor(Color.parseColor("#4CAF50"))
                timelineStep2.setBackgroundColor(Color.parseColor("#4CAF50"))
                timelineStep3.setBackgroundColor(Color.parseColor("#4CAF50"))
            }
        }
    }
    
    private fun startPeriodicUpdates() {
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                // Periodically adjust map view to show both markers
                lastDriverLocation?.let { driverPos ->
                    clientLocation?.let { clientPos ->
                        val points = listOf(driverPos, clientPos)
                        val boundingBox = BoundingBox.fromGeoPoints(points)
                        mapView.post {
                            mapView.zoomToBoundingBox(boundingBox, true, 150)
                        }
                    }
                }
                delay(10000L) // Every 10 seconds
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        mapView.onDetach()
        try {
            stompClient?.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }
    
    // Helper DTO for internal use
    data class LocationUpdateDto(val latitude: Double, val longitude: Double)
}
