package com.example.foodnow.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.R
import com.example.foodnow.data.Order
import java.text.SimpleDateFormat
import java.util.Locale

import android.widget.ImageView
import android.widget.Button
import com.bumptech.glide.Glide

class OrdersAdapter(
    private var orders: List<Order>, 
    private val onClick: (Order) -> Unit,
    private val onRateClick: (Order) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRestaurantName: TextView = view.findViewById(R.id.tvRestaurantName)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvOrderDate: TextView = view.findViewById(R.id.tvOrderDate)
        val tvOrderItems: TextView = view.findViewById(R.id.tvOrderItems)
        val ivRestaurantImage: ImageView = view.findViewById(R.id.ivRestaurantImage)
        val btnRate: Button = view.findViewById(R.id.btnRate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.tvRestaurantName.text = order.restaurantName ?: "FoodNow Order"
        holder.tvStatus.text = order.status
        
        // Simple date formatting (assuming ISO string from backend)
        holder.tvOrderDate.text = order.createdAt.take(10) 

        val itemCount = order.items.sumOf { it.quantity }
        holder.tvOrderItems.text = "$itemCount items • ${String.format("%.2f", order.totalAmount)} DH"

        // Load Image
        if (!order.restaurantImageUrl.isNullOrEmpty()) {
            val fullUrl = com.example.foodnow.utils.Constants.getFullImageUrl(order.restaurantImageUrl)
            Glide.with(holder.itemView.context)
                .load(fullUrl)
                .placeholder(R.drawable.bg_placeholder)
                .into(holder.ivRestaurantImage)
        } else {
            holder.ivRestaurantImage.setImageResource(R.drawable.bg_placeholder)
        }

        // Show Rate Button if Delivered
        if (order.status == "DELIVERED") {
            holder.btnRate.visibility = View.VISIBLE
            holder.btnRate.setOnClickListener { onRateClick(order) }
        } else {
            holder.btnRate.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(order) }
    }

    override fun getItemCount() = orders.size

    fun updateData(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}
