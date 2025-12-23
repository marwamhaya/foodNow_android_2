package com.example.foodnow.ui.restaurant

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.ui.ViewModelFactory
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.data.Order
import java.util.Locale

class RestaurantDashboardFragment : Fragment(R.layout.fragment_restaurant_dashboard) {

    private val viewModel: RestaurantViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Header View
        val tvRestaurantNameHeader = view.findViewById<TextView>(R.id.tvRestaurantNameHeader)
        
        // Stats views
        val tvTotalOrders = view.findViewById<TextView>(R.id.tvTotalOrders)
        val tvTotalRevenue = view.findViewById<TextView>(R.id.tvTotalRevenue)
        val tvAverageRating = view.findViewById<TextView>(R.id.tvAverageRating)
        val tvTotalClients = view.findViewById<TextView>(R.id.tvTotalClients)
        
        // RecyclerView for Recent Activity
        val rvRecentOrders = view.findViewById<RecyclerView>(R.id.rvRecentOrders)
        val tvEmptyActivity = view.findViewById<TextView>(R.id.tvEmptyActivity)
        val tvSeeAllOrders = view.findViewById<TextView>(R.id.tvSeeAllOrders)

        // Graph Bars and Labels
        val graphItems = listOf(
            view.findViewById<View>(R.id.bar1) to view.findViewById<TextView>(R.id.tvBarValue1),
            view.findViewById<View>(R.id.bar2) to view.findViewById<TextView>(R.id.tvBarValue2),
            view.findViewById<View>(R.id.bar3) to view.findViewById<TextView>(R.id.tvBarValue3),
            view.findViewById<View>(R.id.bar4) to view.findViewById<TextView>(R.id.tvBarValue4),
            view.findViewById<View>(R.id.bar5) to view.findViewById<TextView>(R.id.tvBarValue5),
            view.findViewById<View>(R.id.bar6) to view.findViewById<TextView>(R.id.tvBarValue6),
            view.findViewById<View>(R.id.bar7) to view.findViewById<TextView>(R.id.tvBarValue7)
        )

        viewModel.restaurant.observe(viewLifecycleOwner) { result ->
            result.onSuccess { rest ->
                tvRestaurantNameHeader.text = rest.name
            }
        }

        viewModel.stats.observe(viewLifecycleOwner) { result ->
            result.onSuccess { stats ->
                tvTotalOrders.text = stats.totalOrders.toString()
                tvTotalRevenue.text = String.format(Locale.getDefault(), "%.2f", stats.totalRevenue)
                tvAverageRating.text = String.format(Locale.getDefault(), "%.1f", stats.averageRating)
                tvTotalClients.text = stats.totalClients.toString()
            }.onFailure {
                Toast.makeText(context, "Stats Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup Recent Orders List
        rvRecentOrders.layoutManager = LinearLayoutManager(context)
        val recentAdapter = RecentOrdersAdapter { order ->
             // Navigate to specific order details
             val bundle = Bundle().apply { putLong("orderId", order.id) }
             findNavController().navigate(R.id.orderDetailsFragment, bundle)
        }
        rvRecentOrders.adapter = recentAdapter

        viewModel.orders.observe(viewLifecycleOwner) { result ->
            result.onSuccess { orderList ->
                // Sort by date descending and take top 5
                val recent = orderList.sortedByDescending { it.id }.take(5)
                if (recent.isNotEmpty()) {
                    recentAdapter.submitList(recent)
                    tvEmptyActivity.visibility = View.GONE
                    rvRecentOrders.visibility = View.VISIBLE
                } else {
                    tvEmptyActivity.visibility = View.VISIBLE
                    rvRecentOrders.visibility = View.GONE
                }
                
                // Animate graph bars based on recent sales (simulation)
                updateGraphBars(graphItems, orderList)
            }
        }

        tvSeeAllOrders.setOnClickListener {
            findNavController().navigate(R.id.restaurantOrdersFragment)
        }

        // Navigation for stats (as before)
        view.findViewById<View>(R.id.cardHeroRevenue)?.setOnClickListener { navigateToOrdersWithFilter(4) }
        view.findViewById<View>(R.id.tvTotalOrders)?.parent?.let { parent ->
             (parent as? View)?.setOnClickListener { navigateToOrdersWithFilter(0) }
        }

        // Initial Load
        viewModel.fetchStats()
        viewModel.getOrders()
        viewModel.getMyRestaurant()
    }

    private fun updateGraphBars(items: List<Pair<View, TextView>>, orders: List<Order>) {
        val now = java.time.LocalDate.now()
        val density = resources.displayMetrics.density
        val maxHeight = 140 * density
        
        // Group revenue by date for the last 7 days
        val revenueByDate = orders
            .filter { it.status != "CANCELLED" && it.status != "DECLINED" }
            .groupBy { 
                try {
                    java.time.OffsetDateTime.parse(it.createdAt).toLocalDate()
                } catch (e: Exception) {
                    // Fallback for different date formats
                    java.time.LocalDate.now()
                }
            }
            .mapValues { entry -> 
                entry.value.sumOf { it.totalAmount.toDouble() }
            }

        // Generate data for the last 7 days (index 0 is 6 days ago, index 6 is today)
        val dailyRevenue = (0..6).map { daysAgo ->
            val date = now.minusDays((6 - daysAgo).toLong())
            revenueByDate[date] ?: 0.0
        }

        val maxRevenue = dailyRevenue.maxOrNull()?.coerceAtLeast(100.0) ?: 100.0

        items.forEachIndexed { index, item ->
            val bar = item.first
            val label = item.second
            val revenue = dailyRevenue[index]
            
            // Calculate height relative to max revenue (minimum 10% height for visibility)
            val heightPercent = (revenue / maxRevenue).coerceAtLeast(0.1)
            val height = (maxHeight * heightPercent).toInt()
            
            // Update UI
            val params = bar.layoutParams
            params.height = height
            bar.layoutParams = params
            
            label.text = String.format(Locale.getDefault(), "%.0f", revenue)
        }
    }
    
    private fun navigateToOrdersWithFilter(filterTabIndex: Int) {
        val bundle = Bundle().apply {
            putInt("filterTab", filterTabIndex)
        }
        findNavController().navigate(R.id.restaurantOrdersFragment, bundle)
    }

    // --- Compact Adapter for Dashboard ---
    private class RecentOrdersAdapter(private val onClick: (Order) -> Unit) : 
        RecyclerView.Adapter<RecentOrdersAdapter.ViewHolder>() {
        
        private var items: List<Order> = emptyList()

        fun submitList(newList: List<Order>) {
            items = newList
            notifyDataSetChanged()
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvClientName)
            val tvTime: TextView = view.findViewById(R.id.tvOrderTime)
            val tvAmount: TextView = view.findViewById(R.id.tvOrderAmount)
            val indicator: View = view.findViewById(R.id.viewStatusIndicator)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dashboard_recent_order, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.clientName ?: "Guest"
            holder.tvAmount.text = String.format("%.2f DH", item.totalAmount)
            holder.tvTime.text = item.status.replace("_", " ")
            
            // Set indicator color based on status
            val color = when (item.status) {
                "PENDING" -> 0xFFFF9800.toInt()
                "DELIVERED" -> 0xFF4CAF50.toInt()
                "CANCELLED", "DECLINED" -> 0xFFF44336.toInt()
                else -> 0xFF2196F3.toInt()
            }
            holder.indicator.setBackgroundColor(color)
            
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size
    }
}
