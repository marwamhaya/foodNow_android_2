package com.example.foodnow.ui.restaurant

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.ui.ViewModelFactory

class RestaurantHomeMenuTabFragment : Fragment(R.layout.fragment_restaurant_home_menu_tab) {

    private val viewModel: RestaurantViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    private lateinit var rvMenu: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var menuAdapter: RestaurantMenuAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvMenu = view.findViewById(R.id.rvMenu)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        menuAdapter = RestaurantMenuAdapter(
            onItemClick = { menuItem ->
                // Navigate to details page (not edit)
                val bundle = Bundle().apply { putLong("menuItemId", menuItem.id) }
                findNavController().navigate(
                    R.id.action_restaurantHomeFragment_to_menuItemDetailsFragment,
                    bundle
                )
            }
        )
        rvMenu.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = menuAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.menuItems.observe(viewLifecycleOwner) { result ->
            result.onSuccess { items ->
                if (items.isEmpty()) {
                    rvMenu.visibility = View.GONE
                    tvEmptyState.visibility = View.VISIBLE
                } else {
                    rvMenu.visibility = View.VISIBLE
                    tvEmptyState.visibility = View.GONE
                    menuAdapter.submitList(items)
                }
            }
        }
    }

    interface FabStateListener {
        fun onFabStateChanged(visible: Boolean, iconRes: Int, action: () -> Unit)
    }

    override fun onResume() {
        super.onResume()
        // Notify parent to show FAB with add icon
        (parentFragment as? FabStateListener)?.onFabStateChanged(
            visible = true,
            iconRes = android.R.drawable.ic_input_add
        ) {
            // Navigate to add menu item
            viewModel.startCreating()
            findNavController().navigate(R.id.action_restaurantHomeFragment_to_addEditMenuItemFragment)
        }
    }
}
