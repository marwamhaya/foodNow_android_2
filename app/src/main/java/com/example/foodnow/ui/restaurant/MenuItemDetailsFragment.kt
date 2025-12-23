package com.example.foodnow.ui.restaurant

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.ui.ViewModelFactory

class MenuItemDetailsFragment : Fragment(R.layout.fragment_menu_item_details) {

    private val viewModel: RestaurantViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    private var menuItemId: Long = -1L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        menuItemId = arguments?.getLong("menuItemId", -1L) ?: -1L
        if (menuItemId == -1L) {
            findNavController().popBackStack()
            return
        }

        val ivImage = view.findViewById<ImageView>(R.id.ivMenuItemImage)
        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvAvailability = view.findViewById<TextView>(R.id.tvAvailability)
        val tvCategory = view.findViewById<TextView>(R.id.tvCategory)
        val tvPrice = view.findViewById<TextView>(R.id.tvPrice)
        val tvDescription = view.findViewById<TextView>(R.id.tvDescription)
        val tvSupplementsHeader = view.findViewById<TextView>(R.id.tvSupplementsHeader)
        val rvOptionGroups = view.findViewById<RecyclerView>(R.id.rvOptionGroups)
        val tvNoSupplements = view.findViewById<TextView>(R.id.tvNoSupplements)
        val btnEdit = view.findViewById<Button>(R.id.btnEdit)
        val btnDelete = view.findViewById<Button>(R.id.btnDelete)

        rvOptionGroups.layoutManager = LinearLayoutManager(context)

        // Observe selected menu item
        viewModel.selectedMenuItem.observe(viewLifecycleOwner) { item ->
            if (item == null) return@observe

            // Image
            if (!item.imageUrl.isNullOrEmpty()) {
                val fullUrl = com.example.foodnow.utils.Constants.getFullImageUrl(item.imageUrl)
                Glide.with(this).load(fullUrl).into(ivImage)
            }

            // Name
            tvName.text = item.name

            // Availability badge
            if (item.isAvailable) {
                tvAvailability.text = "🟢 Available"
                tvAvailability.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
            } else {
                tvAvailability.text = "Not Available"
                tvAvailability.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
            }

            // Category
            tvCategory.text = "${item.category ?: "Uncategorized"}"

            // Price
            tvPrice.text = "${item.price} DH"

            // Description
            tvDescription.text = item.description ?: "No description available"

            // Option Groups
            val groups = item.optionGroups
            if (groups.isNotEmpty()) {
                tvNoSupplements.visibility = View.GONE
                rvOptionGroups.visibility = View.VISIBLE
                rvOptionGroups.adapter = OptionGroupAdapter { /* Read-only, no action */ }
                (rvOptionGroups.adapter as OptionGroupAdapter).submitList(groups)
            } else {
                tvNoSupplements.visibility = View.VISIBLE
                rvOptionGroups.visibility = View.GONE
            }
        }

        // Load item (fallback to API if not in cache)
        viewModel.loadMenuItemById(menuItemId)

        // Edit button
        btnEdit.setOnClickListener {
            val bundle = Bundle().apply { putLong("menuItemId", menuItemId) }
            findNavController().navigate(R.id.action_details_to_edit, bundle)
        }

        // Delete button with confirmation
        btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Menu Item")
            .setMessage("Are you sure you want to delete this menu item? This action cannot be undone.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Delete") { _, _ ->
                deleteMenuItem()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteMenuItem() {
        viewModel.deleteMenuItem(menuItemId)
        
        // Show success message
        android.widget.Toast.makeText(
            requireContext(),
            "Menu item deleted successfully",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        
        // Navigate back to home
        findNavController().popBackStack(R.id.restaurantHomeFragment, false)
    }
}
