package com.shininggrimace.stitchy.holder

import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.shininggrimace.stitchy.R

class ImageViewHolder(view: View): RecyclerView.ViewHolder(view) {

    private val imageView: ImageView = view.findViewById(R.id.image_preview)

    fun bind(uri: Uri) {
        imageView.setImageURI(uri)
    }
}