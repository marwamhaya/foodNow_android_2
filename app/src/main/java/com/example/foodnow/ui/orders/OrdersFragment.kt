package com.example.foodnow.ui.orders

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.foodnow.R
import com.example.foodnow.databinding.FragmentOrdersBinding

class OrdersFragment : Fragment(R.layout.fragment_orders) {

    private lateinit var binding: FragmentOrdersBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentOrdersBinding.bind(view)

        val adapter = OrdersPagerAdapter(this)
        binding.viewPager.adapter = adapter

        com.google.android.material.tabs.TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "Active Orders" else "Past Orders"
        }.attach()
    }
}
