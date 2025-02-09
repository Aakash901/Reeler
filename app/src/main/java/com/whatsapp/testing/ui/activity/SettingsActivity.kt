package com.whatsapp.testing.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.github.iielse.switchbutton.SwitchView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.whatsapp.testing.AppController.PreferenceManager
import com.whatsapp.testing.R
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
        setupViews()
        setupListeners()
        startAnimations()
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

    private fun setupViews() {
        binding.skipAdsSwitch.setOpened(prefManager.getSkipAds())
    }

    private fun setupListeners() {
        // Back button
        binding.backButton.setOnClickListener {
            finish()
        }

        // Skip Ads Switch
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
        binding.skipAdsSwitch.setOnStateChangedListener(object : SwitchView.OnStateChangedListener {
            override fun toggleToOn(view: SwitchView) {
                Toast.makeText(
                    this@SettingsActivity, "Will implemented in future", Toast.LENGTH_SHORT
                ).show()
//                prefManager.saveSkipAds(true)
//                view.isOpened = true
            }

            override fun toggleToOff(view: SwitchView) {
                Toast.makeText(
                    this@SettingsActivity, "Will implemented in future", Toast.LENGTH_SHORT
                ).show()
//
//                prefManager.saveSkipAds(false)
//                view.isOpened = false
            }
        })

        // Contact Us
        binding.contactLayout.setOnClickListener {
            openEmailClient()
        }

        // Follow Us
        binding.followLayout.setOnClickListener {
            showSocialMediaOptions()
        }

        // Privacy Policy
        binding.privacyLayout.setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java).apply {
                putExtra(
                    WebViewActivity.EXTRA_URL,
                    "https://sites.google.com/view/privacypolicyreeler/home"
                )
                putExtra(WebViewActivity.EXTRA_TITLE, "Privacy Policy")
            }
            startActivity(intent)
        }
    }

    private fun startAnimations() {
        binding.appBarLayout.animate().alpha(1f).translationY(0f).setDuration(500).start()

        binding.settingsCard.animate().alpha(1f).translationX(0f).setStartDelay(300)
            .setDuration(500).start()

        binding.moreCard.animate().alpha(1f).translationX(0f).setStartDelay(500).setDuration(500)
            .start()
    }

    private fun showSocialMediaOptions() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_social_media, null)
        bottomSheetDialog.setContentView(view)

        // Instagram click listener
        view.findViewById<LinearLayout>(R.id.instagramLayout).setOnClickListener {
            openSocialMedia(
                "instagram", "https://www.instagram.com/amezingapps?igsh=MWR4MG1ramh2MThqZg=="
            )
            bottomSheetDialog.dismiss()
        }

        // LinkedIn click listener
        view.findViewById<LinearLayout>(R.id.linkedinLayout).setOnClickListener {
            openSocialMedia(
                "linkedin",
                "https://www.linkedin.com/in/aakash-singh-1832631b4?utm_source=share&utm_campaign=share_via&utm_content=profile&utm_medium=android_app"
            )
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun openSocialMedia(platform: String, url: String) {
        try {
            val intent = when (platform) {
                "instagram" -> Intent(Intent.ACTION_VIEW).apply {
                    setPackage("com.instagram.android")
                    data = Uri.parse(url)
                }

                "linkedin" -> Intent(Intent.ACTION_VIEW).apply {
                    setPackage("com.linkedin.android")
                    data = Uri.parse(url)
                }

                else -> Intent(Intent.ACTION_VIEW, Uri.parse(url))
            }
            startActivity(intent)
        } catch (e: Exception) {
            // If app is not installed, open in browser
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(webIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open $platform!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openEmailClient() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:aakash.kr.singh0102.@gmail.com") // Replace with your email
            putExtra(Intent.EXTRA_SUBJECT, "Reeler App - Contact")
        }

        try {
            startActivity(Intent.createChooser(intent, "Send email using..."))
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found!", Toast.LENGTH_SHORT).show()
        }
    }
}