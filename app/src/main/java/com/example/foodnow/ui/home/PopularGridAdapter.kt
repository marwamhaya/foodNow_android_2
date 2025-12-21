package com.example.foodnow.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodnow.R
import com.example.foodnow.data.MenuItemResponse

class PopularGridAdapter(
    private var items: List<MenuItemResponse>,
    private val onItemClick: (MenuItemResponse) -> Unit
) : RecyclerView.Adapter<PopularGridAdapter.GridViewHolder>() {

    class GridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivItem: ImageView = itemView.findViewById(R.id.ivItem)
        val tvName: TextView = itemView.findViewById(R.id.tvItemName)
        val tvRestaurant: TextView = itemView.findViewById(R.id.tvItemRestaurant)
        val tvPrice: TextView = itemView.findViewById(R.id.tvItemPrice)
        val btnAdd: Button = itemView.findViewById(R.id.btnAddToCart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_popular_grid, parent, false)
        return GridViewHolder(view)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvPrice.text = "${item.price} DH"
        holder.tvRestaurant.text = item.restaurantName ?: "Unknown Restaurant"

        if (!item.imageUrl.isNullOrEmpty()) {
             val fullUrl = com.example.foodnow.utils.Constants.getFullImageUrl(item.imageUrl)
             Glide.with(holder.itemView.context)
                 .load(fullUrl)
                 .placeholder(android.R.drawable.ic_menu_gallery)
                 .into(holder.ivItem)
        } else {
            holder.ivItem.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.btnAdd.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<MenuItemResponse>) {
        items = newItems
        notifyDataSetChanged()
    }
}
