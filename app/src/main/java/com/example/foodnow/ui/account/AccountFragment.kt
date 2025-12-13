package com.example.foodnow.ui.account

import android.app.AlertDialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
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
                if (msg == "Account deleted") {
                     findNavController().navigate(R.id.loginFragment)
                }
            }.onFailure {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.btnDarkMode.setOnClickListener {
             val currentMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
             if (currentMode == Configuration.UI_MODE_NIGHT_YES) {
                 AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
             } else {
                 AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
             }
        }
        
        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            findNavController().navigate(R.id.loginFragment)
        }
        
        binding.tvDeleteAccount.setOnClickListener {
             AlertDialog.Builder(context)
                .setTitle("Delete Account")
                .setMessage("Are you sure? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ -> viewModel.deleteAccount() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showChangePasswordDialog() {
        val input = EditText(context)
        AlertDialog.Builder(context)
            .setTitle("Change Password")
            .setView(input)
            .setPositiveButton("Update") { _, _ ->
                val newPass = input.text.toString()
                if (newPass.isNotEmpty()) {
                    viewModel.changePassword(newPass)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
