package com.example.foodnow.ui.restaurant

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.ui.ViewModelFactory

class RestaurantHomeReviewsTabFragment : Fragment(R.layout.fragment_restaurant_home_reviews_tab) {

    private val viewModel: RestaurantViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    private lateinit var rvReviews: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var reviewsAdapter: RestaurantRatingsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvReviews = view.findViewById(R.id.rvReviews)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)

        setupRecyclerView()
        observeViewModel()
        loadReviews()
    }

    private fun setupRecyclerView() {
        reviewsAdapter = RestaurantRatingsAdapter()
        rvReviews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewsAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.ratings.observe(viewLifecycleOwner) { result ->
            result.onSuccess { ratings ->
                if (ratings.isEmpty()) {
                    rvReviews.visibility = View.GONE
                    tvEmptyState.visibility = View.VISIBLE
                } else {
                    rvReviews.visibility = View.VISIBLE
                    tvEmptyState.visibility = View.GONE
                    reviewsAdapter.updateData(ratings)
                }
            }
        }
    }

    private fun loadReviews() {
        viewModel.fetchRatings()
    }

    override fun onResume() {
        super.onResume()
        // Notify parent to hide FAB for reviews tab
        (parentFragment as? RestaurantHomeMenuTabFragment.FabStateListener)?.onFabStateChanged(
            visible = false,
            iconRes = 0
        ) {}
    }
}
