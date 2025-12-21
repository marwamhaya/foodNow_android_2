package com.example.foodnow.ui.livreur

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.ui.ViewModelFactory
import androidx.navigation.fragment.findNavController

class LivreurDashboardFragment : Fragment(R.layout.fragment_livreur_dashboard) {

    private val viewModel: LivreurViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }
    private lateinit var adapter: DeliveryAdapter
    
    // Store pending action for after permission granted
    private var pendingDeliveryAction: (() -> Unit)? = null
    
    // Permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            // Permission granted, execute pending action
            pendingDeliveryAction?.invoke()
            pendingDeliveryAction = null
        } else {
            Toast.makeText(context, "Location permission required for delivery tracking", Toast.LENGTH_LONG).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvDeliveries)
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = DeliveryAdapter(emptyList()) { delivery ->
            when (delivery.status) {
                "PENDING" -> viewModel.acceptDeliveryRequest(delivery.id)
                "DELIVERY_ACCEPTED", "READY_FOR_PICKUP" -> {
                    // Check permission before starting location service
                    startDeliveryWithPermissionCheck(delivery.id, delivery.orderId)
                }
                "PICKED_UP", "IN_DELIVERY", "ON_THE_WAY" -> {
                    // Navigate to Map
                    val bundle = Bundle().apply { putLong("delivery_id", delivery.id) }
                    try {
                        findNavController().navigate(R.id.nav_active_delivery, bundle)
                    } catch (e: Exception) {
                         parentFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment_livreur, ActiveDeliveryFragment().apply { arguments = bundle })
                            .addToBackStack(null)
                            .commit()
                    }
                }
            }
        }
        recyclerView.adapter = adapter

        // Observer for Assigned Deliveries
        viewModel.deliveries.observe(viewLifecycleOwner) { result ->
            result.onSuccess { assignedList ->
                // Merge with available
            }
        }
        
        // Observer for Available Requests
        viewModel.availableRequests.observe(viewLifecycleOwner) { result ->
             result.onSuccess { availableList ->
                 val assigned = viewModel.deliveries.value?.getOrNull() ?: emptyList()
                 val merged = assigned + availableList
                 adapter.updateDeliveries(merged)
             }
        }
        
        viewModel.deliveries.observe(viewLifecycleOwner) { 
             val assigned = it.getOrNull() ?: emptyList()
             val available = viewModel.availableRequests.value?.getOrNull() ?: emptyList()
             adapter.updateDeliveries(assigned + available)
        }

        viewModel.requestActionStatus.observe(viewLifecycleOwner) { result ->
             result.onSuccess { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
             result.onFailure { e -> Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
        }

        viewModel.getAssignedDeliveries()
        viewModel.getAvailableRequests()
    }
    
    private fun startDeliveryWithPermissionCheck(deliveryId: Long, orderId: Long) {
        if (hasLocationPermission()) {
            startDeliveryTracking(deliveryId, orderId)
        } else {
            // Store action and request permission
            pendingDeliveryAction = { startDeliveryTracking(deliveryId, orderId) }
            requestLocationPermission()
        }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), 
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            requireContext(), 
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }
    
    private fun startDeliveryTracking(deliveryId: Long, orderId: Long) {
        // Update status
        viewModel.updateStatus(deliveryId, "PICKED_UP")
        
        // Start location service
        val intent = android.content.Intent(requireContext(), com.example.foodnow.service.LocationService::class.java)
        intent.putExtra("order_id", orderId)
        
        // For Android 8+ use startForegroundService
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
        
        // Navigate to map
        val bundle = Bundle().apply { putLong("delivery_id", deliveryId) }
        try {
            findNavController().navigate(R.id.nav_active_delivery, bundle)
        } catch (e: Exception) {
             parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment_livreur, ActiveDeliveryFragment().apply { arguments = bundle })
                .addToBackStack(null)
                .commit()
        }
    }
}
