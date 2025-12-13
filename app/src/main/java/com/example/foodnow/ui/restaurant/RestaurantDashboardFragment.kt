package com.example.foodnow.ui.restaurant

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.ui.ViewModelFactory

class RestaurantDashboardFragment : Fragment(R.layout.fragment_restaurant_dashboard) {

    private val viewModel: RestaurantViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvName = view.findViewById<TextView>(R.id.tvRestaurantName)
        val tvAddress = view.findViewById<TextView>(R.id.tvRestaurantAddress)
        val tvStatus = view.findViewById<TextView>(R.id.tvRestaurantStatus)
        val tvDesc = view.findViewById<TextView>(R.id.tvRestaurantDescription)
        val btnRefresh = view.findViewById<Button>(R.id.btnRefresh)
        
        btnRefresh.setOnClickListener {
            viewModel.getMyRestaurant()
        }

        viewModel.restaurant.observe(viewLifecycleOwner) { result ->
            result.onSuccess { restaurant ->
                tvName.text = restaurant.name
                tvAddress.text = restaurant.address ?: "No address"
                tvStatus.text = if (restaurant.isActive) "Status: OPEN" else "Status: CLOSED"
                tvDesc.text = restaurant.description ?: "No description"
            }.onFailure {
                tvName.text = "Error loading restaurant"
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Initial Load
        viewModel.getMyRestaurant()
    }
}
