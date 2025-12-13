package com.example.foodnow.ui.orders

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.foodnow.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent

class TrackOrderFragment : Fragment(R.layout.fragment_track_order), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private var stompClient: StompClient? = null
    private val gson = Gson()
    private var driverId: Long = 0
    private lateinit var tvStatus: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // TODO: Get driverId from arguments
        // arguments?.getLong("driverId")?.let { driverId = it }
        // For testing, hardcoded or needs safe args
        driverId = 1 // Placeholder

        mapView = view.findViewById(R.id.mapView)
        tvStatus = view.findViewById(R.id.tvDriverInfo)
        
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        connectStomp()
    }

    private fun connectStomp() {
        // Use 10.0.2.2 for emulator localhost
        val url = "ws://10.0.2.2:8080/ws/websocket" 
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, url)
        
        stompClient?.lifecycle()?.subscribe { lifecycleEvent ->
            when (lifecycleEvent.type) {
                LifecycleEvent.Type.OPENED -> {
                    requireActivity().runOnUiThread { tvStatus.text = "Connected to Driver" }
                }
                LifecycleEvent.Type.ERROR -> {
                    Log.e("Stomp", "Error", lifecycleEvent.exception)
                    requireActivity().runOnUiThread { tvStatus.text = "Connection Error" }
                }
                else -> {}
            }
        }

        // Subscribe to /topic/drivers/{id}
        stompClient?.topic("/topic/drivers/$driverId")?.subscribe { topicMessage ->
            val payload = topicMessage.payload
            val location = gson.fromJson(payload, LocationUpdateDto::class.java)
            requireActivity().runOnUiThread {
                updateMarker(location)
            }
        }

        stompClient?.connect()
    }

    private fun updateMarker(location: LocationUpdateDto) {
        val latLng = LatLng(location.latitude, location.longitude)
        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(latLng).title("Driver"))
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        tvStatus.text = "Driver moving..."
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
        mapView.onDestroy()
        stompClient?.disconnect()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
    
    // Helper DTO for internal use if not imported
    data class LocationUpdateDto(val latitude: Double, val longitude: Double)
}
