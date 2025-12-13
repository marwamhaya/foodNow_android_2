package com.example.foodnow.ui.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R

class SplashFragment : Fragment(R.layout.fragment_splash) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Handler(Looper.getMainLooper()).postDelayed({
            val repository = (requireActivity().application as FoodNowApp).repository
            if (repository.isLoggedIn()) {
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
                    else -> { // CLIENT or others
                         findNavController().navigate(R.id.action_splash_to_home)
                    }
                }
            } else {
                findNavController().navigate(R.id.action_splash_to_login)
            }
        }, 1500) // 1.5 second delay for splash effect
    }
}
