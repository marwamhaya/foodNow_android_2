package com.example.foodnow.ui.account

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.databinding.FragmentAccountBinding
import com.example.foodnow.ui.ViewModelFactory

class AccountFragment : Fragment(R.layout.fragment_account) {

    private lateinit var binding: FragmentAccountBinding
    
    private val viewModel: AccountViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAccountBinding.bind(view)

        viewModel.fetchProfile()

        viewModel.userProfile.observe(viewLifecycleOwner) { result ->
            result.onSuccess { user ->
                binding.tvUserName.text = user.fullName
                binding.tvUserEmail.text = user.email
            }
        }
        
        viewModel.actionResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }
        }

        // My Profile button
        binding.btnMyProfile.setOnClickListener {
            findNavController().navigate(R.id.action_account_to_profile_details)
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
                    viewModel.logout()
                    findNavController().navigate(R.id.loginFragment)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
