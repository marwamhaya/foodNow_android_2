package com.example.foodnow.ui.restaurant

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodnow.R
import com.example.foodnow.databinding.FragmentRestaurantRatingsBinding

class RestaurantRatingsFragment : Fragment(R.layout.fragment_restaurant_ratings) {

    private val viewModel: RestaurantViewModel by activityViewModels() // Shared ViewModel
    private lateinit var binding: FragmentRestaurantRatingsBinding
    private lateinit var adapter: RestaurantRatingsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRestaurantRatingsBinding.bind(view)

        adapter = RestaurantRatingsAdapter()
        binding.rvRatings.layoutManager = LinearLayoutManager(context)
        binding.rvRatings.adapter = adapter

        viewModel.ratings.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            result.onSuccess { ratings ->
                if (ratings.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.rvRatings.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.rvRatings.visibility = View.VISIBLE
                    adapter.updateData(ratings)
                }
            }.onFailure {
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                binding.tvEmpty.text = "Error loading ratings"
                binding.tvEmpty.visibility = View.VISIBLE
            }
        }

        binding.progressBar.visibility = View.VISIBLE
        viewModel.fetchRatings()
    }
}
