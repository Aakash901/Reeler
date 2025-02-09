package com.whatsapp.testing.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.github.iielse.switchbutton.SwitchView
import com.whatsapp.testing.AppController.PreferenceManager
import com.whatsapp.testing.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    private lateinit var prefManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefManager = PreferenceManager(this)
        setupStatusBar()
        binding.backButton.setOnClickListener {
            finish() // Closes the activity
        }

        binding.appBarLayout.animate().alpha(1f).translationY(0f).setDuration(500).start()

        // Animate First Card
        binding.settingsCard.animate().alpha(1f).translationX(0f).setStartDelay(300)
            .setDuration(500).start()

        // Animate Second Card
        binding.moreCard.animate().alpha(1f).translationX(0f).setStartDelay(500).setDuration(500)
            .start()

        binding.skipAdsSwitch.setOpened(prefManager.getSkipAds())
        binding.skipAdsSwitch.setOnStateChangedListener(object : SwitchView.OnStateChangedListener {
            override fun toggleToOn(view: SwitchView) {
                prefManager.saveSkipAds(true)
                view.isOpened = true
            }

            override fun toggleToOff(view: SwitchView) {
                prefManager.saveSkipAds(false)
                view.isOpened = false
            }
        })


    }

    private fun setupStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        binding.root.setPadding(0, getStatusBarHeight(), 0, 0)
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }
}