package com.example.foodnow.ui.livreur

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.data.DeliveryResponse
import com.example.foodnow.service.NominatimGeocodingService
import com.example.foodnow.ui.ViewModelFactory
import com.example.foodnow.utils.NavigationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class ActiveDeliveryFragment : Fragment(R.layout.fragment_active_delivery) {

    private lateinit var mapView: MapView
    private lateinit var tvClientInfo: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvETA: TextView
    private lateinit var tvNextTurn: TextView
    private lateinit var progressRoute: ProgressBar
    private lateinit var cardNavigationInfo: CardView
    private lateinit var btnAction: Button
    private lateinit var btnStartDelivery: Button
    private lateinit var btnArrived: Button
    private lateinit var btnDelivered: Button
    
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private var clientMarker: Marker? = null
    private var routePolyline: Polyline? = null
    private var currentRoad: Road? = null
    private var clientLocation: GeoPoint? = null
    private var navigationJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    private val viewModel: LivreurViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    private var deliveryId: Long = -1
    private var currentDelivery: DeliveryResponse? = null
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val ROUTE_RECALCULATION_INTERVAL = 5000L // 5 seconds
        private const val TAG = "ActiveDelivery"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        
        arguments?.let {
            deliveryId = it.getLong("delivery_id", -1)
        }

        initializeMap()
        setupObservers()
        setupClickListeners()
        
        // Fetch delivery details
        viewModel.getAssignedDeliveries()
    }
    
    private fun initializeViews(view: View) {
        mapView = view.findViewById(R.id.mapView)
        tvClientInfo = view.findViewById(R.id.tvClientInfo)
        tvDistance = view.findViewById(R.id.tvDistance)
        tvETA = view.findViewById(R.id.tvETA)
        tvNextTurn = view.findViewById(R.id.tvNextTurn)
        progressRoute = view.findViewById(R.id.progressRoute)
        cardNavigationInfo = view.findViewById(R.id.cardNavigationInfo)
        btnAction = view.findViewById(R.id.btnAction)
        btnStartDelivery = view.findViewById(R.id.btnStartDelivery)
        btnArrived = view.findViewById(R.id.btnArrived)
        btnDelivered = view.findViewById(R.id.btnDelivered)
    }
    
    private fun setupClickListeners() {
        btnAction.setOnClickListener {
            currentDelivery?.let { delivery ->
                handleDeliveryAction(delivery)
            }
        }
        
        btnStartDelivery.setOnClickListener {
            startNavigation()
        }
        
        btnArrived.setOnClickListener {
            currentDelivery?.let { delivery ->
                // Visual feedback only? Or just stop navigation?
                // Backend doesn't support ARRIVED status.
                stopNavigation()
                Toast.makeText(context, "You have arrived!", Toast.LENGTH_SHORT).show()
            }
        }
        
        btnDelivered.setOnClickListener {
            currentDelivery?.let { delivery ->
                // Visual feedback
                btnDelivered.isEnabled = false
                btnDelivered.text = "Updating..."
                
                viewModel.updateStatus(delivery.id, "DELIVERED")
                stopNavigation()
            }
        }
    }
    
    private fun handleDeliveryAction(delivery: DeliveryResponse) {
        when(delivery.status) {
            "DELIVERY_ACCEPTED", "READY_FOR_PICKUP" -> {
                viewModel.updateStatus(delivery.id, "PICKED_UP")
                // Start location service
                val intent = android.content.Intent(requireContext(), com.example.foodnow.service.LocationService::class.java)
                intent.putExtra("order_id", delivery.orderId)
                requireContext().startService(intent)
            }
            "PICKED_UP", "IN_DELIVERY", "ON_THE_WAY" -> {
                // STOP Navigation immediately UI-wise
                stopNavigation()
                // Call API
                viewModel.updateStatus(delivery.id, "DELIVERED")
            }
        }
    }

    private fun initializeMap() {
        Configuration.getInstance().userAgentValue = requireContext().packageName
        
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(GeoPoint(33.5731, -7.5898))
        
        checkAndRequestLocationPermission()
    }
    
    private fun checkAndRequestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), mapView)
            myLocationOverlay?.enableMyLocation()
            myLocationOverlay?.enableFollowLocation()
            mapView.overlays.add(myLocationOverlay)
            Log.d(TAG, "My location overlay enabled")
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
                Toast.makeText(context, "Location permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Location permission denied. Cannot track your location.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupObservers() {
        viewModel.deliveries.observe(viewLifecycleOwner) { result ->
            result.onSuccess { list ->
                currentDelivery = list.find { it.id == deliveryId }
                currentDelivery?.let { updateUI(it) }
            }
        }
        
        viewModel.requestActionStatus.observe(viewLifecycleOwner) { result ->
             result.onSuccess { 
                 Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                 // Refresh
                 viewModel.getAssignedDeliveries()
             }
        }

        viewModel.statusUpdateResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, "Status Updated Successfully!", Toast.LENGTH_SHORT).show()
                
                // If the updated status was DELIVERED, verify and close
                currentDelivery?.let {
                    // Logic to handle cleanup after success
                    if (btnDelivered.visibility == View.VISIBLE) { // Determine if we just finished
                       // Ideally we check what status we just sent, but for now assuming if successful and we clicked finish..
                       // Better approach: Since we are observing a generic result, we should act based on local intention or the refreshed data.
                       // However, simply popping back stack on success is usually fine for this flow.
                    }
                }
                
                // Ensure services are stopped if needed (redundant check but safe)
                val intent = android.content.Intent(requireContext(), com.example.foodnow.service.LocationService::class.java)
                requireContext().stopService(intent)
                
                parentFragmentManager.popBackStack()
            }.onFailure {
                Toast.makeText(context, "Error updating status: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun updateUI(delivery: DeliveryResponse) {
        tvClientInfo.text = "Client: ${delivery.clientName}\n${delivery.clientAddress}\nPhone: ${delivery.clientPhone}"

        // NEW: ensure LocationService is running for this order whenever the screen is opened
        val intent = android.content.Intent(requireContext(), com.example.foodnow.service.LocationService::class.java)
        intent.putExtra("order_id", delivery.orderId)
        requireContext().startService(intent)
        
        // Update button visibility based on status
        when(delivery.status) {
            "DELIVERY_ACCEPTED", "READY_FOR_PICKUP" -> {
                btnAction.text = "Pick Up Order"
                btnStartDelivery.visibility = View.GONE
                btnArrived.visibility = View.GONE
                btnDelivered.visibility = View.GONE
            }
            "PICKED_UP", "IN_DELIVERY", "ON_THE_WAY" -> {
                btnAction.visibility = View.GONE
                btnStartDelivery.visibility = View.VISIBLE
                btnArrived.visibility = View.VISIBLE
                btnDelivered.visibility = View.VISIBLE
            }
            else -> {
                btnAction.visibility = View.GONE
                btnStartDelivery.visibility = View.GONE
                btnArrived.visibility = View.GONE
                btnDelivered.visibility = View.GONE
            }
        }
        
        // Fetch client GPS location from backend instead of geocoding
        fetchClientGPSLocation(delivery.orderId)
    }

    private fun fetchClientGPSLocation(orderId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Fetching GPS location for order: $orderId")
                val repository = (requireActivity().application as FoodNowApp).repository
                val response = repository.getOrderLocation(orderId)
                
                if (response.isSuccessful && response.body() != null) {
                    val location = response.body()!!
                    withContext(Dispatchers.Main) {
                        val geoPoint = GeoPoint(location.clientLatitude, location.clientLongitude)
                        clientLocation = geoPoint
                        
                        clientMarker = Marker(mapView)
                        clientMarker?.position = geoPoint
                        clientMarker?.title = "Client Location"
                        clientMarker?.snippet = "Delivery destination"
                        clientMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        
                        // Clear previous client markers
                        mapView.overlays.removeAll { it is Marker && it != myLocationOverlay }
                        mapView.overlays.add(clientMarker)
                        
                        // Center map on client location
                        mapView.controller.setCenter(geoPoint)
                        mapView.invalidate()
                        
                        Log.d(TAG, "Client GPS marker added at: ${location.clientLatitude}, ${location.clientLongitude}")
                    }
                } else {
                    Log.w(TAG, "Failed to fetch GPS location: ${response.code()}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Unable to get client location", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching GPS location", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error loading client location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun startNavigation() {
        val myLocation = myLocationOverlay?.myLocation
        val destination = clientLocation
        
        if (myLocation == null) {
            Toast.makeText(context, "Waiting for GPS location...", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (destination == null) {
            Toast.makeText(context, "Client location not available", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Acquire wake lock to keep screen on
        acquireWakeLock()
        
        // Show navigation card
        cardNavigationInfo.visibility = View.VISIBLE
        
        // Start route calculation and auto-recalculation
        navigationJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                // Get fresh location
                val currentLocation = myLocationOverlay?.myLocation
                if (currentLocation != null) {
                    calculateAndDisplayRoute(currentLocation, destination)
                }
                delay(ROUTE_RECALCULATION_INTERVAL)
            }
        }
    }
    
    private fun stopNavigation() {
        navigationJob?.cancel()
        navigationJob = null
        releaseWakeLock()
        cardNavigationInfo.visibility = View.GONE
        
        // Clear route polyline
        routePolyline?.let {
            mapView.overlays.remove(it)
            routePolyline = null
        }
        mapView.invalidate()
    }
    
    private suspend fun calculateAndDisplayRoute(start: GeoPoint, end: GeoPoint) {
        if (!isAdded || context == null) return

        withContext(Dispatchers.Main) {
            progressRoute.visibility = View.VISIBLE
        }
        
        val ctx = context ?: return
        val road = NavigationHelper.calculateRoute(ctx, start, end)
        
        withContext(Dispatchers.Main) {
            if (!isAdded) return@withContext
            progressRoute.visibility = View.GONE
            
            if (road != null) {
                currentRoad = road
                
                // Remove old route polyline
                routePolyline?.let { mapView.overlays.remove(it) }
                
                // Add new route polyline
                routePolyline = NavigationHelper.createRoutePolyline(road, Color.BLUE, 12f)
                mapView.overlays.add(0, routePolyline) // Add below markers
                
                // Update UI
                val distance = road.mLength // in km
                val duration = (road.mDuration / 60).toInt() // in minutes
                
                tvDistance.text = NavigationHelper.formatDistance(distance)
                tvETA.text = NavigationHelper.formatETA(duration)
                
                // Get next turn instruction
                if (road.mNodes.isNotEmpty() && road.mNodes[0].mInstructions.isNotEmpty()) {
                    tvNextTurn.text = road.mNodes[0].mInstructions
                } else {
                    tvNextTurn.text = "Continue to destination"
                }
                
                mapView.invalidate()
                
                Log.d(TAG, "Route updated: ${distance}km, ${duration}min")
            } else {
                Toast.makeText(context, "Unable to calculate route", Toast.LENGTH_SHORT).show()
                tvNextTurn.text = "Route calculation failed"
            }
        }
    }
    
    private fun acquireWakeLock() {
        val powerManager = ContextCompat.getSystemService(requireContext(), PowerManager::class.java)
        wakeLock = powerManager?.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "FoodNow:NavigationWakeLock"
        )
        wakeLock?.acquire(10*60*1000L /*10 minutes*/)
        Log.d(TAG, "Wake lock acquired")
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "Wake lock released")
            }
        }
        wakeLock = null
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        myLocationOverlay?.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        myLocationOverlay?.disableMyLocation()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopNavigation()
        mapView.onDetach()
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }
}
