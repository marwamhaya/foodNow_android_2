package com.example.foodnow.ui.restaurant

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.ui.ViewModelFactory

class OrderDetailsFragment : Fragment(R.layout.fragment_order_details) {

    private val viewModel: RestaurantViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    private var orderId: Long = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderId = arguments?.getLong("orderId", -1L) ?: -1L
        if (orderId == -1L) {
             Toast.makeText(context, "Invalid order ID", Toast.LENGTH_SHORT).show()
             return
        }

        val tvId = view.findViewById<TextView>(R.id.tvOrderId)
        val tvDate = view.findViewById<TextView>(R.id.tvOrderDate)
        val tvStatus = view.findViewById<TextView>(R.id.tvOrderStatus)
        val tvTotal = view.findViewById<TextView>(R.id.tvTotalAmount)
        val tvClientName = view.findViewById<TextView>(R.id.tvClientName)
        val tvClientPhone = view.findViewById<TextView>(R.id.tvClientPhone)
        val tvDeliveryAddress = view.findViewById<TextView>(R.id.tvDeliveryAddress)
        val rvItems = view.findViewById<RecyclerView>(R.id.rvOrderItems)
        val btnAction1 = view.findViewById<Button>(R.id.btnAction1)
        val btnAction2 = view.findViewById<Button>(R.id.btnAction2)

        rvItems.layoutManager = LinearLayoutManager(context)

        viewModel.orders.observe(viewLifecycleOwner) { result ->
             val order = result.getOrNull()?.find { it.id == orderId }
             if (order != null) {
                 tvId.text = "Order #${order.id}"
                 tvDate.text = "Date: ${order.createdAt.take(16).replace("T", " ")}"
                 tvStatus.text = order.status
                 tvTotal.text = "${String.format("%.2f", order.totalAmount)} DH"
                 tvClientName.text = "Client: ${order.clientName ?: "Unknown"}"
                 tvClientPhone?.text = "Phone: ${order.clientPhone ?: "N/A"}"
                 tvDeliveryAddress?.text = "Delivery: ${order.deliveryAddress ?: "N/A"}"
                 
                 // Update buttons based on status - only show for PENDING
                 updateButtons(order.status, btnAction1, btnAction2)
                 
                 // Set items adapter
                 if (order.items.isNotEmpty()) {
                     rvItems.adapter = OrderItemsAdapter(order.items)
                 } else {
                     // Show empty message
                     Toast.makeText(context, "No items in order", Toast.LENGTH_SHORT).show()
                 }
             }
        }

        viewModel.orderActionStatus.observe(viewLifecycleOwner) { result ->
            result.onSuccess { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                viewModel.getOrders() // Refresh to update status
            }.onFailure { e ->
                Toast.makeText(context, "Action failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        btnAction1.setOnClickListener {
             val order = viewModel.orders.value?.getOrNull()?.find { it.id == orderId } ?: return@setOnClickListener
             when(order.status) {
                 "PENDING" -> viewModel.acceptOrder(orderId)
                 "ACCEPTED" -> viewModel.prepareOrder(orderId)
                 "PREPARING" -> viewModel.readyOrder(orderId)
             }
        }
        
        btnAction2.setOnClickListener {
             val order = viewModel.orders.value?.getOrNull()?.find { it.id == orderId } ?: return@setOnClickListener
             if (order.status == "PENDING") {
                 viewModel.rejectOrder(orderId, "Declined by restaurant")
             }
        }
    }

    private fun updateButtons(status: String, btn1: Button, btn2: Button) {
        when(status) {
            "PENDING" -> {
                btn1.text = "Accept Order"
                btn1.visibility = View.VISIBLE
                btn2.text = "Reject Order"
                btn2.visibility = View.VISIBLE
            }
            "ACCEPTED" -> {
                btn1.text = "Start Preparing"
                btn1.visibility = View.VISIBLE
                btn2.visibility = View.GONE
            }
            "PREPARING" -> {
                btn1.text = "Mark Ready"
                btn1.visibility = View.VISIBLE
                btn2.visibility = View.GONE
            }
            else -> {
                btn1.visibility = View.GONE
                btn2.visibility = View.GONE
            }
        }
    }
}
