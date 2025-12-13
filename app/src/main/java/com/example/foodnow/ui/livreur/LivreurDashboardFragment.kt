package com.example.foodnow.ui.livreur

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.ui.ViewModelFactory

class LivreurDashboardFragment : Fragment(R.layout.fragment_livreur_dashboard) {

    private val viewModel: LivreurViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }
    private lateinit var adapter: DeliveryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Assuming fragment_livreur_dashboard has a RecyclerView with ID rvDeliveries
        // If not, I need to update the XML. I recall I created a placeholder XML earlier. 
        // I should overwrite it with one containing RecyclerView.
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvDeliveries)
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = DeliveryAdapter(emptyList()) { delivery ->
            when (delivery.status) {
                "READY_FOR_PICKUP" -> viewModel.updateStatus(delivery.id, "IN_DELIVERY")
                "IN_DELIVERY" -> viewModel.updateStatus(delivery.id, "DELIVERED")
            }
        }
        recyclerView.adapter = adapter

        viewModel.deliveries.observe(viewLifecycleOwner) { result ->
            result.onSuccess { list ->
                adapter.updateDeliveries(list)
            }.onFailure {
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.getAssignedDeliveries()
    }
}
