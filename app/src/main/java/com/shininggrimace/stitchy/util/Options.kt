package com.shininggrimace.stitchy.util

import com.shininggrimace.stitchy.MainActivity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

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
                quality = 0,
                maxd = 0,
                maxw = 0,
                maxh = 0,
                jpeg = false,
                png = true,
                gif = false,
                bmp = false
            )
    }

    fun toJson(): Result<String> {
        return try {
            val json = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
                .adapter(Options::class.java)
                .toJson(this)
            Result.success(json)
        } catch (e: Exception) {
            MainActivity.logException(e)
            Result.failure(Exception("Error converting options to JSON"))
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
