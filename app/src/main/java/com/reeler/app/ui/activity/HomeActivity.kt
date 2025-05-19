package com.reeler.app.ui.activity

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.reeler.app.AppController.PreferenceManager
import com.reeler.app.R
import com.reeler.app.database.DailyStats
import com.reeler.app.database.StatsDatabase
import com.reeler.app.database.StatsRepository
import com.reeler.app.databinding.ActivityMainBinding
import com.reeler.app.service.AutoScrollService
import com.reeler.app.ui.activity.adapter.StatsChartPagerAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefManager: PreferenceManager
    private lateinit var statsRepository: StatsRepository
    private var statsUpdateJob: Job? = null
    private var isInForeground = false
    private lateinit var statsChartPagerAdapter: StatsChartPagerAdapter


    private val apps = listOf(
        "Instagram Reels", "YouTube Shorts", "LinkedIn Videos", "Snapchat Stories"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefManager = PreferenceManager(this)

        initializeViews()
        initializeDatabase()
        startStatsUpdates()
    }

    private fun initializeViews() {
        setupStatusBar()
        setupAppSelector()
        loadSavedSettings()
        setupChartViewPager()
        setupClickListeners()

    }

    private fun setupStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        binding.root.setPadding(0, getStatusBarHeight(), 0, 0)
    }

    private fun setupChartViewPager() {
        statsChartPagerAdapter = StatsChartPagerAdapter(this)
        binding.statsViewPager.apply {
            adapter = statsChartPagerAdapter
            orientation = ViewPager2.ORIENTATION_HORIZONTAL

            // Add offscreen page limit to keep adjacent pages ready
            offscreenPageLimit = 1

            // Add page transform animation
            setPageTransformer { page, position ->
                page.apply {
                    val pageWidth = width
                    when {
                        position < -1 -> { // Page is way off-screen to the left
                            alpha = 0f
                        }

                        position <= 1 -> { // Page is visible or adjacent
                            // Base scaling and alpha
                            alpha = 1 - abs(position)
                            scaleY = 0.85f + (1 - abs(position)) * 0.15f

                            // Add slight translation for smooth transition
                            translationX = -position * (pageWidth / 4)
                        }

                        else -> { // Page is way off-screen to the right
                            alpha = 0f
                        }
                    }
                }
            }
        }

        // Connect dots indicator
        binding.dotsIndicator.attachTo(binding.statsViewPager)
    }


    private fun setupClickListeners() {
        with(binding) {
            btnStartService.setOnClickListener {
                if (isAccessibilityServiceEnabled()) startService()
                else requestAccessibilityPermission()
            }

            fullGraph.setOnClickListener {
                startActivity(Intent(this@HomeActivity, StatsActivity::class.java))
            }

            settingBtn.setOnClickListener {
                startActivity(Intent(this@HomeActivity, SettingsActivity::class.java))
            }

            btnStopService.setOnClickListener { stopService() }

            btnSaveSettings.setOnClickListener { saveSettings() }


//            skipAdsSwitch.setOnStateChangedListener(object : SwitchView.OnStateChangedListener {
//                override fun toggleToOn(view: SwitchView) =
//                    prefManager.saveSkipAds(true)
//                override fun toggleToOff(view: SwitchView) =
//                    prefManager.saveSkipAds(false)
//            })
        }
    }

    private fun setupAppSelector() {
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, apps)
        binding.appSelector.apply {
            setAdapter(adapter)
            prefManager.getSelectedApp()?.let { savedApp ->
                if (savedApp.isNotEmpty()) setText(savedApp, false)
            }
            setOnItemClickListener { _, _, position, _ ->
                prefManager.saveSelectedApp(apps[position])
            }
        }
    }

    private fun loadSavedSettings() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val interval = prefManager.getScrollInterval()
                val reelLimit = prefManager.getReelLimit()
                val skipAds = prefManager.getSkipAds()

                withContext(Dispatchers.Main) {
                    binding.editTextInterval.setText(interval.toString())
                    binding.editTextReelLimit.setText(reelLimit.toString())
                    binding.skipAdsSwitch.setOpened(skipAds)
                }
            }
        }
    }

    private fun initializeDatabase() {
        val dao = StatsDatabase.getDatabase(this).dailyStatsDao()
        statsRepository = StatsRepository(dao)
    }

    private fun startStatsUpdates() {
        lifecycleScope.launch { updateStatsDisplay() }
    }

    private suspend fun updateStatsDisplay() = withContext(Dispatchers.IO) {
        try {
            val allStats = statsRepository.getAllStatsOrderedByDate()
            val todayStats = statsRepository.getTodayStats()
            val reelLimit = prefManager.getReelLimit()
            val selectedApp = binding.appSelector.text.toString()

            val totalWatched = getTotalWatchedForApp(selectedApp, todayStats)
            val remainingContent = reelLimit - totalWatched
            val contentType = getContentType(selectedApp)

            withContext(Dispatchers.Main) {
                // Check if we have any stats
                if (allStats.isEmpty()) {
                    // Show empty state
                    binding.emptyState.root.visibility = View.VISIBLE
                    binding.statsViewPager.visibility = View.GONE
                    binding.statsHeading.visibility = View.GONE
                    binding.fullGraph.visibility = View.GONE
                    binding.dotsIndicator.visibility = View.GONE
                } else {
                    // Show stats
                    binding.emptyState.root.visibility = View.GONE
                    binding.statsHeading.visibility = View.VISIBLE
//                    binding.fullGraph.visibility = View.VISIBLE
                    binding.statsViewPager.visibility = View.VISIBLE
                    binding.dotsIndicator.visibility = View.VISIBLE
                    statsChartPagerAdapter.updateAllFragmentsStats(allStats)
                }

                binding.remainingReelsText.text = "Remaining $contentType: $remainingContent"
                binding.btnStartService.isEnabled = remainingContent > 0
            }
        } catch (e: Exception) {
            Log.e("HomeActivity", "Error in updateStatsDisplay: ${e.message}")
        }
    }

    private fun getTotalWatchedForApp(selectedApp: String, todayStats: DailyStats) = when (selectedApp) {
            "Instagram Reels" -> todayStats.instagramReelsWatched
            "YouTube Shorts" -> todayStats.youtubeReelsWatched
            "LinkedIn Videos" -> todayStats.linkedInVideosWatched
            "Snapchat Stories" -> todayStats.snapchatStoriesWatched
            else -> 0
        }

    private fun getContentType(selectedApp: String) = when (selectedApp) {
        "Instagram Reels" -> "reels"
        "YouTube Shorts" -> "shorts"
        "LinkedIn Videos" -> "videos"
        "Snapchat Stories" -> "stories"
        else -> "content"
    }

    private fun saveSettings() {
        try {
            val intervalText = binding.editTextInterval.text.toString()
            val reelLimitText = binding.editTextReelLimit.text.toString()

            // Validation messages
            var hasError = false
            val errorMessages = mutableListOf<String>()

            // Validate interval
            val interval = intervalText.toLongOrNull()
            if (intervalText.isNotEmpty() && (interval == null || !prefManager.isValidInterval(
                    interval
                ))
            ) {
                errorMessages.add("Scroll interval must be at least ${PreferenceManager.MIN_INTERVAL} seconds")
                hasError = true
            }

            // Validate reel limit
            val reelLimit = reelLimitText.toLongOrNull()
            if (reelLimitText.isNotEmpty() && (reelLimit == null || !prefManager.isValidReelLimit(
                    reelLimit
                ))
            ) {
                errorMessages.add("Reel limit must be between ${PreferenceManager.MIN_REEL_LIMIT} and ${PreferenceManager.MAX_REEL_LIMIT}")
                hasError = true
            }

            // If there are validation errors, show them
            if (hasError) {
                errorMessages.forEach { showToast(it) }
                return
            }

            // Save valid values
            var settingsUpdated = false
            if (interval != null && intervalText.isNotEmpty()) {
                if (prefManager.saveScrollInterval(interval)) {
                    settingsUpdated = true
                }
            }
            if (reelLimit != null && reelLimitText.isNotEmpty()) {
                if (prefManager.saveReelLimit(reelLimit)) {
                    settingsUpdated = true
                }
            }

            // Show success message only if something was actually updated
            if (settingsUpdated) {
                showToast("Settings saved successfully")
            }

        } catch (e: Exception) {
            Log.e("HomeActivity", "Error saving settings: ${e.message}")
            showToast("Error saving settings")
        }
    }

    private fun startService() {
        // First check if app is selected
        if (!validateAppSelection()) return

        // Then validate all settings
        if (!prefManager.areSettingsValid()) {
            // Show all validation errors
            prefManager.getSettingsValidationErrors().forEach { error ->
                showToast(error)
            }
            return
        }

        // Check accessibility service
        if (isAccessibilityServiceEnabled()) {
            // Validate saved settings one final time
            val interval = prefManager.getScrollInterval()
            val reelLimit = prefManager.getReelLimit()

            when {
                interval < PreferenceManager.MIN_INTERVAL -> {
                    showToast("Invalid scroll interval. Please update settings.")
                    return
                }

                reelLimit !in PreferenceManager.MIN_REEL_LIMIT..PreferenceManager.MAX_REEL_LIMIT -> {
                    showToast("Invalid reel limit. Please update settings.")
                    return
                }

                else -> {
                    // All validations passed, start the service
                    when (binding.appSelector.text.toString()) {
                        "Instagram Reels" -> openApp("com.instagram.android", "Instagram")
                        "YouTube Shorts" -> openApp("com.google.android.youtube", "YouTube")
                        "Snapchat Stories" -> openApp("com.snapchat.android", "Snapchat")
                        "LinkedIn Videos" -> openApp("com.linkedin.android", "LinkedIn")
                    }
                }
            }
        } else {
            requestAccessibilityPermission()
        }
    }

    private fun validateAppSelection(): Boolean {
        val selectedApp = binding.appSelector.text.toString()
        return if (selectedApp.isEmpty() || selectedApp !in apps) {
            showToast("Please select an app")
            false
        } else true
    }

    private fun openApp(packageName: String, appName: String) {
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            startActivity(it)
        } ?: showToast("$appName app not found")
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager =
            getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        val serviceComponentName = ComponentName(packageName, AutoScrollService::class.java.name)
        return enabledServices.any {
            ComponentName.unflattenFromString(it.id)?.equals(serviceComponentName) ?: false
        }
    }

    private fun requestAccessibilityPermission() {
        startActivityForResult(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), 100
        )
    }

    private fun stopService() {
        AutoScrollService.getCurrentInstance()?.stopScrollService()
        stopService(Intent(this, AutoScrollService::class.java))
        AutoScrollService.instance = null
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        if (!isInForeground) {
            isInForeground = true
            lifecycleScope.launch { updateStatsDisplay() }
        }
    }

    override fun onPause() {
        super.onPause()
        isInForeground = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService()
        statsUpdateJob?.cancel()
    }
}