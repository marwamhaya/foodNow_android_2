package com.example.foodnow.ui.restaurant

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.ui.ViewModelFactory
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView

class RestaurantDashboardFragment : Fragment(R.layout.fragment_restaurant_dashboard) {

    private val viewModel: RestaurantViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvName = view.findViewById<TextView>(R.id.tvRestaurantName)
        val tvAddress = view.findViewById<TextView>(R.id.tvRestaurantAddress)
        val tvStatus = view.findViewById<TextView>(R.id.tvRestaurantStatus)
        val tvDesc = view.findViewById<TextView>(R.id.tvRestaurantDescription)
        val btnRefresh = view.findViewById<Button>(R.id.btnRefresh)
        val ivImage = view.findViewById<android.widget.ImageView>(R.id.ivRestaurantImage)
        

        val btnEditProfile = view.findViewById<Button>(R.id.btnEditProfile)
        
        btnRefresh.setOnClickListener {
            viewModel.getMyRestaurant()
            viewModel.fetchStats()
        }

        btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_profile)
        }

        viewModel.restaurant.observe(viewLifecycleOwner) { result ->
            result.onSuccess { restaurant ->
                tvName.text = restaurant.name
                tvAddress.text = restaurant.address ?: "No address"
                tvStatus.text = if (restaurant.isActive) "Status: OPEN" else "Status: CLOSED"
                tvDesc.text = restaurant.description ?: "No description"
                
                // Load restaurant image
                if (!restaurant.imageUrl.isNullOrEmpty()) {
                    val fullUrl = com.example.foodnow.utils.Constants.getFullImageUrl(restaurant.imageUrl)
                    com.bumptech.glide.Glide.with(this)
                        .load(fullUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(ivImage)
                }
            }.onFailure {
                tvName.text = "Error loading restaurant"
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Stats views
        val tvTotalOrders = view.findViewById<TextView>(R.id.tvTotalOrders)
        val tvTotalRevenue = view.findViewById<TextView>(R.id.tvTotalRevenue)
        val tvAverageRating = view.findViewById<TextView>(R.id.tvAverageRating)
        val tvTotalClients = view.findViewById<TextView>(R.id.tvTotalClients)

        viewModel.stats.observe(viewLifecycleOwner) { result ->
            result.onSuccess { stats ->
                tvTotalOrders.text = stats.totalOrders.toString()
                tvTotalRevenue.text = String.format("%.2f DH", stats.totalRevenue)
                tvAverageRating.text = String.format("%.1f", stats.averageRating)
                tvTotalClients.text = stats.totalClients.toString()
            }.onFailure {
                Toast.makeText(context, "Stats Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Stats card click listeners - navigate to orders with specific filter
        val cardTotalOrders = view.findViewById<MaterialCardView>(R.id.cardTotalOrders)
        val cardTotalRevenue = view.findViewById<MaterialCardView>(R.id.cardTotalRevenue)
        val cardAvgRating = view.findViewById<MaterialCardView>(R.id.cardAvgRating)
        val cardTotalClients = view.findViewById<MaterialCardView>(R.id.cardTotalClients)

        // Tab indices: 0=All, 1=Pending, 2=In Progress, 3=Ready, 4=Delivered, 5=Cancelled
        cardTotalOrders?.setOnClickListener {
            navigateToOrdersWithFilter(0) // All orders
        }
        
        cardTotalRevenue?.setOnClickListener {
            navigateToOrdersWithFilter(4) // Delivered (revenue from completed orders)
        }
        
        cardAvgRating?.setOnClickListener {
            // Navigate to ratings page
            findNavController().navigate(R.id.action_menu_to_ratings)
        }
        
        cardTotalClients?.setOnClickListener {
            navigateToOrdersWithFilter(4) // Delivered (clients who completed orders)
        }

        // Initial Load
        viewModel.getMyRestaurant()
        viewModel.fetchStats()
    }
    
    private fun navigateToOrdersWithFilter(filterTabIndex: Int) {
        val bundle = Bundle().apply {
            putInt("filterTab", filterTabIndex)
        }
        findNavController().navigate(R.id.restaurantOrdersFragment, bundle)
    }
}
