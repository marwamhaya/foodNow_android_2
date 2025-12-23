package com.example.foodnow.ui.orders

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.data.Order
import com.example.foodnow.databinding.FragmentOrderListBinding
import com.example.foodnow.ui.ViewModelFactory

class OrderListFragment : Fragment(R.layout.fragment_order_list) {

    companion object {
        private const val ARG_FILTER = "filter_type"
        
        fun newInstance(filter: String): OrderListFragment {
            val fragment = OrderListFragment()
            val args = Bundle()
            args.putString(ARG_FILTER, filter)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var binding: FragmentOrderListBinding
    private lateinit var adapter: OrdersAdapter
    private var filterType: String = "ACTIVE"

    // Each fragment has its own ViewModel instance which fetches data independently.
    // simpler than sharing for now, though slightly inefficient (double fetch).
    // Or we could scope to requireActivity() if we want to share.
    // Let's scope to requireParentFragment() is not directly supported by by viewModels without helper, 
    // so let's stick to new instance or activity scope. 
    // Given the ViewModel fetches in init/onViewCreated via method call, distinct instances are fine.
    private val viewModel: OrdersViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentOrderListBinding.bind(view)
        
        filterType = arguments?.getString(ARG_FILTER) ?: "ACTIVE"

        adapter = OrdersAdapter(
            orders = emptyList(),
            onClick = { order ->
                val detailsSheet = OrderDetailsBottomSheet(order)
                detailsSheet.show(parentFragmentManager, "OrderDetailsBottomSheet")
            },
            onRateClick = { order ->
                 val ratingSheet = RatingBottomSheetFragment.newInstance(
                     order.id, 
                     order.restaurantName,
                     order.restaurantAddress,
                     order.restaurantImageUrl
                 )
                 ratingSheet.show(parentFragmentManager, "RatingBottomSheetFragment")
            }
        )

        binding.rvOrders.layoutManager = LinearLayoutManager(context)
        binding.rvOrders.adapter = adapter

        viewModel.orders.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            result.onSuccess { list ->
                val filteredList = filterOrders(list)
                if (filteredList.isEmpty()) {
                    binding.rvOrders.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    // Update empty text based on tab
                    binding.tvEmpty.text = if (filterType == "ACTIVE") "No active orders" else "No past orders"
                } else {
                    binding.rvOrders.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                    adapter.updateData(filteredList)
                }
            }.onFailure {
                binding.rvOrders.visibility = View.GONE
                binding.tvError.text = it.message
                binding.tvError.visibility = View.VISIBLE
            }
        }

        binding.progressBar.visibility = View.VISIBLE
        viewModel.fetchOrders()
    }

    private fun filterOrders(allOrders: List<Order>): List<Order> {
        return if (filterType == "ACTIVE") {
            allOrders.filter { it.status !in listOf("DELIVERED", "CANCELLED", "DECLINED") }
        } else {
            allOrders.filter { it.status in listOf("DELIVERED", "CANCELLED", "DECLINED") }
        }
    }
}
