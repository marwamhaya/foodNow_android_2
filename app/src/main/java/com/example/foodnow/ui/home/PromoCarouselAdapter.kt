package com.example.foodnow.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodnow.R
import com.google.android.material.card.MaterialCardView

data class PromoItem(
    val title: String,
    val backgroundColor: Int, // Color resource ID
    val imageResId: Int
)

class PromoCarouselAdapter(
    private val items: List<PromoItem>,
    private val onOrderClick: () -> Unit
) : RecyclerView.Adapter<PromoCarouselAdapter.PromoViewHolder>() {

    class PromoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.promoCard)
        val tvTitle: TextView = itemView.findViewById(R.id.tvPromoTitle)
        val btnOrder: Button = itemView.findViewById(R.id.btnPromoOrder)
        val ivImage: android.widget.ImageView = itemView.findViewById(R.id.ivPromoImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_promo_card, parent, false)
        return PromoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PromoViewHolder, position: Int) {
        val promo = items[position]
        holder.tvTitle.text = promo.title
        // Use color defined in promo item
        holder.card.setCardBackgroundColor(holder.itemView.context.getColor(promo.backgroundColor))
        // Set dynamic image
        holder.ivImage.setImageResource(promo.imageResId)
        
        holder.btnOrder.setOnClickListener { onOrderClick() }
    }

    override fun getItemCount() = items.size
}
