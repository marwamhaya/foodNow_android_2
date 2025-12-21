package com.example.foodnow.ui.restaurant

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.ui.ViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton

import androidx.fragment.app.activityViewModels

class RestaurantMenuFragment : Fragment(R.layout.fragment_restaurant_menu) {

    private val viewModel: RestaurantViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }
    private lateinit var adapter: MenuItemAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvMenuItems)
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAddMenuItem)
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = MenuItemAdapter(emptyList(),
            onItemClick = { item ->
                val bundle = Bundle().apply { putLong("menuItemId", item.id) }
                findNavController().navigate(R.id.action_menu_to_details, bundle)
            },
            onEditClick = { item ->
                val bundle = Bundle().apply { putLong("menuItemId", item.id) }
                findNavController().navigate(R.id.action_menu_to_addEdit, bundle)
            },
            onDeleteClick = { item ->
                viewModel.deleteMenuItem(item.id)
            }
        )
        recyclerView.adapter = adapter

        fab.setOnClickListener {
            // Pass -1 for new item
            val bundle = Bundle().apply { putLong("menuItemId", -1L) }
            findNavController().navigate(R.id.action_menu_to_addEdit, bundle)
        }

        val tvEmpty = view.findViewById<android.widget.TextView>(R.id.tvEmptyState)
        val tvRating = view.findViewById<android.widget.TextView>(R.id.tvRating)
        val tvRatingCount = view.findViewById<android.widget.TextView>(R.id.tvRatingCount)
        val containerRatings = view.findViewById<View>(R.id.containerRatings)

        containerRatings.setOnClickListener {
             try {
                 findNavController().navigate(R.id.action_menu_to_ratings)
             } catch (e: Exception) {
                 // Fallback if action not found or similar
                 Toast.makeText(context, "Nav error: ${e.message}", Toast.LENGTH_SHORT).show()
             }
        }

        viewModel.restaurant.observe(viewLifecycleOwner) { result ->
            result.onSuccess { rest ->
                 tvRating.text = String.format("%.1f", rest.averageRating ?: 0.0)
                 tvRatingCount.text = "(${rest.ratingCount ?: 0})"
            }
        }

        viewModel.menuItems.observe(viewLifecycleOwner) { result ->
            result.onSuccess { items ->
                adapter.updateItems(items)
                if (items.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }.onFailure {
                Toast.makeText(context, "Error loading menu", Toast.LENGTH_SHORT).show()
            }
        }

        // Trigger load
        viewModel.getMyRestaurant()
    }
}
