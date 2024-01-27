package com.shininggrimace.stitchy.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.lang.Exception

private const val PREFS_FILE_NAME = "prefs"
private const val PREFS_OPTIONS_KEY = "options"

object OptionsRepository {

    private var prefs: SharedPreferences? = null

    fun getOptions(context: Context): Options? {

        val prefs = prefs ?: run {
            val newInstance = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
            prefs = newInstance
            newInstance
        }

        val json = prefs.getString(PREFS_OPTIONS_KEY, null) ?: return null

        return try {
            Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
                .adapter(Options::class.java)
                .fromJson(json)
        } catch (e: Exception) {
            Log.d("StitchyMobile", "Error decoding options: $e")
            null
        }
    }
}