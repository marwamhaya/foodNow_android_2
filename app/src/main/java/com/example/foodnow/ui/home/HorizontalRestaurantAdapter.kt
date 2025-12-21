package com.example.foodnow.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodnow.R
import com.example.foodnow.data.RestaurantResponse

class HorizontalRestaurantAdapter(
    private var restaurants: List<RestaurantResponse>,
    private val onRestaurantClick: (Long) -> Unit,
    private val onSeeAllClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_FOOTER = 1
        private const val MAX_ITEMS = 4
    }

    class HorizontalRestaurantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivRestaurant: ImageView = itemView.findViewById(R.id.ivRestaurant)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvRating: TextView = itemView.findViewById(R.id.tvRating)
    }

    class SeeAllViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ITEM) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_restaurant_horizontal, parent, false)
            HorizontalRestaurantViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_see_all, parent, false)
            SeeAllViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HorizontalRestaurantViewHolder) {
            val restaurant = restaurants[position]
            holder.tvName.text = restaurant.name
            holder.tvDescription.text = restaurant.description ?: "No description available"
            
            val rating = restaurant.averageRating ?: 0.0
            val count = restaurant.ratingCount ?: 0
            holder.tvRating.text = "${String.format("%.1f", rating)} ($count)"
            
            if (!restaurant.imageUrl.isNullOrEmpty()) {
                val fullUrl = com.example.foodnow.utils.Constants.getFullImageUrl(restaurant.imageUrl)
                Glide.with(holder.itemView.context)
                    .load(fullUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivRestaurant)
            } else {
                holder.ivRestaurant.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            holder.itemView.setOnClickListener {
                onRestaurantClick(restaurant.id)
            }
        } else if (holder is SeeAllViewHolder) {
            holder.itemView.setOnClickListener {
                onSeeAllClick()
            }
        }
    }

    override fun getItemCount(): Int {
        // If empty, 0. If > 0, min(size, 4) + 1 (for footer)
        return if (restaurants.isEmpty()) 0 else minOf(restaurants.size, MAX_ITEMS) + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == minOf(restaurants.size, MAX_ITEMS)) TYPE_FOOTER else TYPE_ITEM
    }

    fun updateData(newRestaurants: List<RestaurantResponse>) {
        restaurants = newRestaurants
        notifyDataSetChanged()
    }
}
