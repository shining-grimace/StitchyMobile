package com.shininggrimace.stitchy.util

import android.content.Context
import com.shininggrimace.stitchy.R
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import timber.log.Timber

data class Options(
    val horizontal: Boolean,
    val vertical: Boolean,
    val quality: Int,
    val maxd: Int,
    val maxw: Int,
    val maxh: Int,
    val jpeg: Boolean,
    val png: Boolean,
    val gif: Boolean,
    val bmp: Boolean
) {
    companion object {
        fun default(): Options =
            Options(
                horizontal = true,
                vertical = false,
                quality = DEFAULT_JPEG_QUALITY,
                maxd = 0,
                maxw = 0,
                maxh = 0,
                jpeg = false,
                png = true,
                gif = false,
                bmp = false
            )

        const val DEFAULT_JPEG_QUALITY = 90
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
        else -> "png"
    }

    fun getMimeType(): String = when {
        jpeg -> "image/jpeg"
        png -> "image/png"
        gif -> "image/gif"
        bmp -> "image/bmp"
        else -> "image/png"
    }
}
