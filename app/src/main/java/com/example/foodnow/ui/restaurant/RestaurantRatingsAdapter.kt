package com.example.foodnow.ui.restaurant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.R
import com.example.foodnow.data.RestaurantRatingResponse

class RestaurantRatingsAdapter(
    private var ratings: List<RestaurantRatingResponse> = emptyList()
) : RecyclerView.Adapter<RestaurantRatingsAdapter.RatingViewHolder>() {

    class RatingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvClientName: TextView = view.findViewById(R.id.tvClientName)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
        val tvComment: TextView = view.findViewById(R.id.tvComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RatingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_restaurant_rating, parent, false)
        return RatingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RatingViewHolder, position: Int) {
        val rating = ratings[position]
        holder.tvClientName.text = rating.clientName
        holder.tvDate.text = rating.createdAt.take(10) // Simple date formatting
        holder.ratingBar.rating = rating.rating.toFloat()
        
        if (rating.comment.isNullOrEmpty()) {
            holder.tvComment.visibility = View.GONE
        } else {
            holder.tvComment.visibility = View.VISIBLE
            holder.tvComment.text = rating.comment
        }
    }

    override fun getItemCount() = ratings.size

    fun updateData(newRatings: List<RestaurantRatingResponse>) {
        ratings = newRatings
        notifyDataSetChanged()
    }
}
