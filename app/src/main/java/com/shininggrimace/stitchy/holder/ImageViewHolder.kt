package com.shininggrimace.stitchy.holder

import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.shininggrimace.stitchy.R
import timber.log.Timber

class ImageViewHolder(view: View): RecyclerView.ViewHolder(view) {

    private val imageView: ImageView = view.findViewById(R.id.image_preview)
    private val loadingView: ProgressBar = view.findViewById(R.id.image_loading)

    private var lastBoundUri: Uri? = null

    fun bind(uri: Uri) {
        if (lastBoundUri == uri) {
            return
        }
        lastBoundUri = uri
        imageView.load(uri) {
            listener(
                onStart = { showLoadingState() },
                onCancel = { showErrorState(null) },
                onError = { _, throwable -> showErrorState(throwable) },
                onSuccess = { _, _ -> showImageLoadedState() }
            )
        }
    }

    private fun showLoadingState() {
        imageView.visibility = View.INVISIBLE
        loadingView.visibility = View.VISIBLE
    }

    private fun showErrorState(error: Throwable?) {
        error?.let { Timber.e(it) }
        imageView.visibility = View.VISIBLE
        loadingView.visibility = View.GONE
        imageView.setImageResource(R.drawable.ic_error_blocked)
    }

    private fun showImageLoadedState() {
        imageView.visibility = View.VISIBLE
        loadingView.visibility = View.GONE
    }
}