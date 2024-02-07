package com.shininggrimace.stitchy.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shininggrimace.stitchy.R
import com.shininggrimace.stitchy.holder.ImageViewHolder

class ImageAdapter(
    private val images: List<Uri>
): RecyclerView.Adapter<ImageViewHolder>() {

    override fun getItemCount(): Int = images.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.holder_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val image = images[position]
        holder.bind(image)
    }
}