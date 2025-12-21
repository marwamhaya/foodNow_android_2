package com.example.foodnow.ui.restaurant

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.data.MenuOptionResponse
import com.example.foodnow.ui.ViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.math.BigDecimal

class SupplementGroupFragment : Fragment(R.layout.fragment_supplement_group) {

    private val viewModel: RestaurantViewModel by activityViewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }
    
    private lateinit var adapter: OptionsAdapter
    private var groupId: Long = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        groupId = arguments?.getLong("groupId", -1L) ?: -1L
        if (groupId == -1L) {
             // Handle error
             return
        }
        
        val tvName = view.findViewById<TextView>(R.id.tvGroupName)
        val tvDetails = view.findViewById<TextView>(R.id.tvGroupDetails)
        val rvOptions = view.findViewById<RecyclerView>(R.id.rvOptions)
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAddOption)
        
        rvOptions.layoutManager = LinearLayoutManager(context)
        adapter = OptionsAdapter()
        rvOptions.adapter = adapter
        
        // Observe draftMenuItem
        viewModel.draftMenuItem.observe(viewLifecycleOwner) { item ->
            if (item == null) return@observe
            val group = item.optionGroups.find { it.id == groupId }
            if (group != null) {
                tvName.text = group.name
                tvDetails.text = "Required: ${group.isRequired}, Multiple: ${group.isMultiple}"
                adapter.submitList(group.options)
            }
        }
        
        fab.setOnClickListener {
             showAddOptionDialog()
        }
    }
    
    private fun showAddOptionDialog() {
         // Dialog to add Option
         val view = layoutInflater.inflate(R.layout.dialog_add_option, null) // Need layout
         val etName = view.findViewById<EditText>(R.id.etOptionName)
         val etPrice = view.findViewById<EditText>(R.id.etOptionPrice)
         
         AlertDialog.Builder(requireContext())
             .setTitle("Add Option")
             .setView(view)
             .setPositiveButton("Add") { _, _ ->
                 val name = etName.text.toString()
                 val priceStr = etPrice.text.toString()
                 if (name.isNotEmpty()) {
                     val price = if (priceStr.isNotEmpty()) BigDecimal(priceStr) else BigDecimal.ZERO
                     addOption(name, price)
                 }
             }
             .setNegativeButton("Cancel", null)
             .show()
    }
    
    private fun addOption(name: String, price: BigDecimal) {
        val newId = System.currentTimeMillis() // Temp ID
        val option = MenuOptionResponse(newId, name, price)
        viewModel.addOptionToDraftGroup(groupId, option)
    }
}
