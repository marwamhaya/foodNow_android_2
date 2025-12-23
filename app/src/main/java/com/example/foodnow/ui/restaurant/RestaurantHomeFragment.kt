package com.example.foodnow.ui.restaurant

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.ui.ViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class RestaurantHomeFragment : Fragment(R.layout.fragment_restaurant_home),
    RestaurantHomeMenuTabFragment.FabStateListener {

    private val viewModel: RestaurantViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var fabAction: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    
    // Header views
    private lateinit var ivRestaurantImage: ImageView
    private lateinit var tvRestaurantName: TextView
    private lateinit var tvRestaurantRating: TextView

    private var fabClickAction: (() -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupViewPager()
        observeViewModel()
        loadRestaurantData()
    }

    private fun initializeViews(view: View) {
        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)
        fabAction = view.findViewById(R.id.fabAction)
        progressBar = view.findViewById(R.id.progressBar)
        
        ivRestaurantImage = view.findViewById(R.id.ivRestaurantImage)
        tvRestaurantName = view.findViewById(R.id.tvRestaurantName)
        tvRestaurantRating = view.findViewById(R.id.tvRestaurantRating)

        fabAction.setOnClickListener {
            fabClickAction?.invoke()
        }
    }

    private fun setupViewPager() {
        val adapter = RestaurantHomePagerAdapter(this)
        viewPager.adapter = adapter

        // Link TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "MENU"
                1 -> "REVIEW"
                2 -> "INFORMATION"
                else -> ""
            }
        }.attach()

        // Handle page changes to update FAB
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // FAB will be updated via onFabStateChanged callback from child fragments
            }
        })
    }

    private fun observeViewModel() {
        viewModel.restaurant.observe(viewLifecycleOwner) { result ->
            result.onSuccess { restaurant ->
                tvRestaurantName.text = restaurant.name
                tvRestaurantRating.text = "★ ${restaurant.averageRating ?: 0.0} (${restaurant.ratingCount ?: 0} reviews)"
                
                // Load restaurant image with error logging
                restaurant.imageUrl?.let { url ->
                    // Construct full URL if needed
                    val fullUrl = if (url.startsWith("http")) {
                        url
                    } else {
                        com.example.foodnow.utils.Constants.getFullImageUrl(url)
                    }
                    
                    android.util.Log.d("RestaurantHome", "Loading image from: $fullUrl")
                    
                    Glide.with(this)
                        .load(fullUrl)
                        .placeholder(R.drawable.bg_bottom_sheet)
                        .error(R.drawable.bg_bottom_sheet)
                        .addListener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                            override fun onLoadFailed(
                                e: com.bumptech.glide.load.engine.GlideException?,
                                model: Any?,
                                target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                android.util.Log.e("RestaurantHome", "Failed to load restaurant image: $fullUrl", e)
                                return false
                            }

                            override fun onResourceReady(
                                resource: android.graphics.drawable.Drawable,
                                model: Any,
                                target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                                dataSource: com.bumptech.glide.load.DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                android.util.Log.d("RestaurantHome", "Successfully loaded restaurant image")
                                return false
                            }
                        })
                        .into(ivRestaurantImage)
                } ?: run {
                    android.util.Log.w("RestaurantHome", "Restaurant imageUrl is null")
                    ivRestaurantImage.setImageResource(R.drawable.bg_bottom_sheet)
                }
            }
        }
    }

    private fun loadRestaurantData() {
        viewModel.getMyRestaurant()
        viewModel.getMenuItems()
    }

    override fun onFabStateChanged(visible: Boolean, iconRes: Int, action: () -> Unit) {
        if (visible) {
            fabAction.visibility = View.VISIBLE
            fabAction.setImageResource(iconRes)
            fabClickAction = action
        } else {
            fabAction.visibility = View.GONE
            fabClickAction = null
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload data in case it was updated
        loadRestaurantData()
    }

    /**
     * Adapter for ViewPager2 that manages the three tabs
     */
    private inner class RestaurantHomePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> RestaurantHomeMenuTabFragment()
                1 -> RestaurantHomeReviewsTabFragment()
                2 -> RestaurantHomeInfoTabFragment()
                else -> throw IllegalStateException("Invalid position: $position")
            }
        }
    }
}
