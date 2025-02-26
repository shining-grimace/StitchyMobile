package com.shininggrimace.stitchy.util

import android.content.Context
import androidx.annotation.Keep
import com.shininggrimace.stitchy.R
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import timber.log.Timber

@Keep
data class Options(
    val horizontal: Boolean,
    val vertical: Boolean,
    val small: Boolean,
    val fast: Boolean,
    val quality: Int,
    val maxd: Int,
    val maxw: Int,
    val maxh: Int,
    val jpeg: Boolean,
    val png: Boolean,
    val gif: Boolean,
    val bmp: Boolean,
    val webp: Boolean
) {
    companion object {
        fun default(): Options =
            Options(
                horizontal = true,
                vertical = false,
                small = false,
                fast = false,
                quality = DEFAULT_JPEG_QUALITY,
                maxd = MAX_DIMENSION,
                maxw = 0,
                maxh = 0,
                jpeg = false,
                png = true,
                gif = false,
                bmp = false,
                webp = false
            )

        const val DEFAULT_JPEG_QUALITY = 90
        const val MAX_DIMENSION = 4096
    }

    fun toJson(context: Context): Result<String> {
        return try {
            val json = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
                .adapter(Options::class.java)
                .toJson(this)
            Result.success(json)
        } catch (e: Exception) {
            Timber.e(e)
            Result.failure(Exception(
                context.getString(R.string.error_parsing_setting)))
        }
    }

    fun getFileExtension(): String = when {
        jpeg -> "jpg"
        png -> "png"
        gif -> "gif"
        bmp -> "bmp"
        webp -> "webp"
        else -> "png"
    }

    fun getMimeType(): String = when {
        jpeg -> "image/jpeg"
        png -> "image/png"
        gif -> "image/gif"
        bmp -> "image/bmp"
        webp -> "image/webp"
        else -> "image/png"
    }
}
