package com.shininggrimace.stitchy

import android.app.Application
import timber.log.Timber
import timber.log.Timber.DebugTree

class StitchyApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }
}