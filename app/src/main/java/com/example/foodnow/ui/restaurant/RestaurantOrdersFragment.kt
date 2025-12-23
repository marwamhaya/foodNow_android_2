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

import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

class RestaurantOrdersFragment : Fragment(R.layout.fragment_restaurant_orders) {

    private val viewModel: RestaurantViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout = view.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayoutOrders)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerOrders)

        val tabTitles = listOf("All", "Pending", "In Progress", "Ready", "Delivered", "Cancelled")
        
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = tabTitles.size
            override fun createFragment(position: Int): Fragment = OrderListFragment.newInstance(position)
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        viewModel.getOrders()
    }
}
