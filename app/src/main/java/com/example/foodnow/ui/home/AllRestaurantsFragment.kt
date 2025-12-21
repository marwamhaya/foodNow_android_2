package com.example.foodnow.ui.home

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.databinding.FragmentAllRestaurantsBinding
import com.example.foodnow.ui.ViewModelFactory

class AllRestaurantsFragment : Fragment(R.layout.fragment_all_restaurants) {

    private lateinit var binding: FragmentAllRestaurantsBinding
    private lateinit var adapter: RestaurantAdapter
    private var allRestaurants: List<com.example.foodnow.data.RestaurantResponse> = emptyList()

    private val viewModel: HomeViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAllRestaurantsBinding.bind(view)

        adapter = RestaurantAdapter(emptyList()) { restaurantId ->
            val bundle = bundleOf("restaurantId" to restaurantId)
            findNavController().navigate(R.id.action_all_to_menu, bundle)
        }

        binding.recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
        
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        viewModel.restaurants.observe(viewLifecycleOwner) { result ->
            result.onSuccess { list ->
                allRestaurants = list
                filterList(binding.etSearch.text.toString())
            }.onFailure {
                // Handle error
            }
        }
        
        // If strict separation is needed, we can call load here too if exposed.
        // However, standard flow is fine.
    }
    private fun filterList(query: String) {
        val filtered = if (query.isEmpty()) {
            allRestaurants
        } else {
            allRestaurants.filter { it.name.contains(query, ignoreCase = true) }
        }
        adapter.updateData(filtered)
    }
}
