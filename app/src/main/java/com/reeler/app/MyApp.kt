// MyApp.kt
package com.reeler.app

import android.app.Application
import com.reeler.app.AppController.PreferenceManager

class MyApp : Application() {
    companion object {
        lateinit var prefManager: PreferenceManager
            private set
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize PreferenceManager once for the entire app
        prefManager = PreferenceManager(applicationContext)

    }
}
