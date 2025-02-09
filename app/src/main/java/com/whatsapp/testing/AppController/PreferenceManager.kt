package com.whatsapp.testing.AppController


import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val pref: SharedPreferences
    private val editor: SharedPreferences.Editor

    init {
        pref = context.getSharedPreferences(SharedPrefConst.PREF_NAME, Context.MODE_PRIVATE)
        editor = pref.edit()
    }

    fun setFirstTimeLaunch(isFirstTime: Boolean) {
        editor.putBoolean(SharedPrefConst.IS_FIRST_TIME_LAUNCH, isFirstTime)
        editor.commit()
    }

    fun isFirstTimeLaunch(): Boolean {
        return pref.getBoolean(SharedPrefConst.IS_FIRST_TIME_LAUNCH, true)
    }

    // Added methods from HomeActivity
    fun saveScrollInterval(interval: Long) {
        editor.putLong(SharedPrefConst.KEY_SCROLL_INTERVAL, interval).apply()
    }

    fun saveReelLimit(limit: Long) {
        editor.putLong(SharedPrefConst.KEY_REEL_LIMIT, limit).apply()
    }

    fun saveSelectedApp(app: String) {
        editor.putString(SharedPrefConst.KEY_SELECTED_APP, app).apply()
    }

    fun saveSkipAds(skip: Boolean) {
        editor.putBoolean(SharedPrefConst.KEY_SKIP_ADS, skip).apply()
    }

    fun getScrollInterval() = pref.getLong(SharedPrefConst.KEY_SCROLL_INTERVAL, 5)
    fun getReelLimit() = pref.getLong(SharedPrefConst.KEY_REEL_LIMIT, 50)
    fun getSelectedApp() = pref.getString(SharedPrefConst.KEY_SELECTED_APP, "")
    fun getSkipAds() = pref.getBoolean(SharedPrefConst.KEY_SKIP_ADS, false)

    fun validateSettings(): Boolean {
        val interval = getScrollInterval()
        val reelLimit = getReelLimit()

        return !(interval < 3 || reelLimit < 5 || reelLimit > 5000)
    }
}