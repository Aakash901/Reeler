package com.whatsapp.testing.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.whatsapp.testing.R
import com.whatsapp.testing.databinding.ActivityOnboardingBinding
import com.whatsapp.testing.model.OnboardingItem
import com.whatsapp.testing.ui.activity.adapter.OnboardingAdapter

// OnboardingActivity.kt
class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private val onboardingItems = listOf(
        OnboardingItem(
            R.drawable.onboarding_1, "Auto Scroll", "Let the app do the scrolling for you"
        ), OnboardingItem(
            R.drawable.ic_linkedin, "Skip Ads", "Skip sponsored content automatically"
        ), OnboardingItem(
            R.drawable.ic_instagram, "Track Stats", "Monitor your viewing habits"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViewPager()
        setupButtons()
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = OnboardingAdapter(onboardingItems)
        binding.dotsIndicator.attachTo(binding.viewPager)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.btnNext.text =
                    if (position == onboardingItems.size - 1) "Get Started" else "Next"
            }
        })
    }

    private fun setupButtons() {
        binding.btnNext.setOnClickListener {
            if (binding.viewPager.currentItem == onboardingItems.size - 1) {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            } else {
                binding.viewPager.currentItem++
            }
        }

        binding.btnSkip.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }
}


