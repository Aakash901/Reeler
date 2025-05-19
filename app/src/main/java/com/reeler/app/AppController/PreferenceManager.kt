package com.reeler.app.AppController

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val pref: SharedPreferences
    private val editor: SharedPreferences.Editor

    companion object {
        const val MIN_INTERVAL = 3L
        const val MIN_REEL_LIMIT = 5L
        const val MAX_REEL_LIMIT = 5000L
    }

    init {
        pref = context.getSharedPreferences(SharedPrefConst.PREF_NAME, Context.MODE_PRIVATE)
        editor = pref.edit()
    }
    fun areSettingsValid(): Boolean {
        val interval = getScrollInterval()
        val reelLimit = getReelLimit()
        val selectedApp = getSelectedApp()

        return interval >= MIN_INTERVAL &&
                reelLimit in MIN_REEL_LIMIT..MAX_REEL_LIMIT &&
                selectedApp.isNotEmpty()
    }
    // Improved save methods with validation
    fun saveScrollInterval(interval: Long): Boolean {
        return if (isValidInterval(interval)) {
            editor.putLong(SharedPrefConst.KEY_SCROLL_INTERVAL, interval).commit()
            true
        } else false
    }

    fun saveReelLimit(limit: Long): Boolean {
        return if (isValidReelLimit(limit)) {
            editor.putLong(SharedPrefConst.KEY_REEL_LIMIT, limit).commit()
            true
        } else false
    }

    // Validation methods
    fun isValidInterval(interval: Long): Boolean = interval >= MIN_INTERVAL
    fun isValidReelLimit(limit: Long): Boolean = limit in MIN_REEL_LIMIT..MAX_REEL_LIMIT

    // Transaction support for batch updates
    fun saveSettings(interval: Long? = null, reelLimit: Long? = null): Boolean {
        var isValid = true
        editor.apply {
            interval?.let {
                if (isValidInterval(it)) {
                    putLong(SharedPrefConst.KEY_SCROLL_INTERVAL, it)
                } else {
                    isValid = false
                }
            }
            reelLimit?.let {
                if (isValidReelLimit(it)) {
                    putLong(SharedPrefConst.KEY_REEL_LIMIT, it)
                } else {
                    isValid = false
                }
            }
        }
        return if (isValid) {
            editor.commit()
            true
        } else false
    }

    // Add method to get specific validation errors
    fun getSettingsValidationErrors(): List<String> {
        val errors = mutableListOf<String>()

        val interval = getScrollInterval()
        if (interval < MIN_INTERVAL) {
            errors.add("Scroll interval must be at least $MIN_INTERVAL seconds")
        }

        val reelLimit = getReelLimit()
        if (reelLimit !in MIN_REEL_LIMIT..MAX_REEL_LIMIT) {
            errors.add("Reel limit must be between $MIN_REEL_LIMIT and $MAX_REEL_LIMIT")
        }

        val selectedApp = getSelectedApp()
        if (selectedApp.isEmpty()) {
            errors.add("Please select an app")
        }

        return errors
    }
    // Other methods remain the same
    fun saveSelectedApp(app: String) {
        editor.putString(SharedPrefConst.KEY_SELECTED_APP, app).commit()
    }

    fun saveSkipAds(skip: Boolean) {
        editor.putBoolean(SharedPrefConst.KEY_SKIP_ADS, skip).apply()
    }

    fun getSelectedApp(): String {
        return pref.getString(SharedPrefConst.KEY_SELECTED_APP, "") ?: ""
    }
    fun setFirstTimeLaunch(isFirstTime: Boolean) {
        editor.putBoolean(SharedPrefConst.IS_FIRST_TIME_LAUNCH, isFirstTime)
        editor.commit()  // Using commit() for immediate write
    }

    fun isFirstTimeLaunch(): Boolean {
        return pref.getBoolean(SharedPrefConst.IS_FIRST_TIME_LAUNCH, true)
    }
    // Updated return type to be explicit
    fun getSkipAds(): Boolean {
        return pref.getBoolean(SharedPrefConst.KEY_SKIP_ADS, false)
    }

    // Getters with validation
    fun getScrollInterval(): Long {
        val interval = pref.getLong(SharedPrefConst.KEY_SCROLL_INTERVAL, MIN_INTERVAL)
        return if (isValidInterval(interval)) interval else MIN_INTERVAL
    }

    fun getReelLimit(): Long {
        val limit = pref.getLong(SharedPrefConst.KEY_REEL_LIMIT, MIN_REEL_LIMIT)
        return if (isValidReelLimit(limit)) limit else MIN_REEL_LIMIT
    }
}