package com.example.foodnow.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodnow.R
import com.example.foodnow.data.MenuItemResponse
import kotlin.math.min

class HorizontalPopularItemsAdapter(
    private var items: List<MenuItemResponse>,
    private val onItemClick: (MenuItemResponse) -> Unit,
    private val onSeeAllClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_FOOTER = 1
        private const val MAX_ITEMS = 3
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivItem: ImageView = itemView.findViewById(R.id.ivItem)
        val tvName: TextView = itemView.findViewById(R.id.tvItemName)
        val tvRestaurant: TextView = itemView.findViewById(R.id.tvItemRestaurant)
        val tvPrice: TextView = itemView.findViewById(R.id.tvItemPrice)
    }

    class SeeAllViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ITEM) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_popular_horizontal, parent, false)
            ItemViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_see_all, parent, false)
            SeeAllViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            val item = items[position]
            holder.tvName.text = item.name
            holder.tvPrice.text = "${String.format("%.2f", item.price)} DH"

            if (!item.imageUrl.isNullOrEmpty()) {
                val fullUrl = com.example.foodnow.utils.Constants.getFullImageUrl(item.imageUrl)
                Glide.with(holder.itemView.context)
                    .load(fullUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivItem)
            } else {
                holder.ivItem.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            
            holder.tvRestaurant.text = item.restaurantName ?: "Unknown Restaurant"

            holder.itemView.setOnClickListener {
                onItemClick(item)
            }
        } else if (holder is SeeAllViewHolder) {
            holder.itemView.setOnClickListener {
                onSeeAllClick()
            }
        }
    }

    override fun getItemCount(): Int {
        return if (items.isEmpty()) 0 else minOf(items.size, MAX_ITEMS) + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == minOf(items.size, MAX_ITEMS)) TYPE_FOOTER else TYPE_ITEM
    }

    fun updateData(newItems: List<MenuItemResponse>) {
        items = newItems
        notifyDataSetChanged()
    }
}
