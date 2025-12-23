package com.example.foodnow.ui.restaurant

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.ui.ViewModelFactory

class RestaurantHomeInfoTabFragment : Fragment(R.layout.fragment_restaurant_home_info_tab) {

    private val viewModel: RestaurantViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    private lateinit var tvInfoDescription: TextView
    private lateinit var tvInfoAddress: TextView
    private lateinit var tvInfoHours: TextView
    private lateinit var tvInfoPhone: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvInfoDescription = view.findViewById(R.id.tvInfoDescription)
        tvInfoAddress = view.findViewById(R.id.tvInfoAddress)
        tvInfoHours = view.findViewById(R.id.tvInfoHours)
        tvInfoPhone = view.findViewById(R.id.tvInfoPhone)

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.restaurant.observe(viewLifecycleOwner) { result ->
            result.onSuccess { restaurant ->
                tvInfoDescription.text = restaurant.description ?: "Add your restaurant description..."
                tvInfoAddress.text = restaurant.address ?: "Address not set"
                tvInfoHours.text = restaurant.openingHours ?: "Hours not set"
                tvInfoPhone.text = restaurant.phone ?: "Phone not set"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Notify parent to show FAB with edit icon
        (parentFragment as? RestaurantHomeMenuTabFragment.FabStateListener)?.onFabStateChanged(
            visible = true,
            iconRes = android.R.drawable.ic_menu_edit
        ) {
            // Navigate to edit restaurant info
            findNavController().navigate(R.id.action_restaurantHomeFragment_to_editRestaurantInfoFragment)
        }
    }
}
