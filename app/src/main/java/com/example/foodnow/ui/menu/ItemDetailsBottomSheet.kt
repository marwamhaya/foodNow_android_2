package com.example.foodnow.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import com.example.foodnow.R
import com.example.foodnow.data.MenuItemResponse
import com.example.foodnow.data.MenuOptionResponse
import com.example.foodnow.databinding.BottomSheetItemDetailsBinding
import com.example.foodnow.utils.CartManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.math.BigDecimal

class ItemDetailsBottomSheet(
    private val menuItem: MenuItemResponse,
    private val restaurantId: Long
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetItemDetailsBinding
    private var quantity = 1
    private val selectedOptions = mutableMapOf<Long, MutableList<MenuOptionResponse>>() // GroupId -> List<Option>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetItemDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Bind basic item info
        binding.tvItemName.text = menuItem.name
        binding.tvBasePrice.text = "${String.format("%.2f", menuItem.price)} DH"
        
        // Show Category
        binding.tvCategory.text = menuItem.category ?: "General"

        // Load Image
        if (!menuItem.imageUrl.isNullOrEmpty()) {
            val fullUrl = com.example.foodnow.utils.Constants.getFullImageUrl(menuItem.imageUrl)
            com.bumptech.glide.Glide.with(this)
                .load(fullUrl)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.ivItemImage)
        }

        // Close Button
        binding.btnClose.setOnClickListener { dismiss() }

        updateTotalPrice()

        // Dynamically add options
        menuItem.optionGroups.orEmpty().forEach { group ->
            // Group Title
            val groupTitle = TextView(context).apply {
                text = "${group.name} ${if (group.isRequired) "(Required)" else ""}" // Removed (Optional) to clean up
                textSize = 18f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(resources.getColor(R.color.black, null))
                setPadding(0, 32, 0, 16)
            }
            binding.optionsContainer.addView(groupTitle)

            // Inflate option rows
            group.options.forEach { option ->
                val optionView = LayoutInflater.from(context).inflate(R.layout.item_menu_option, binding.optionsContainer, false)
                val tvName = optionView.findViewById<TextView>(R.id.tvOptionName)
                val tvPrice = optionView.findViewById<TextView>(R.id.tvOptionPrice)
                val rb = optionView.findViewById<RadioButton>(R.id.rbOption)
                val cb = optionView.findViewById<CheckBox>(R.id.cbOption)

                tvName.text = option.name
                tvPrice.text = if (option.extraPrice.toDouble() > 0) "+${option.extraPrice}DH" else "Free"

                if (group.isMultiple) {
                    cb.visibility = View.VISIBLE
                    rb.visibility = View.GONE
                    
                    // Restore state if needed (not persistent here yet)
                    
                    val clickListener = View.OnClickListener {
                        val isChecked = !cb.isChecked 
                        cb.isChecked = isChecked
                        toggleOption(group.id, option, isChecked, true)
                    }
                    optionView.setOnClickListener(clickListener)
                    cb.setOnClickListener { toggleOption(group.id, option, cb.isChecked, true) }
                    
                } else {
                    rb.visibility = View.VISIBLE
                    cb.visibility = View.GONE
                    
                    // Radio Logic handling manually to allow row clicks across multiple inflated views
                    // We need to manage the "RadioGroup" behavior manually since they aren't in a real RadioGroup
                    
                    val updateSelection = {
                        // Uncheck others in this group
                        val childCount = binding.optionsContainer.childCount
                        for (i in 0 until childCount) {
                            val v = binding.optionsContainer.getChildAt(i)
                            // Check if this view belongs to the same group (this is tricky without tags, let's use tags)
                            if (v.tag == group.id) {
                                val otherRb = v.findViewById<RadioButton>(R.id.rbOption)
                                if (otherRb != null) otherRb.isChecked = false
                            }
                        }
                        rb.isChecked = true
                        
                        // Update logic
                        selectedOptions[group.id] = mutableListOf(option)
                        updateTotalPrice()
                    }

                    optionView.tag = group.id // Mark view as belonging to this group
                    optionView.setOnClickListener { updateSelection() }
                    rb.setOnClickListener { updateSelection() }
                }

                binding.optionsContainer.addView(optionView)
            }
        }

        binding.btnIncrease.setOnClickListener {
            quantity++
            binding.tvQuantity.text = String.format("%02d", quantity)
            updateTotalPrice()
        }

        binding.btnDecrease.setOnClickListener {
            if (quantity > 1) {
                quantity--
                binding.tvQuantity.text = String.format("%02d", quantity)
                updateTotalPrice()
            }
        }

        binding.btnAddToCart.setOnClickListener {
            if (validateSelections()) {
                val flatOptions = selectedOptions.values.flatten()
                val optionIds = flatOptions.map { it.id }
                val optionsPrice = flatOptions.sumOf { it.extraPrice.toDouble() }

                val success = CartManager.addItem(menuItem, quantity, optionIds, optionsPrice, restaurantId)
                if (success) {
                    dismiss()
                    Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show()
                } else {
                    // Show mismatch dialog
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Start new basket?")
                        .setMessage("Your basket contains items from another restaurant. Do you want to clear it and add this item?")
                        .setPositiveButton("New Basket") { _, _ ->
                            CartManager.clearCart()
                            CartManager.addItem(menuItem, quantity, optionIds, optionsPrice, restaurantId)
                            dismiss()
                            Toast.makeText(context, "Cart cleared and item added", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }
    }

    private fun toggleOption(groupId: Long, option: MenuOptionResponse, isSelected: Boolean, isMultiple: Boolean) {
        if (!selectedOptions.containsKey(groupId)) {
            selectedOptions[groupId] = mutableListOf()
        }
        val list = selectedOptions[groupId]!!
        if (isSelected) {
            list.add(option)
        } else {
            list.remove(option)
        }
        updateTotalPrice()
    }

    private fun updateTotalPrice() {
        val basePrice = menuItem.price.toDouble()
        val optionsPrice = selectedOptions.values.flatten().sumOf { it.extraPrice.toDouble() }
        val total = (basePrice + optionsPrice) * quantity
        
        // Update Total Text and Add Button
        binding.tvTotalPrice.text = "${String.format("%.2f", total)}DH"
        binding.btnAddToCart.text = "Add to Cart" // Fixed text, total is above
    }

    private fun validateSelections(): Boolean { 
        menuItem.optionGroups.orEmpty().filter { it.isRequired }.forEach { group ->
            if (selectedOptions[group.id].isNullOrEmpty()) {
                Toast.makeText(context, "Please select ${group.name}", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }
}
