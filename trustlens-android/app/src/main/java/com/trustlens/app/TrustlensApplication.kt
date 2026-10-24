package com.trustlens.app

import android.app.Application
import android.content.Context
import com.trustlens.app.data.remote.RetrofitClient

/**
 * TRUSTLENS Application entry class.
 */
class TrustlensApplication : Application() {

    companion object {
        var instance: TrustlensApplication? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Load persisted custom backend URL if previously saved by user
        val prefs = getSharedPreferences("trustlens_config", Context.MODE_PRIVATE)
        val savedUrl = prefs.getString("backend_base_url", null)
        if (!savedUrl.isNullOrBlank()) {
            RetrofitClient.setBaseUrl(savedUrl)
        }
    }
}
