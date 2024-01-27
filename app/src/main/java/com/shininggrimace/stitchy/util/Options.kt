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
    val maxh: Int
) {
    companion object {
        fun default(): Options =
            Options(
                horizontal = true,
                vertical = false,
                quality = 0,
                maxd = 0,
                maxw = 0,
                maxh = 0
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
}
