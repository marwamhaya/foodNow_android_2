package com.example.foodnow.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodnow.R
import com.example.foodnow.data.Order
import com.example.foodnow.data.OrderItem
import com.example.foodnow.databinding.BottomSheetOrderDetailsBinding
import com.example.foodnow.utils.Constants
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.navigation.fragment.findNavController
import com.example.foodnow.ui.orders.TrackOrderFragment
import java.math.BigDecimal

class OrderDetailsBottomSheet(private val order: Order) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetOrderDetailsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetOrderDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Close button
        binding.btnClose.setOnClickListener { dismiss() }

        binding.tvOrderId.visibility = View.GONE
        binding.tvOrderId.text = "Order #${order.id}"
        binding.tvRestaurantName.text = order.restaurantName
        binding.tvRestaurantCategory.text = "Restaurant" // Placeholder category
        binding.tvOrderDate.text = "Date: ${order.createdAt.take(10)}"
        binding.tvOrderStatus.text = order.status.replace("_", " ")
        binding.tvTotal.text = "${String.format("%.2f", order.totalAmount)} DH"

        // Load restaurant image
        if (!order.restaurantImageUrl.isNullOrEmpty()) {
            val fullUrl = Constants.getFullImageUrl(order.restaurantImageUrl)
            Glide.with(this)
                .load(fullUrl)
                .centerCrop()
                .placeholder(R.drawable.placeholder_food)
                .into(binding.ivRestaurantImage)
        }

        // Set status color
        val colorRes = when (order.status) {
            "DELIVERED" -> R.color.success
            "CANCELLED", "DECLINED" -> R.color.error
            else -> R.color.primary // Active/Pending
        }
        binding.tvOrderStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
            androidx.core.content.ContextCompat.getColor(requireContext(), colorRes)
        )

        // Setup RecyclerView
        val adapter = OrderItemsAdapter(order.items)
        binding.rvOrderItems.layoutManager = LinearLayoutManager(context)
        binding.rvOrderItems.adapter = adapter

        // Handle Track Button visibility
        if (order.status == "IN_DELIVERY" || order.status == "ON_THE_WAY" || order.status == "PICKED_UP") {
            binding.btnTrackOrder.visibility = View.VISIBLE
            binding.btnTrackOrder.setOnClickListener {
                dismiss()
                val bundle = Bundle().apply { putLong("orderId", order.id) }
                try {
                    findNavController().navigate(R.id.nav_track_order, bundle)
                } catch (e: Exception) {
                    // Fallback
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, TrackOrderFragment().apply { arguments = bundle })
                        .addToBackStack(null)
                        .commit()
                }
            }
        } else {
             binding.btnTrackOrder.visibility = View.GONE
        }

        // Handle Rate Button visibility
        if (order.status == "DELIVERED") {
            binding.btnRateOrder.visibility = View.VISIBLE
            binding.btnRateOrder.setOnClickListener {
                val ratingBottomSheet = RatingBottomSheetFragment.newInstance(
                    order.id, 
                    order.restaurantName,
                    order.restaurantAddress,
                    order.restaurantImageUrl
                )
                ratingBottomSheet.show(parentFragmentManager, "RatingBottomSheet")
            }
        } else {
            binding.btnRateOrder.visibility = View.GONE
        }
    }
}

class OrderItemsAdapter(private val items: List<OrderItem>) : RecyclerView.Adapter<OrderItemsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivItemImage: ImageView = view.findViewById(R.id.ivItemImage)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        val tvItemDetails: TextView = view.findViewById(R.id.tvItemDetails)
        val tvItemPrice: TextView = view.findViewById(R.id.tvItemPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvQuantity.text = "${item.quantity}x"
        holder.tvItemName.text = item.menuItemName
        
        // Load item image
        if (!item.menuItemImageUrl.isNullOrEmpty()) {
            val fullUrl = Constants.getFullImageUrl(item.menuItemImageUrl)
            Glide.with(holder.itemView.context)
                .load(fullUrl)
                .centerCrop()
                .placeholder(R.drawable.placeholder_food)
                .into(holder.ivItemImage)
        } else {
            holder.ivItemImage.setImageResource(R.drawable.placeholder_food)
        }
        
        // Show supplements with prices if any
        if (!item.selectedOptions.isNullOrEmpty()) {
            holder.tvItemDetails.visibility = View.VISIBLE
            val detailsText = item.selectedOptions.joinToString(", ") { 
                "${it.name} (+${String.format("%.2f", it.price)} DH)" 
            }
            holder.tvItemDetails.text = "• $detailsText"
        } else {
            holder.tvItemDetails.visibility = View.GONE
        }
        
        // Calculate total price for item
        val totalPrice = item.price.multiply(java.math.BigDecimal(item.quantity))
        holder.tvItemPrice.text = "${String.format("%.2f", totalPrice)} DH"
    }

    override fun getItemCount() = items.size
}
