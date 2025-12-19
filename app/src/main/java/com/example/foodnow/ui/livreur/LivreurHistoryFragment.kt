package com.example.foodnow.ui.livreur

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.ui.ViewModelFactory

class LivreurHistoryFragment : Fragment(R.layout.fragment_livreur_dashboard) { // Reuse layout or create new

    private val viewModel: LivreurViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }
    private lateinit var adapter: DeliveryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvDeliveries)
        val title = view.findViewById<TextView>(R.id.tvDashboardTitle) // Assumed ID
        title.text = "Delivery History"

        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = DeliveryAdapter(emptyList()) { 
            // No action in history
        }
        recyclerView.adapter = adapter

        viewModel.deliveries.observe(viewLifecycleOwner) { result ->
            result.onSuccess { list ->
                adapter.updateDeliveries(list)
            }
        }

        // Call history method
        viewModel.getDeliveryHistory()
    }
}
