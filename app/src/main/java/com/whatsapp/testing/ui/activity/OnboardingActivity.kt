package com.whatsapp.testing.ui.activity

import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.TextAppearanceSpan
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewpager2.widget.ViewPager2
import com.whatsapp.testing.AppController.PreferenceManager
import com.whatsapp.testing.R
import com.whatsapp.testing.databinding.ActivityOnboardingBinding
import com.whatsapp.testing.model.OnboardingContent
import com.whatsapp.testing.model.OnboardingItem
import com.whatsapp.testing.ui.activity.adapter.OnboardingAdapter

// OnboardingActivity.kt
class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var preferenceManager: PreferenceManager


    private val onboardingItems = listOf(
        OnboardingContent(
            R.drawable.onboarding_three,
            "Sit back",
            "and Relax",
            "Are you lazy, Let us scroll for you",
            "Next"
        ),
        OnboardingContent(
            R.drawable.onboarding_third,
            "Control",
            "your content",
            "Decide and choose the app and let us scroll for you",
            "Next"
        ),
        OnboardingContent(
            R.drawable.onboardig_one,
            "Track",
            "your activity",
            "See how many reels you watch everyday",
            "Done"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager = PreferenceManager(this)
//         If not first time, go directly to HomeActivity
        if (!preferenceManager.isFirstTimeLaunch()) {
            launchHomeScreen()
            finish()
            return
        }

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupStatusBar()
        setupViewPager()
        setupButtons()
        updateContent(0)
    }

    private fun updateContent(position: Int) {
        val content = onboardingItems[position]
        binding.apply {
            titleOne.text = content.titleOne
            titleTwo.text = content.titleTwo
            subTitle.text = content.subtitle
            btnNext.text = content.buttonText
        }
    }

    private fun launchHomeScreen() {
        preferenceManager.setFirstTimeLaunch(false)
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun setupStatusBar() {
        // Make status bar transparent
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)

        // Optional: Make status bar icons dark/light based on your theme
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        // Add system bar padding to root layout
        binding.root.setPadding(0, getStatusBarHeight(), 0, 0)
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = OnboardingAdapter(onboardingItems.map {
            OnboardingItem(it.imageRes)
        })
        binding.dotsIndicator.attachTo(binding.viewPager)

        // Add page transformer for 3D effect
        binding.viewPager.setPageTransformer { page, position ->
            page.apply {
                val r = 1 - kotlin.math.abs(position)
                page.scaleY = 0.85f + r * 0.15f

                // 3D rotation effect
                page.cameraDistance = 20000f
                page.rotationY = position * -30

                // Fade effect
                page.alpha = 0.25f + (1 - kotlin.math.abs(position)) * 0.75f

                // Elevation change
                page.translationZ = -kotlin.math.abs(position) * 30f
            }
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateContent(position)
            }
        })
    }

    private fun setupButtons() {
        binding.btnNext.setOnClickListener {
            if (binding.viewPager.currentItem == onboardingItems.size - 1) {
                launchHomeScreen()
            } else {
                binding.viewPager.currentItem++
            }
        }

        // Setup privacy policy text with gradient and clickable spans
        setupPrivacyPolicyText()
    }

    private fun setupPrivacyPolicyText() {
        val fullText = "By continuing I accept the Terms of Service and Privacy Policy"
        val spannableString = SpannableString(fullText)

        // Define the gradient shader for clickable text
        val textShader = LinearGradient(
            0f, 0f, 0f, binding.privacyPolicy.textSize,
            intArrayOf(
                Color.parseColor("#FF4B6C"),  // Start color
                Color.parseColor("#FFB344")   // End color
            ),
            null,
            Shader.TileMode.CLAMP
        )

        // Create gradient spans
        val gradientSpan1 = object : TextAppearanceSpan(this, android.R.style.TextAppearance) {
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.shader = textShader
            }
        }

        val gradientSpan2 = object : TextAppearanceSpan(this, android.R.style.TextAppearance) {
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.shader = textShader
            }
        }

        // Find the start and end indices for the clickable terms
        val tosStart = fullText.indexOf("Terms of Service")
        val tosEnd = tosStart + "Terms of Service".length
        val ppStart = fullText.indexOf("Privacy Policy")
        val ppEnd = ppStart + "Privacy Policy".length

        // Apply spans
        spannableString.setSpan(
            gradientSpan1,
            tosStart, tosEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannableString.setSpan(
            gradientSpan2,
            ppStart, ppEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Set light black color for regular text
        spannableString.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.text_light_black)),
            0, fullText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Make terms clickable
        spannableString.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // Open Terms of Service URL
                    openUrl("https://sites.google.com/view/privacypolicyreeler/home")
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false  // Remove underline
                }
            },
            tosStart, tosEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannableString.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // Open Privacy Policy URL
                    openUrl("https://sites.google.com/view/privacypolicyreeler/home")
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                }
            },
            ppStart, ppEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Set the text and make it clickable
        binding.privacyPolicy.apply {
            text = spannableString
            movementMethod = LinkMovementMethod.getInstance()  // Enable clicking
            highlightColor = Color.TRANSPARENT
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            // Handle error opening URL
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }

}


