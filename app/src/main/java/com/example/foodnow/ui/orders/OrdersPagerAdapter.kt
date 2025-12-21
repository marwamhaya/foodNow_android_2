package com.example.foodnow.ui.orders

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class OrdersPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        val filterType = if (position == 0) "ACTIVE" else "PAST"
        return OrderListFragment.newInstance(filterType)
    }
}
