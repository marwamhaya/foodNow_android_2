package com.example.foodnow.ui.livreur

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.ui.ViewModelFactory

class LivreurProfileFragment : Fragment(R.layout.fragment_livreur_profile) {

    private val viewModel: LivreurViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val tvName = view.findViewById<TextView>(R.id.tvLivreurName)
        val tvStatus = view.findViewById<TextView>(R.id.tvLivreurStatus)
        val btnToggle = view.findViewById<Button>(R.id.btnToggleAvailability)
        
        btnToggle.setOnClickListener { viewModel.toggleAvailability() }
        
        viewModel.profile.observe(viewLifecycleOwner) { result ->
            result.onSuccess { profile ->
                tvName.text = profile.fullName
                tvStatus.text = if (profile.isAvailable) "Status: AVAILABLE" else "Status: OFFLINE"
                btnToggle.text = if (profile.isAvailable) "Go Offline" else "Go Online"
            }
        }
        viewModel.getProfile()
    }
}
