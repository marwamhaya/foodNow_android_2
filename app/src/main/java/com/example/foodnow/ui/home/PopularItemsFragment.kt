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
import com.example.foodnow.databinding.FragmentPopularItemsBinding
import com.example.foodnow.ui.ViewModelFactory
import android.widget.Toast
import com.example.foodnow.ui.menu.ItemDetailsBottomSheet

class PopularItemsFragment : Fragment(R.layout.fragment_popular_items) {

    private lateinit var binding: FragmentPopularItemsBinding
    private lateinit var adapter: PopularGridAdapter
    private var allItems: List<com.example.foodnow.data.MenuItemResponse> = emptyList()
    
    // Reuse HomeViewModel for data
    private val viewModel: HomeViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPopularItemsBinding.bind(view)

        adapter = PopularGridAdapter(emptyList()) { item ->
             // Navigate to the restaurant's menu page
             val restaurantId = item.restaurantId ?: -1L
             if (restaurantId != -1L) {
                 val bundle = bundleOf("restaurantId" to restaurantId)
                 findNavController().navigate(R.id.action_popular_items_to_menu, bundle)
             } else {
                 Toast.makeText(context, "Cannot open restaurant: Missing restaurant info", Toast.LENGTH_SHORT).show()
             }
        }

        binding.recyclerView.layoutManager = GridLayoutManager(context, 2)
        binding.recyclerView.adapter = adapter

        // Filtering Logic
        binding.chipGroupCategories.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == View.NO_ID) {
                // Default to All if nothing selected (though singleSelect=true usually enforces one)
                filterList("All")
                return@setOnCheckedChangeListener
            }
            // Get text from chip
            val chip = group.findViewById<com.google.android.material.chip.Chip>(checkedId)
            val category = chip.text.toString()
            filterList(category)
        }
        
        // Initial Selection
        binding.chipAll.isChecked = true

        viewModel.popularItems.observe(viewLifecycleOwner) { result ->
            result.onSuccess { list ->
                allItems = list
                // Apply current filter
                val checkedId = binding.chipGroupCategories.checkedChipId
                val chip = binding.chipGroupCategories.findViewById<com.google.android.material.chip.Chip>(checkedId)
                val category = chip?.text?.toString() ?: "All"
                filterList(category)
            }
        }
        
        // Trigger load via ViewModel (HomeFragment calls init, but here we might need to if accessed directly or via deep link)
        // HomeViewModel usually loads in init block.
    }
    
    private fun filterList(category: String) {
        val filtered = if (category == "All") {
            allItems
        } else {
            allItems.filter { it.category?.contains(category, ignoreCase = true) == true }
        }
        adapter.updateData(filtered)
    }
}
