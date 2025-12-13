package com.example.foodnow.ui.login

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.databinding.FragmentLoginBinding
import com.example.foodnow.ui.ViewModelFactory

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var binding: FragmentLoginBinding
    
    private val viewModel: LoginViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLoginBinding.bind(view)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            
            if (email.isNotBlank() && password.isNotBlank()) {
                binding.progressBar.visibility = View.VISIBLE
                binding.btnLogin.isEnabled = false
                viewModel.login(email, password)
            } else {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            binding.btnLogin.isEnabled = true
            
            result.onSuccess {
                val repository = (requireActivity().application as FoodNowApp).repository
                val role = repository.getUserRole()
                when (role) {
                    "RESTAURANT" -> {
                        startActivity(android.content.Intent(requireContext(), com.example.foodnow.RestaurantActivity::class.java))
                        requireActivity().finish()
                    }
                    "LIVREUR" -> {
                        startActivity(android.content.Intent(requireContext(), com.example.foodnow.LivreurActivity::class.java))
                        requireActivity().finish()
                    }
                    "ADMIN" -> {
                        startActivity(android.content.Intent(requireContext(), com.example.foodnow.AdminActivity::class.java))
                        requireActivity().finish()
                    }
                    else -> {
                        findNavController().navigate(R.id.action_login_to_home)
                    }
                }
            }.onFailure {
                Toast.makeText(context, "Login failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
