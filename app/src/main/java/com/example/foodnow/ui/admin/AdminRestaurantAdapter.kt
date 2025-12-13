package com.example.foodnow.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.R
import com.example.foodnow.data.RestaurantResponse

class AdminRestaurantAdapter(
    private var restaurants: List<RestaurantResponse>,
    private val onToggleClick: (RestaurantResponse) -> Unit,
    private val onItemClick: (RestaurantResponse) -> Unit
) : RecyclerView.Adapter<AdminRestaurantAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvRestoName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvRestoStatus)
        val btnToggle: Button = itemView.findViewById(R.id.btnToggleStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_restaurant, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = restaurants[position]
        holder.tvName.text = item.name
        holder.tvStatus.text = if (item.isActive) "Active" else "Inactive"
        holder.btnToggle.text = if (item.isActive) "Disable" else "Enable"
        
        holder.btnToggle.setOnClickListener { onToggleClick(item) }
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = restaurants.size

    fun updateData(newData: List<RestaurantResponse>) {
        restaurants = newData
        notifyDataSetChanged()
    }
}
