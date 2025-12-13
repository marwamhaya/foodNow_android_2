package com.example.foodnow.ui.restaurant

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.ui.ViewModelFactory

class RestaurantOrdersFragment : Fragment(R.layout.fragment_restaurant_orders) {

    private val viewModel: RestaurantViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }
    private lateinit var adapter: RestaurantOrderAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvRestaurantOrders)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = RestaurantOrderAdapter(emptyList(), 
            onAction1Click = { order ->
                when (order.status) {
                    "PENDING" -> viewModel.acceptOrder(order.id)
                    "ACCEPTED" -> viewModel.prepareOrder(order.id)
                    "PREPARING" -> viewModel.readyOrder(order.id)
                }
            },
            onAction2Click = { order ->
                if (order.status == "PENDING") {
                     viewModel.rejectOrder(order.id, "Busy") // Simple reject logic
                }
            }
        )
        recyclerView.adapter = adapter

        viewModel.orders.observe(viewLifecycleOwner) { result ->
            result.onSuccess { orders ->
                adapter.updateOrders(orders)
            }.onFailure {
                Toast.makeText(context, "Error loading orders", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.getOrders()
    }
}
