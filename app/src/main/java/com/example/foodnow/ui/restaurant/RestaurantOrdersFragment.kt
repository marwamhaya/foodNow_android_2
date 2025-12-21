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

import androidx.navigation.fragment.findNavController

import androidx.fragment.app.activityViewModels

class RestaurantOrdersFragment : Fragment(R.layout.fragment_restaurant_orders) {

    private val viewModel: RestaurantViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }
    private lateinit var adapter: RestaurantOrderAdapter
    private var allOrders: List<com.example.foodnow.data.Order> = emptyList()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvRestaurantOrders)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val tvEmpty = view.findViewById<android.widget.TextView>(R.id.tvEmptyOrders)
        
        adapter = RestaurantOrderAdapter(
            emptyList(), 
            onAction1Click = { order ->
                android.util.Log.d("RestaurantOrders", "Action 1 clicked for order ${order.id} with status ${order.status}")
                when (order.status) {
                    "PENDING" -> viewModel.acceptOrder(order.id)
                    "ACCEPTED" -> viewModel.prepareOrder(order.id)
                    "PREPARING" -> viewModel.readyOrder(order.id)
                }
            },
            onAction2Click = { order ->
                android.util.Log.d("RestaurantOrders", "Action 2 clicked for order ${order.id}")
                viewModel.rejectOrder(order.id, "Declined by restaurant")
            },
            onItemClick = { order ->
                // Navigate to order details
                val bundle = Bundle().apply {
                    putLong("orderId", order.id)
                }
                findNavController().navigate(R.id.action_orders_to_details, bundle)
            }
        )
        
        recyclerView.adapter = adapter
        
        val tabLayout = view.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayoutOrders)
        tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                filterOrders(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        viewModel.orders.observe(viewLifecycleOwner) { result ->
            result.onSuccess { orders ->
                allOrders = orders
                filterOrders(tabLayout.selectedTabPosition)
            }.onFailure {
                Toast.makeText(context, "Error loading orders", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.orderActionStatus.observe(viewLifecycleOwner) { result ->
            result.onSuccess { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(context, "Action failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.getOrders()
    }

    private fun filterOrders(tabIndex: Int) {
        val filtered = when (tabIndex) {
            0 -> allOrders // All
            1 -> allOrders.filter { it.status == "PENDING" } // Pending
            2 -> allOrders.filter { it.status == "ACCEPTED" || it.status == "PREPARING" || it.status == "IN_DELIVERY" } // In Progress
            3 -> allOrders.filter { it.status == "READY_FOR_PICKUP" } // Ready
            4 -> allOrders.filter { it.status == "DELIVERED" } // Delivered
            5 -> allOrders.filter { it.status == "CANCELLED" || it.status == "DECLINED" } // Cancelled
            else -> allOrders
        }
        
        // Sort: oldest first, non-completed orders first
        val sorted = filtered.sortedWith(
            compareBy<com.example.foodnow.data.Order> { 
                when (it.status) {
                    "PENDING" -> 0
                    "ACCEPTED" -> 1
                    "PREPARING" -> 2
                    "READY_FOR_PICKUP" -> 3
                    "IN_DELIVERY" -> 4
                    "DELIVERED" -> 5
                    "CANCELLED", "DECLINED" -> 6
                    else -> 7
                }
            }.thenBy { it.createdAt } // Oldest first within same priority
        )
        
        adapter.updateOrders(sorted)
        val tvEmpty = view?.findViewById<android.widget.TextView>(R.id.tvEmptyOrders)
        val recyclerView = view?.findViewById<RecyclerView>(R.id.rvRestaurantOrders)
        
        if (sorted.isEmpty()) {
            tvEmpty?.visibility = View.VISIBLE
            recyclerView?.visibility = View.GONE
        } else {
            tvEmpty?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
        }
    }
}
