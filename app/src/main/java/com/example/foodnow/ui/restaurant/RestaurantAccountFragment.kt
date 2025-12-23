package com.example.foodnow.ui.restaurant

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.foodnow.FoodNowApp
import com.example.foodnow.MainActivity
import com.example.foodnow.R
import com.example.foodnow.databinding.FragmentAccountBinding
import com.example.foodnow.ui.ViewModelFactory
import com.example.foodnow.ui.account.ContactUsBottomSheet

class RestaurantAccountFragment : Fragment(R.layout.fragment_account) {

    private lateinit var binding: FragmentAccountBinding
    
    private val viewModel: RestaurantViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAccountBinding.bind(view)

        // Load restaurant data
        viewModel.restaurant.observe(viewLifecycleOwner) { result ->
            result.onSuccess { restaurant ->
                // Display restaurant name and phone
                binding.tvUserName.text = restaurant.name
                binding.tvUserEmail.text = restaurant.phone ?: restaurant.address ?: "Restaurant"
            }
        }

        // Fetch restaurant info
        viewModel.getMyRestaurant()

        // My Profile button navigates to restaurant owner's personal profile
        binding.btnMyProfile.setOnClickListener {
            findNavController().navigate(R.id.action_restaurantAccountFragment_to_ownerProfile)
        }

        // Contact Us button
        binding.btnContactUs.setOnClickListener {
            val contactBottomSheet = ContactUsBottomSheet()
            contactBottomSheet.show(parentFragmentManager, "ContactUsBottomSheet")
        }
        
        // Logout button
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    logout()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun logout() {
        val app = requireActivity().application as FoodNowApp
        app.repository.logout()
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}
