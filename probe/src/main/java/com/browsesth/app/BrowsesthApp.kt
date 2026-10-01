package com.browsesth.app

import android.app.Application
import android.util.Log

/**
 * This build activates Aloha at cold start and auto-loads system plat_support.
 * MainActivity must NOT inflate android.webkit.WebView (native conflict).
 */
class BrowsesthApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            BrowsesthBromiumBootstrap.initializeAll(this, R.array.config_key_system_uuid_map)
            initOk = true
            initError = null
            alohaActive = true
            Log.i(TAG, "Aloha init OK; plat=${BrowsesthBromiumBootstrap.platSupportSource}")
        } catch (t: Throwable) {
            initOk = false
            initError = t
            alohaActive = false
            Log.e(TAG, "Aloha init FAILED; plat=${BrowsesthBromiumBootstrap.platSupportSource}", t)
        }
    }

    companion object {
        private const val TAG = "BrowsesthKernel"
        @Volatile var initOk: Boolean = false
        @Volatile var initError: Throwable? = null
        @Volatile var alohaActive: Boolean = false
    }
}
