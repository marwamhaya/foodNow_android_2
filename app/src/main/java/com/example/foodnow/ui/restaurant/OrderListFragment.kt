package com.example.foodnow.ui.restaurant

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.ui.ViewModelFactory

class OrderListFragment : Fragment(R.layout.fragment_order_list) {

    private val viewModel: RestaurantViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }
    private lateinit var adapter: RestaurantOrderAdapter
    private var statusIndex: Int = 0

    companion object {
        fun newInstance(statusIndex: Int): OrderListFragment {
            val fragment = OrderListFragment()
            val args = Bundle()
            args.putInt("statusIndex", statusIndex)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusIndex = arguments?.getInt("statusIndex") ?: 0

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvOrders)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)
        val progressBar = view.findViewById<View>(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = RestaurantOrderAdapter(
            emptyList(),
            onAction1Click = { order ->
                when (order.status) {
                    "PENDING" -> viewModel.acceptOrder(order.id)
                    "ACCEPTED" -> viewModel.prepareOrder(order.id)
                    "PREPARING" -> viewModel.readyOrder(order.id)
                }
            },
            onAction2Click = { order ->
                viewModel.rejectOrder(order.id, "Declined by restaurant")
            },
            onItemClick = { order ->
                val bundle = Bundle().apply {
                    putLong("orderId", order.id)
                }
                findNavController().navigate(R.id.action_orders_to_details, bundle)
            }
        )
        recyclerView.adapter = adapter

        viewModel.orders.observe(viewLifecycleOwner) { result ->
            result.onSuccess { orders ->
                filterAndDisplayOrders(orders, tvEmpty, recyclerView)
            }.onFailure {
                Toast.makeText(context, "Error loading orders", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterAndDisplayOrders(allOrders: List<com.example.foodnow.data.Order>, tvEmpty: TextView, recyclerView: RecyclerView) {
        val filtered = when (statusIndex) {
            0 -> allOrders // All
            1 -> allOrders.filter { it.status == "PENDING" } // Pending
            2 -> allOrders.filter { it.status == "ACCEPTED" || it.status == "PREPARING" || it.status == "IN_DELIVERY" } // In Progress
            3 -> allOrders.filter { it.status == "READY_FOR_PICKUP" } // Ready
            4 -> allOrders.filter { it.status == "DELIVERED" } // Delivered
            5 -> allOrders.filter { it.status == "CANCELLED" || it.status == "DECLINED" } // Cancelled
            else -> allOrders
        }

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
            }.thenByDescending { it.createdAt } // Recent first
        )

        adapter.updateOrders(sorted)
        if (sorted.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}
