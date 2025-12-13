package com.example.foodnow.ui.restaurant

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.data.MenuItemRequest
import com.example.foodnow.ui.ViewModelFactory
import java.math.BigDecimal

class AddEditMenuItemFragment : Fragment(R.layout.fragment_add_edit_menu_item) {

    private val viewModel: RestaurantViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etName = view.findViewById<EditText>(R.id.etName)
        val etDesc = view.findViewById<EditText>(R.id.etDescription)
        val etPrice = view.findViewById<EditText>(R.id.etPrice)
        val btnSave = view.findViewById<Button>(R.id.btnSave)

        // TODO: Handle Edit Mode (fetch arguments)

        btnSave.setOnClickListener {
            val name = etName.text.toString()
            val desc = etDesc.text.toString()
            val priceStr = etPrice.text.toString()

            if (name.isNotEmpty() && priceStr.isNotEmpty()) {
                val price = BigDecimal(priceStr)
                val request = MenuItemRequest(
                    name = name,
                    description = desc,
                    price = price,
                    imageUrl = null,
                    category = "MAIN",
                    isAvailable = true
                )
                
                // For now, always create. Edit needs ID passing.
                viewModel.createMenuItem(request)
                findNavController().popBackStack()
            } else {
                 Toast.makeText(context, "Fill required fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
