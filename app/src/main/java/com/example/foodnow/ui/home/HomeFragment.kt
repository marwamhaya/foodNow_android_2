package com.example.foodnow.ui.home

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.databinding.FragmentHomeBinding
import com.example.foodnow.ui.ViewModelFactory
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.foodnow.ui.menu.CartBottomSheet
import com.example.foodnow.ui.menu.MenuViewModel
import com.example.foodnow.utils.CartManager

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: HorizontalRestaurantAdapter
    private lateinit var popularItemsAdapter: HorizontalPopularItemsAdapter
    
    private val viewModel: HomeViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    private val menuViewModel: MenuViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentHomeBinding.bind(view)

        // Restaurants Adapter
        adapter = HorizontalRestaurantAdapter(
            emptyList(),
            onRestaurantClick = { restaurantId ->
                val bundle = bundleOf("restaurantId" to restaurantId)
                findNavController().navigate(R.id.action_home_to_menu, bundle)
            },
            onSeeAllClick = {
                findNavController().navigate(R.id.action_home_to_all_restaurants)
            }
        )

        binding.recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerView.adapter = adapter

        // Popular Items Adapter
        popularItemsAdapter = HorizontalPopularItemsAdapter(
            emptyList(),
            onItemClick = { item ->
                // Navigate to the restaurant's menu page
                val restaurantId = item.restaurantId
                if (restaurantId != null && restaurantId > 0) {
                    val bundle = bundleOf("restaurantId" to restaurantId)
                    findNavController().navigate(R.id.action_home_to_menu, bundle)
                }
            },
            onSeeAllClick = {
                // Navigate to Top 20 Page (PopularItemsFragment)
                findNavController().navigate(R.id.action_home_to_popular_items)
            }
        )
        binding.rvPopularItems.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
        binding.rvPopularItems.adapter = popularItemsAdapter

        binding.btnSeeAll.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_all_restaurants)
        }
        
        binding.btnSeeAllPopular.setOnClickListener {
             // Navigate to Top 20 Page
             findNavController().navigate(R.id.action_home_to_popular_items)
        }

        // Setup Promo Carousel with ViewPager2
        val promoItems = listOf(
            PromoItem("Get your 30% daily discount now!", R.color.black, R.drawable.img_promo_burger),
            PromoItem("The fastest delivery you will ever get", R.color.black, R.drawable.img_promo_pizza)
        )
        binding.vpPromoCarousel.adapter = PromoCarouselAdapter(promoItems) {
            // Navigate to Order Page
            findNavController().navigate(R.id.action_home_to_all_restaurants)
        }
        
        // Connect Indicators
        com.google.android.material.tabs.TabLayoutMediator(binding.tlPromoIndicator, binding.vpPromoCarousel) { _, _ ->
            // No text for tabs, just dots
        }.attach()

        binding.progressBar.visibility = View.VISIBLE
        
        // Observers
        viewModel.restaurants.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            result.onSuccess { list ->
                if (list.isEmpty()) {
                    binding.tvError.text = "No restaurants found"
                    binding.tvError.visibility = View.VISIBLE
                } else {
                    binding.tvError.visibility = View.GONE
                    adapter.updateData(list)
                }
            }.onFailure {
                binding.tvError.text = "Error: ${it.message}"
                binding.tvError.visibility = View.VISIBLE
            }
        }

        viewModel.popularItems.observe(viewLifecycleOwner) { result ->
            result.onSuccess { list ->
                popularItemsAdapter.updateData(list)
            }
        }


        // Observe CartManager to show global cart button
        viewLifecycleOwner.lifecycleScope.launch {
            CartManager.cartItems.collect { cartItems ->
                val restaurantId = CartManager.getCurrentRestaurantId()
                // In Home Fragment we might not show cart button if we are just browsing promo
                // but kept it as per previous code if relevant. The Layout doesn't have it anymore in XML though?
                // Wait, I checked fragment_home.xml, the cart layout might have been removed or moved?
                // Checking XML: I KEPT the layoutCart in the FrameLayout in my previous XML update?
                // Wait, I replaced LinearLayout content. Let me double check usage.
                // If I removed it from XML, I should remove it here.
                // In the XML update I did:
                // <LinearLayout ... android:orientation="vertical" android:paddingBottom="80dp"> ... </LinearLayout>
                // And I REMOVED the "Cart Layout" that was at the bottom of the FrameLayout in previous file.
                // So I should remove this code to avoid crash.
            }
        }
    }
}
