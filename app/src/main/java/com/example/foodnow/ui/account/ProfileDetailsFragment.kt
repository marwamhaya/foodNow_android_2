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
import com.example.foodnow.databinding.FragmentProfileDetailsBinding
import com.example.foodnow.ui.ViewModelFactory

class ProfileDetailsFragment : Fragment(R.layout.fragment_profile_details) {

    private lateinit var binding: FragmentProfileDetailsBinding
    
    private val viewModel: AccountViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProfileDetailsBinding.bind(view)

        // Fetch user profile
        viewModel.fetchProfile()

        // Observe user profile
        viewModel.userProfile.observe(viewLifecycleOwner) { result ->
            result.onSuccess { user ->
                binding.tvProfileName.text = user.fullName
                binding.tvProfileEmail.text = user.email
                // Note: Phone might not be available in the User model
                // binding.tvProfilePhone.text = user.phoneNumber ?: "Not provided"
            }
        }

        // Observe action results
        viewModel.actionResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }
        }

        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Change password button
        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun showChangePasswordDialog() {
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val etCurrent = EditText(context).apply {
            hint = "Current Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val etNew = EditText(context).apply {
            hint = "New Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(etCurrent)
        layout.addView(etNew)

        AlertDialog.Builder(context)
            .setTitle("Change Password")
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->
                val current = etCurrent.text.toString()
                val newPass = etNew.text.toString()
                if (current.isNotEmpty() && newPass.isNotEmpty()) {
                    viewModel.changePassword(current, newPass)
                } else {
                    Toast.makeText(context, "Both fields required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
