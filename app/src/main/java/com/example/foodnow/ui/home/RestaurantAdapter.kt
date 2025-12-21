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

class RestaurantAdapter(
    private var restaurants: List<RestaurantResponse>,
    private val onRestaurantClick: (Long) -> Unit
) : RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder>() {

    class RestaurantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivRestaurant: ImageView = itemView.findViewById(R.id.ivRestaurant)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvRating: TextView = itemView.findViewById(R.id.tvRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_restaurant, parent, false)
        return RestaurantViewHolder(view)
    }

    override fun onBindViewHolder(holder: RestaurantViewHolder, position: Int) {
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
    }

    override fun getItemCount() = restaurants.size

    fun updateData(newRestaurants: List<RestaurantResponse>) {
        restaurants = newRestaurants
        notifyDataSetChanged()
    }
}
