package com.example.foodnow.ui.menu

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.databinding.FragmentMenuBinding
import com.example.foodnow.ui.ViewModelFactory

class MenuFragment : Fragment(R.layout.fragment_menu) {

    private lateinit var binding: FragmentMenuBinding
    private lateinit var adapter: MenuAdapter
    
    private val viewModel: MenuViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMenuBinding.bind(view)

        val restaurantId = arguments?.getLong("restaurantId") ?: -1L
        if (restaurantId == -1L) {
            Toast.makeText(context, "Invalid Restaurant ID", Toast.LENGTH_SHORT).show()
            return
        }

        adapter = MenuAdapter(emptyList())
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        binding.progressBar.visibility = View.VISIBLE
        viewModel.loadMenu(restaurantId)

        viewModel.menuItems.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            result.onSuccess { list ->
                adapter.updateData(list)
            }.onFailure {
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
