package com.example.foodnow.ui.restaurant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.R
import com.example.foodnow.data.Order
import java.text.SimpleDateFormat
import java.util.Locale

class RestaurantOrderAdapter(
    private var orders: List<Order>,
    private val onAction1Click: (Order) -> Unit,
    private val onAction2Click: (Order) -> Unit,
    private val onItemClick: (Order) -> Unit = {}
) : RecyclerView.Adapter<RestaurantOrderAdapter.OrderViewHolder>() {

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvId: TextView = itemView.findViewById(R.id.tvOrderId)
        val tvStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
        val tvPrice: TextView = itemView.findViewById(R.id.tvOrderPrice)
        val tvClientName: TextView = itemView.findViewById(R.id.tvClientName)
        val tvOrderDate: TextView = itemView.findViewById(R.id.tvOrderDate)
        val btnAction1: Button = itemView.findViewById(R.id.btnAction1)
        val btnAction2: Button = itemView.findViewById(R.id.btnAction2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_restaurant_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.tvId.text = "Order #${order.id}"
        holder.tvStatus.text = order.status
        holder.tvPrice.text = "Total: ${String.format("%.2f", order.totalAmount)} DH"
        holder.tvClientName.text = "Client: ${order.clientName ?: "Unknown"}"
        
        // Format date
        try {
            holder.tvOrderDate.text = order.createdAt.take(16).replace("T", " • ")
        } catch (e: Exception) {
            holder.tvOrderDate.text = order.createdAt
        }

        // Format status for display
        val displayStatus = when (order.status) {
            "PENDING" -> "Pending"
            "ACCEPTED" -> "Accepted"
            "PREPARING" -> "Preparing"
            "READY_FOR_PICKUP" -> "Ready"
            "IN_DELIVERY" -> "In Delivery"
            "DELIVERED" -> "Delivered"
            "CANCELLED" -> "Cancelled"
            "DECLINED" -> "Declined"
            else -> order.status
        }
        holder.tvStatus.text = displayStatus
        
        // Status color
        val statusColor = when (order.status) {
            "PENDING" -> android.R.color.holo_orange_dark
            "ACCEPTED" -> android.R.color.holo_blue_dark
            "PREPARING" -> android.R.color.holo_purple
            "READY_FOR_PICKUP" -> android.R.color.holo_green_dark
            "IN_DELIVERY" -> android.R.color.holo_blue_light
            "DELIVERED" -> android.R.color.darker_gray
            "CANCELLED", "DECLINED" -> android.R.color.holo_red_dark
            else -> android.R.color.darker_gray
        }
        holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, statusColor))

        // Reset state for recycling
        holder.btnAction1.isEnabled = true
        holder.btnAction1.visibility = View.VISIBLE
        holder.btnAction2.isEnabled = true 
        holder.btnAction2.visibility = View.VISIBLE

        // Configure buttons based on status
        when (order.status) {
            "PENDING" -> {
                holder.btnAction1.text = "Accept"
                holder.btnAction2.text = "Reject"
            }
            "ACCEPTED" -> {
                holder.btnAction1.text = "Prepare"
                holder.btnAction2.visibility = View.GONE
            }
            "PREPARING" -> {
                holder.btnAction1.text = "Ready"
                holder.btnAction2.visibility = View.GONE
            }
            "READY_FOR_PICKUP" -> {
                holder.btnAction1.text = "Waiting Pickup"
                holder.btnAction1.isEnabled = false
                holder.btnAction2.visibility = View.GONE
            }
            "IN_DELIVERY" -> {
                holder.btnAction1.text = "In Delivery"
                holder.btnAction1.isEnabled = false
                holder.btnAction2.visibility = View.GONE
            }
            "DELIVERED", "CANCELLED", "DECLINED" -> {
                holder.btnAction1.visibility = View.GONE
                holder.btnAction2.visibility = View.GONE
            }
            else -> {
                holder.btnAction1.visibility = View.GONE
                holder.btnAction2.visibility = View.GONE
            }
        }

        holder.btnAction1.setOnClickListener { onAction1Click(order) }
        holder.btnAction2.setOnClickListener { onAction2Click(order) }
        
        // Item click for viewing details
        holder.itemView.setOnClickListener { onItemClick(order) }
    }

    override fun getItemCount() = orders.size

    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}
