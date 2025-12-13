package com.example.foodnow.ui.restaurant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.R
import com.example.foodnow.data.MenuItemResponse

class MenuItemAdapter(
    private var items: List<MenuItemResponse>,
    private val onEditClick: (MenuItemResponse) -> Unit,
    private val onDeleteClick: (MenuItemResponse) -> Unit
) : RecyclerView.Adapter<MenuItemAdapter.MenuItemViewHolder>() {

    class MenuItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvMenuItemName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvMenuItemPrice)
        val tvDesc: TextView = itemView.findViewById(R.id.tvMenuItemDesc)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_restaurant_menu, parent, false)
        return MenuItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvPrice.text = "$${item.price}"
        holder.tvDesc.text = item.description ?: ""

        holder.btnEdit.setOnClickListener { onEditClick(item) }
        holder.btnDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<MenuItemResponse>) {
        items = newItems
        notifyDataSetChanged()
    }
}
