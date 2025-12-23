package com.example.foodnow.ui.restaurant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodnow.R
import com.example.foodnow.data.MenuItemResponse

class RestaurantMenuAdapter(
    private val onItemClick: (MenuItemResponse) -> Unit
) : ListAdapter<MenuItemResponse, RestaurantMenuAdapter.MenuItemViewHolder>(MenuItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu, parent, false)
        return MenuItemViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MenuItemViewHolder(
        itemView: View,
        private val onItemClick: (MenuItemResponse) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivDish: ImageView = itemView.findViewById(R.id.ivDish)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)

        fun bind(menuItem: MenuItemResponse) {
            tvName.text = menuItem.name
            tvDescription.text = menuItem.description ?: ""
            tvPrice.text = "${menuItem.price} DH"

            // Load image with error handling and logging
            menuItem.imageUrl?.let { url ->
                val fullUrl = if (url.startsWith("http")) {
                    url
                } else {
                    com.example.foodnow.utils.Constants.getFullImageUrl(url)
                }
                
                Glide.with(itemView.context)
                    .load(fullUrl)
                    .placeholder(R.drawable.bg_bottom_sheet)
                    .error(R.drawable.bg_bottom_sheet)
                    .addListener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                        override fun onLoadFailed(
                            e: com.bumptech.glide.load.engine.GlideException?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            android.util.Log.e("MenuAdapter", "Failed to load image: $fullUrl", e)
                            return false
                        }

                        override fun onResourceReady(
                            resource: android.graphics.drawable.Drawable,
                            model: Any,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                            dataSource: com.bumptech.glide.load.DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }
                    })
                    .into(ivDish)
            } ?: run {
                ivDish.setImageResource(R.drawable.bg_bottom_sheet)
            }

            itemView.setOnClickListener {
                onItemClick(menuItem)
            }
        }
    }

    class MenuItemDiffCallback : DiffUtil.ItemCallback<MenuItemResponse>() {
        override fun areItemsTheSame(oldItem: MenuItemResponse, newItem: MenuItemResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MenuItemResponse, newItem: MenuItemResponse): Boolean {
            return oldItem == newItem
        }
    }
}
