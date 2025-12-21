package com.example.foodnow.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.databinding.FragmentMenuBinding
import com.example.foodnow.ui.ViewModelFactory
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MenuFragment : Fragment(R.layout.fragment_menu) {

    private lateinit var binding: FragmentMenuBinding
    private lateinit var adapter: MenuAdapter
    
    private val viewModel: MenuViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMenuBinding.bind(view)

        val restaurantId = arguments?.getLong("restaurantId") ?: -1L
        if (restaurantId == -1L) {
            Toast.makeText(context, "Invalid Restaurant ID", Toast.LENGTH_SHORT).show()
            return
        }

        adapter = MenuAdapter(emptyList()) { item ->
            // Open Details Bottom Sheet
            val bottomSheet = ItemDetailsBottomSheet(item, restaurantId)
            bottomSheet.show(parentFragmentManager, "ItemDetailsBottomSheet")
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        binding.progressBar.visibility = View.VISIBLE
        viewModel.loadMenu(restaurantId)
        viewModel.loadRestaurantDetails(restaurantId)
        viewModel.loadRestaurantReviews(restaurantId)

        // Setup Tabs
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Order"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Review"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Information"))

        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.recyclerView.visibility = View.VISIBLE
                        binding.rvReviews.visibility = View.GONE
                        binding.layoutInfo.visibility = View.GONE
                    }
                    1 -> {
                        binding.recyclerView.visibility = View.GONE
                        binding.rvReviews.visibility = View.VISIBLE
                        binding.layoutInfo.visibility = View.GONE
                    }
                    2 -> {
                        binding.recyclerView.visibility = View.GONE
                        binding.rvReviews.visibility = View.GONE
                        binding.layoutInfo.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        // Reviews Adapter (Simple inline adapter for now or reuse a generic one if available)
        // I'll create a simple inner class adapter for reviews to be quick and clean
        val reviewsAdapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            var reviews = emptyList<com.example.foodnow.data.RestaurantRatingResponse>()
            
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_restaurant_rating, parent, false)
                return object : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {}
            }

            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                val item = reviews[position]
                val tvName = holder.itemView.findViewById<TextView>(R.id.tvClientName)
                val tvDate = holder.itemView.findViewById<TextView>(R.id.tvDate)
                val ratingBar = holder.itemView.findViewById<RatingBar>(R.id.ratingBar)
                val tvComment = holder.itemView.findViewById<TextView>(R.id.tvComment)
                val tvAvatarLetter = holder.itemView.findViewById<TextView>(R.id.tvAvatarLetter)

                tvName.text = item.clientName
                tvDate.text = item.createdAt.take(10)
                ratingBar.rating = item.rating.toFloat()
                tvComment.text = item.comment
                
                // Set Avatar Letter
                tvAvatarLetter.text = item.clientName.firstOrNull()?.toString()?.uppercase() ?: "?"
            }

            override fun getItemCount() = reviews.size
        }
        binding.rvReviews.layoutManager = LinearLayoutManager(context)
        binding.rvReviews.adapter = reviewsAdapter

        viewModel.menuItems.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            result.onSuccess { list ->
                adapter.updateData(list)
            }.onFailure {
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.restaurantReviews.observe(viewLifecycleOwner) { result ->
            result.onSuccess { list ->
                // Sort by new
                reviewsAdapter.reviews = list
                reviewsAdapter.notifyDataSetChanged()
            }
        }

        viewModel.restaurantDetails.observe(viewLifecycleOwner) { result ->
             result.onSuccess { restaurant ->
                 binding.tvRestaurantName.text = restaurant.name
                 binding.tvRestaurantRating.text = "★ ${restaurant.averageRating ?: 0.0} (${restaurant.ratingCount ?: 0} ratings) • ${restaurant.description ?: ""}"
                 
                 // Info Tab
                 binding.tvInfoDescription.text = restaurant.description ?: "No description available."
                 binding.tvInfoAddress.text = restaurant.address ?: "No address provided."
                 binding.tvInfoHours.text = "Open: ${restaurant.openingHours ?: "Unknown"}" // Updated binding
                 
                 // Phone / Call Button
                 binding.tvInfoPhone.text = restaurant.phone ?: "No phone available"
                 binding.cardContact.setOnClickListener {
                     if (!restaurant.phone.isNullOrEmpty()) {
                         val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
                         intent.data = android.net.Uri.parse("tel:${restaurant.phone}")
                         startActivity(intent)
                     } else {
                         Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
                     }
                 }

                 // Load Image
                 if (!restaurant.imageUrl.isNullOrEmpty()) {
                     val fullUrl = com.example.foodnow.utils.Constants.getFullImageUrl(restaurant.imageUrl)
                     com.bumptech.glide.Glide.with(this)
                         .load(fullUrl)
                         .centerCrop()
                         .into(binding.ivRestaurantImage)
                 }
             }.onFailure {
                 // Log error or show toast
             }
        }

        // Local Cart Bar removed in favor of Global Cart Bar in MainActivity
        /*
        viewLifecycleOwner.lifecycleScope.launch {
            com.example.foodnow.utils.CartManager.cartItems.collect { cartItems ->
                 if (cartItems.isNotEmpty() && com.example.foodnow.utils.CartManager.getCurrentRestaurantId() == restaurantId) {
                    binding.layoutCart.visibility = View.VISIBLE
                    val total = com.example.foodnow.utils.CartManager.getTotal()
                    binding.btnPlaceOrder.text = "View Cart (${cartItems.sumOf { it.quantity }}) - ${String.format("%.2f", total)}DH"
                    binding.btnPlaceOrder.setOnClickListener {
                        val cartSheet = CartBottomSheet(viewModel, restaurantId)
                        cartSheet.show(parentFragmentManager, "CartBottomSheet")
                    }
                } else {
                    binding.layoutCart.visibility = View.GONE
                }
            }
        }
        */

        viewModel.orderResult.observe(viewLifecycleOwner) { result ->
             result.onSuccess {
                 Toast.makeText(context, "Order Placed! ID: ${it.id}", Toast.LENGTH_LONG).show()
                 // Navigate to Orders or Track
                 // findNavController().navigate(R.id.action_menu_to_orders) // If action exists
                 requireActivity().onBackPressedDispatcher.onBackPressed() // Go back for now
             }.onFailure {
                 Toast.makeText(context, "Order Failed: ${it.message}", Toast.LENGTH_LONG).show()
             }
        }
    }
}
