package com.example.foodnow.ui.admin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.ui.ViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AdminLivreursFragment : Fragment(R.layout.fragment_admin_livreurs) {

    private val viewModel: AdminViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }
    private lateinit var adapter: AdminLivreurAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvLivreurs)
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAddLivreur)

        rv.layoutManager = LinearLayoutManager(context)
        adapter = AdminLivreurAdapter(emptyList()) { item ->
            viewModel.toggleUserStatus(item.id)
        }
        rv.adapter = adapter

        fab.setOnClickListener {
            findNavController().navigate(R.id.action_admin_livreurs_to_create)
        }

        viewModel.users.observe(viewLifecycleOwner) { result ->
            result.onSuccess { list -> 
                // Filter only Livreurs? Or users with ROLE_LIVREUR?
                // API getAllUsers returns all users. I should filter.
                val livreurs = list.filter { it.role == "ROLE_LIVREUR" } // Assuming Role string
                adapter.updateData(livreurs) 
            }
        }

        viewModel.getAllUsers()
    }
}
