package com.example.foodnow.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.foodnow.FoodNowApp
import com.example.foodnow.R
import com.example.foodnow.ui.ViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminDashboardFragment : Fragment(R.layout.fragment_admin_dashboard) {

    private val viewModel: AdminViewModel by viewModels {
        ViewModelFactory((requireActivity().application as FoodNowApp).repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val tvStats = view.findViewById<TextView>(R.id.tvStats)
        
        CoroutineScope(Dispatchers.Main).launch {
             try {
                 val repo = (requireActivity().application as FoodNowApp).repository
                 val response = withContext(Dispatchers.IO) { repo.getSystemStats() }
                 if (response.isSuccessful && response.body() != null) {
                     val stats = response.body()!!
                     val sb = StringBuilder()
                     stats.forEach { (k, v) -> sb.append("$k: $v\n") }
                     tvStats.text = sb.toString()
                 } else {
                     tvStats.text = "Failed to load stats"
                 }
             } catch (e: Exception) {
                 tvStats.text = "Error: ${e.message}"
             }
        }
    }
}
