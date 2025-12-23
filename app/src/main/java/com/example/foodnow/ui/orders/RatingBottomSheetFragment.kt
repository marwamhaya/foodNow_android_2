package com.example.foodnow.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.databinding.BottomSheetRatingBinding
import com.example.foodnow.ui.ViewModelFactory
import com.example.foodnow.utils.Constants
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RatingBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetRatingBinding
    private val viewModel: OrdersViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetRatingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val orderId = arguments?.getLong("orderId") ?: 0L
        val restaurantName = arguments?.getString("restaurantName") ?: ""
        val restaurantAddress = arguments?.getString("restaurantAddress") ?: "Restaurant"
        val restaurantImageUrl = arguments?.getString("restaurantImageUrl")

        // Setup header
        binding.btnBack.setOnClickListener { dismiss() }

        // Setup restaurant info
        binding.tvRestaurantTitle.text = restaurantName
        binding.tvRestaurantAddress.text = restaurantAddress

        // Load restaurant image
        if (!restaurantImageUrl.isNullOrEmpty()) {
            val fullUrl = Constants.getFullImageUrl(restaurantImageUrl)
            Glide.with(this)
                .load(fullUrl)
                .centerCrop()
                .placeholder(R.drawable.placeholder_food)
                .into(binding.ivRestaurantImage)
        }


        binding.btnSubmitRating.setOnClickListener {
            val rating = binding.ratingBar.rating.toInt()
            val comment = binding.etComment.text.toString()

            if (rating == 0) {
                Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnSubmitRating.isEnabled = false
            binding.btnSubmitRating.text = "Submitting..."
            
            viewModel.submitRating(orderId, rating, comment)
        }

        viewModel.ratingStatus.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, "Rating submitted!", Toast.LENGTH_SHORT).show()
                dismiss()
            }.onFailure {
                binding.btnSubmitRating.isEnabled = true
                binding.btnSubmitRating.text = "Next"
                Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    companion object {
        fun newInstance(orderId: Long, restaurantName: String, restaurantAddress: String? = null, restaurantImageUrl: String? = null): RatingBottomSheetFragment {
            return RatingBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putLong("orderId", orderId)
                    putString("restaurantName", restaurantName)
                    putString("restaurantAddress", restaurantAddress ?: "")
                    putString("restaurantImageUrl", restaurantImageUrl)
                }
            }
        }
    }
}
