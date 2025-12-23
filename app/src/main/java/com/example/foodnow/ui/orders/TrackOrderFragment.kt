package com.example.foodnow.ui.orders

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.coroutineScope
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.data.DeliveryResponse
import com.example.foodnow.data.LocationUpdateDto
import com.example.foodnow.service.NominatimGeocodingService
import com.example.foodnow.ui.ViewModelFactory
import com.example.foodnow.ui.livreur.LivreurViewModel
import com.example.foodnow.utils.MarkerAnimator
import com.example.foodnow.utils.NavigationHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
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

    // UI Elements
    private lateinit var mapView: MapView
    private lateinit var cardConnectionStatus: CardView
    private lateinit var tvConnectionStatus: TextView
    private lateinit var btnRecenter: FloatingActionButton
    
    // Bottom Sheet Info
    private lateinit var tvETAHeader: TextView
    private lateinit var tvRestaurantName: TextView
    private lateinit var tvOrderAmount: TextView
    private lateinit var tvClientAddress: TextView
    private lateinit var tvDriverName: TextView
    private lateinit var tvDriverRole: TextView
    private lateinit var ivDriverAvatar: ImageView
    private lateinit var btnCallDriver: CardView
    private lateinit var btnChatDriver: CardView

    // Logic Variables
    private var stompClient: StompClient? = null
    private val gson = Gson()
    private var driverMarker: Marker? = null
    private var clientMarker: Marker? = null
    private var routeLine: Polyline? = null
    private var clientLocation: GeoPoint? = null
    private var lastDriverLocation: GeoPoint? = null
    private var updateJob: Job? = null
    private var reconnectAttempts = 0
    private var currentDriverPhone: String? = null
    
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
        setupListeners()
        
        // Check permissions
        checkAndRequestLocationPermission()
        
        // Fetch delivery details
        val orderId = arguments?.getLong("orderId") ?: 1L
        fetchDeliveryDetails(orderId)
        
        connectStomp()
        
        // Start periodic UI updates
        startPeriodicUpdates()
    }
    
    private fun initializeViews(view: View) {
        mapView = view.findViewById(R.id.mapView)
        cardConnectionStatus = view.findViewById(R.id.cardConnectionStatus)
        tvConnectionStatus = view.findViewById(R.id.tvConnectionStatus)
        btnRecenter = view.findViewById(R.id.btnRecenter)
        
        tvETAHeader = view.findViewById(R.id.tvETAHeader)
        tvRestaurantName = view.findViewById(R.id.tvRestaurantName)
        tvOrderAmount = view.findViewById(R.id.tvOrderAmount)
        tvClientAddress = view.findViewById(R.id.tvClientAddress)
        tvDriverName = view.findViewById(R.id.tvDriverName)
        tvDriverRole = view.findViewById(R.id.tvDriverRole)
        ivDriverAvatar = view.findViewById(R.id.ivDriverAvatar)
        btnCallDriver = view.findViewById(R.id.btnCallDriver)
        btnChatDriver = view.findViewById(R.id.btnChatDriver)
    }
    
    private fun initializeMap() {
        Configuration.getInstance().userAgentValue = requireContext().packageName
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        // Default center Casablanca
        mapView.controller.setCenter(GeoPoint(33.5731, -7.5898))
    }
    
    private fun setupListeners() {
        btnRecenter.setOnClickListener {
            recenterMap()
        }
        
        btnCallDriver.setOnClickListener {
            currentDriverPhone?.let { phone ->
                 val intent = Intent(Intent.ACTION_DIAL)
                 intent.data = Uri.parse("tel:$phone")
                 startActivity(intent)
            } ?: run {
                Toast.makeText(context, "Driver phone not available", Toast.LENGTH_SHORT).show()
            }
        }
        
        btnChatDriver.setOnClickListener {
            Toast.makeText(context, "Chat feature coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectStomp() {
        showConnectionStatus("Connecting...", "#FF9800")
        
        val ctx = context ?: return
        val tokenManager = com.example.foodnow.data.TokenManager(ctx)
        val token = tokenManager.getToken()
        
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "No auth token available for WebSocket")
            showConnectionStatus("Authentication Required", "#F44336")
            return
        }
        
        val url = com.example.foodnow.utils.Constants.WS_URL
        
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
                        showConnectionStatus("Live", "#4CAF50")
                        reconnectAttempts = 0
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(2000)
                            cardConnectionStatus.visibility = View.GONE
                        }
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
                    activity?.runOnUiThread { 
                        handleConnectionLoss()
                    }
                }
                else -> {}
            }
        }, { error ->
            Log.e(TAG, "Lifecycle subscription error", error)
        })

        val orderId = arguments?.getLong("orderId") ?: 1L
        
        // Location Updates
        stompClient?.topic("/topic/delivery/$orderId/location")?.subscribe({ topicMessage ->
            try {
                val location = gson.fromJson(topicMessage.payload, LocationUpdateDto::class.java)
                requireActivity().runOnUiThread {
                    updateDriverLocation(location)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing location", e)
            }
        }, { error ->
            Log.e(TAG, "Location subscription error", error)
        })
        
        // Status Updates (DeliveryResponse)
        stompClient?.topic("/topic/delivery/$orderId/status")?.subscribe({ topicMessage ->
             requireActivity().runOnUiThread {
                 try {
                     val response = gson.fromJson(topicMessage.payload, DeliveryResponse::class.java)
                     if (response.driverName != null) {
                         tvDriverName.text = response.driverName
                         currentDriverPhone = response.driverPhone
                     }
                     
                     if (response.status == "DELIVERED") {
                         tvETAHeader.text = "Order Delivered!"
                         tvETAHeader.setTextColor(Color.parseColor("#4CAF50")) // Green for success
                         tvDriverRole.text = "Enjoy your meal!" // Use available view
                         
                         // Show clear indication
                         Toast.makeText(context, "Your order has been delivered!", Toast.LENGTH_LONG).show()
                         
                         // Stop updates
                         updateJob?.cancel()
                     }
                 } catch (e: Exception) {
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
            CoroutineScope(Dispatchers.IO).launch {
                delay(RECONNECT_DELAY)
                withContext(Dispatchers.Main) {
                    try {
                        stompClient?.disconnect()
                        connectStomp()
                    } catch (e: Exception) {
                         // Ignore
                    }
                }
            }
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
    
    private fun fetchDeliveryDetails(orderId: Long) {
        viewLifecycleOwner.lifecycle.coroutineScope.launch {
            try {
                val repository = (requireActivity().application as FoodNowApp).repository
                // Get Client orders
                val response = repository.getMyOrders()
                if (response.isSuccessful && response.body() != null) {
                    val orders = response.body()!!
                    val order = orders.find { it.id == orderId }
                    
                    if (order != null) {
                        val address = order.deliveryAddress ?: "Unknown Address"
                        tvRestaurantName.text = order.restaurantName
                        tvOrderAmount.text = "Total: ${order.totalAmount} DH"
                        tvClientAddress.text = "Drop-off: $address"
                        
                        if (!order.driverName.isNullOrEmpty()) {
                            tvDriverName.text = order.driverName
                            currentDriverPhone = order.driverPhone
                            tvDriverRole.text = "Delivery Partner"
                        } else {
                            tvDriverName.text = "Searching Driver..."
                            tvDriverRole.text = "Waiting for assignment"
                        }
                        
                        // GPS or Geocoding
                        try {
                            val locResponse = repository.getOrderLocation(orderId)
                            if (locResponse.isSuccessful && locResponse.body() != null) {
                                val loc = locResponse.body()!!
                                val geoPoint = GeoPoint(loc.clientLatitude, loc.clientLongitude)
                                clientLocation = geoPoint
                                addClientMarkerAt(geoPoint, address)
                                // Also check for initial driver location
                                if (loc.driverLatitude != null && loc.driverLongitude != null) {
                                    updateDriverLocation(LocationUpdateDto(loc.driverLatitude, loc.driverLongitude))
                                }
                            } else {
                                geocodeAndAddMarker(address)
                            }
                        } catch (e: Exception) {
                            geocodeAndAddMarker(address)
                        }
                    } 
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching order details", e)
            }
        }
    }

    private fun geocodeAndAddMarker(address: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val location = NominatimGeocodingService.geocode(address)
                if (location != null) {
                    withContext(Dispatchers.Main) {
                        val geoPoint = GeoPoint(location.latitude, location.longitude)
                        clientLocation = geoPoint
                        addClientMarkerAt(geoPoint, address)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Geocoding failed", e)
            }
        }
    }

    private fun addClientMarkerAt(geoPoint: GeoPoint, address: String) {
        clientMarker = Marker(mapView)
        clientMarker?.position = geoPoint
        clientMarker?.title = "My Location"
        clientMarker?.snippet = address
        clientMarker?.icon = resources.getDrawable(android.R.drawable.ic_menu_myplaces, null) 
        // Tint Orange
        clientMarker?.icon?.setTint(Color.parseColor("#FF6200"))
        
        clientMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(clientMarker)
        
        mapView.controller.setCenter(geoPoint)
        mapView.invalidate()
    }

    // ... 

    private fun updateDriverLocation(location: LocationUpdateDto) {
        val newPosition = GeoPoint(location.latitude, location.longitude)
        
        if (driverMarker == null) {
            driverMarker = Marker(mapView)
            driverMarker?.title = "Driver"
            driverMarker?.icon = resources.getDrawable(android.R.drawable.ic_menu_mylocation, null)
            // Tint Orange
            driverMarker?.icon?.setTint(Color.parseColor("#FF6200"))
            
            driverMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(driverMarker)
            driverMarker?.position = newPosition
        } else {
            // ... (animate)
             driverMarker?.let { marker ->
                MarkerAnimator.animateMarkerToPosition(marker, newPosition, mapView, 1000L)
            }
        }
        
        lastDriverLocation = newPosition
        updateRouteLine(newPosition)
        updateDistanceAndETA(newPosition)
    }
    
    private fun updateRouteLine(driverPosition: GeoPoint) {
        clientLocation?.let { clientGeo ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val road = NavigationHelper.calculateRoute(requireContext(), driverPosition, clientGeo)
                    if (road != null) {
                        withContext(Dispatchers.Main) {
                            if (routeLine != null) mapView.overlays.remove(routeLine)
                            
                            // Orange color for route
                            routeLine = NavigationHelper.createRoutePolyline(road, Color.parseColor("#FF6200"), 15f)
                            mapView.overlays.add(0, routeLine)
                            mapView.invalidate()
                        }
                    } else {
                         withContext(Dispatchers.Main) { drawStraightLine(driverPosition, clientGeo) }
                    }
                } catch (e: Exception) {
                     withContext(Dispatchers.Main) { drawStraightLine(driverPosition, clientGeo) }
                }
            }
        }
    }

    private fun drawStraightLine(start: GeoPoint, end: GeoPoint) {
         if (routeLine == null) {
            routeLine = Polyline()
            routeLine?.outlinePaint?.color = Color.parseColor("#FF6200")
            routeLine?.outlinePaint?.strokeWidth = 10f
            mapView.overlays.add(0, routeLine)
         }
         routeLine?.setPoints(listOf(start, end))
         mapView.invalidate()
    }
    
    private fun updateDistanceAndETA(driverPosition: GeoPoint) {
        clientLocation?.let { clientGeo ->
            val distance = NavigationHelper.calculateDistance(driverPosition, clientGeo)
            val eta = NavigationHelper.calculateETA(distance)
            
            val etaString = NavigationHelper.formatETA(eta) 
            // formatETA usually returns "XX min" or "X h XX min"
            // We want "Arrives in XX minutes"
            
            // Just use the string for now, but improving format:
            tvETAHeader.text = "Arrives in $etaString"
        }
    }
    
    private fun recenterMap() {
        if (clientLocation != null && lastDriverLocation != null) {
            val points = listOf(clientLocation!!, lastDriverLocation!!)
            val boundingBox = BoundingBox.fromGeoPoints(points)
            mapView.zoomToBoundingBox(boundingBox, true, 150)
        } else if (clientLocation != null) {
            mapView.controller.animateTo(clientLocation)
            mapView.controller.setZoom(17.0)
        } else if (lastDriverLocation != null) {
            mapView.controller.animateTo(lastDriverLocation)
            mapView.controller.setZoom(17.0)
        }
    }
    
    private fun startPeriodicUpdates() {
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                // Keep map bounded only if user hasn't interracted recently?
                // For simplified UX, let's just update bounds if far off?
                // Or just rely on user clicking Recenter.
                // The image shows a Recenter button, implying the map is free to move.
                // So I removed the auto-force-center loop.
                delay(10000L)
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
        try { stompClient?.disconnect() } catch (e: Exception) {}
    }
}
