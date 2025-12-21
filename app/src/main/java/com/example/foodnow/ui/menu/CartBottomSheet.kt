package com.example.foodnow.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageButton
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.R
import com.example.foodnow.FoodNowApp
import com.example.foodnow.databinding.BottomSheetCartBinding
import com.example.foodnow.ui.ViewModelFactory
import com.example.foodnow.utils.CartItem
import com.example.foodnow.utils.CartManager
import com.example.foodnow.utils.Constants
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class CartBottomSheet(
    private val externalViewModel: MenuViewModel? = null, 
    private val externalRestaurantId: Long? = null
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetCartBinding
    private lateinit var adapter: CartAdapter
    private lateinit var viewModel: MenuViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Use external ViewModel if provided, otherwise create our own scoped to Activity
        // This ensures the ViewModel (and its coroutines) survive if CartBottomSheet is dismissed (e.g. when opening PaymentBottomSheet)
        viewModel = externalViewModel ?: run {
            val app = requireActivity().application as FoodNowApp
            ViewModelProvider(requireActivity(), ViewModelFactory(app.repository))[MenuViewModel::class.java]
        }
        
        // Get restaurant ID from external source or from CartManager
        val restaurantId = externalRestaurantId ?: CartManager.getCurrentRestaurantId()

        adapter = CartAdapter(
            onQuantityChange = { idx, newQty ->
                CartManager.updateQuantity(idx, newQty)
            },
            onRemoveClick = { idx ->
                CartManager.removeItem(idx)
            }
        )
        
        binding.rvCartItems.layoutManager = LinearLayoutManager(context)
        binding.rvCartItems.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            CartManager.cartItems.collect { items ->
                adapter.submitList(items)
                updateTotal(items)
                if (items.isEmpty()) {
                     dismiss()
                }
            }
        }

        binding.btnPlaceOrder.setOnClickListener {
            if (restaurantId != null) {
                // Open Payment Sheet with the ViewModel (always available now)
                val paymentSheet = PaymentBottomSheet(viewModel, restaurantId)
                paymentSheet.show(parentFragmentManager, "PaymentBottomSheet")
                dismiss()
            } else {
                // Cart is empty or no restaurant selected
                Toast.makeText(context, "Your cart is empty.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun updateTotal(items: List<CartItem>) {
        val total = CartManager.getTotal()
        binding.tvTotal.text = "${String.format("%.2f", total)} DH"
        // Button text is just "Checkout", no dynamic total anymore in button
        binding.btnPlaceOrder.text = "Checkout"
    }
}

class CartAdapter(
    private val onQuantityChange: (Int, Int) -> Unit,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {
    private var items = listOf<CartItem>()

    fun submitList(newItems: List<CartItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        
        holder.tvName.text = item.menuItem.name
        holder.tvQuantity.text = String.format("%02d", item.quantity) // Format 01, 02
        holder.tvPrice.text = "${String.format("%.2f", item.totalPrice)} DH"
        
        // Load Image
        val imageUrl = item.menuItem.imageUrl
        if (!imageUrl.isNullOrEmpty()) {
             val fullUrl = Constants.getFullImageUrl(imageUrl)
             Glide.with(context)
                 .load(fullUrl)
                 .centerCrop()
                 .into(holder.ivImage)
        } else {
             holder.ivImage.setImageResource(R.drawable.placeholder_food)
        }

        // Display selected options/supplements
        if (item.selectedOptionIds.isNotEmpty()) {
            val optionNames = mutableListOf<String>()
            item.menuItem.optionGroups?.forEach { group ->
                group.options?.forEach { option ->
                    if (item.selectedOptionIds.contains(option.id)) {
                        optionNames.add(option.name)
                    }
                }
            }
            if (optionNames.isNotEmpty()) {
                holder.tvOptions.visibility = View.VISIBLE
                holder.tvOptions.text = optionNames.joinToString(", ")
            } else {
                holder.tvOptions.visibility = View.GONE
            }
        } else {
            holder.tvOptions.visibility = View.GONE
        }
        
        // Quantity controls
        holder.btnDecrease.setOnClickListener {
            val newQty = (item.quantity - 1).coerceAtLeast(0)
            onQuantityChange(position, newQty)
        }
        
        holder.btnIncrease.setOnClickListener {
            onQuantityChange(position, item.quantity + 1)
        }
        
        // Remove button is hidden in layout but can keep listener if visible
        holder.btnRemove.setOnClickListener {
             onRemoveClick(position)
        }
    }

    override fun getItemCount() = items.size

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvCartItemName)
        val tvOptions: TextView = itemView.findViewById(R.id.tvCartItemOptions)
        val tvPrice: TextView = itemView.findViewById(R.id.tvCartItemPrice)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvCartItemQuantity)
        val btnDecrease: View = itemView.findViewById(R.id.btnDecreaseQty) // View or ImageButton
        val btnIncrease: View = itemView.findViewById(R.id.btnIncreaseQty)
        val btnRemove: View = itemView.findViewById(R.id.btnRemove)
        val ivImage: android.widget.ImageView = itemView.findViewById(R.id.ivCartItemImage)
    }
}
