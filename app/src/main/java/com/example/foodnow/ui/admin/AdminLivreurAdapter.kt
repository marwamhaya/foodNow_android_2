package com.example.foodnow.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.R
import com.example.foodnow.data.User

class AdminLivreurAdapter(
    private var users: List<User>,
    private val onToggleClick: (User) -> Unit
) : RecyclerView.Adapter<AdminLivreurAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvLivreurName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvLivreurStatus)
        val btnToggle: Button = itemView.findViewById(R.id.btnToggleStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // reuse layout or create item_admin_livreur
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_livreur, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = users[position]
        holder.tvName.text = item.fullName + " (${item.email})"
        holder.tvStatus.text = if (item.isActive) "Active" else "Inactive"
        holder.btnToggle.text = if (item.isActive) "Disable" else "Enable"
        
        holder.btnToggle.setOnClickListener { onToggleClick(item) }
    }

    override fun getItemCount() = users.size

    fun updateData(newData: List<User>) {
        users = newData
        notifyDataSetChanged()
    }
}
