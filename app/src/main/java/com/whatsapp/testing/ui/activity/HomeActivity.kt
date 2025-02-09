package com.whatsapp.testing.ui.activity

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
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
import com.whatsapp.testing.AppController.PreferenceManager
import com.whatsapp.testing.R
import com.whatsapp.testing.database.DailyStats
import com.whatsapp.testing.database.StatsDatabase
import com.whatsapp.testing.database.StatsRepository
import com.whatsapp.testing.databinding.ActivityMainBinding
import com.whatsapp.testing.service.AutoScrollService
import com.whatsapp.testing.ui.activity.adapter.StatsChartPagerAdapter
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
        setupAutoSave()
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

    private fun setupAutoSave() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                saveSettings()
            }
        }

        binding.editTextInterval.addTextChangedListener(textWatcher)
        binding.editTextReelLimit.addTextChangedListener(textWatcher)
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
        with(binding) {
            editTextInterval.setText(prefManager.getScrollInterval().toString())
            editTextReelLimit.setText(prefManager.getReelLimit().toString())
            skipAdsSwitch.setOpened(prefManager.getSkipAds())
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
                    binding.fullGraph.visibility = View.VISIBLE
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

    private fun getTotalWatchedForApp(selectedApp: String, todayStats: DailyStats) =
        when (selectedApp) {
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
        val interval = binding.editTextInterval.text.toString().toLongOrNull()
        val reelLimit = binding.editTextReelLimit.text.toString().toLongOrNull()

        when {
            interval == null || interval < 3 -> {
                showToast("Scroll interval must be at least 3 seconds")
                return
            }

            reelLimit == null || reelLimit < 5 || reelLimit > 5000 -> {
                showToast("Reel limit must be between 5 and 5,000")
                return
            }

            else -> {
                prefManager.apply {
                    saveScrollInterval(interval)
                    saveReelLimit(reelLimit)
                }
                showToast("Settings saved successfully")
            }
        }
    }

    private fun startService() {
        if (!validateAppSelection()) return

        if (isAccessibilityServiceEnabled()) {
            when (binding.appSelector.text.toString()) {
                "Instagram Reels" -> openApp("com.instagram.android", "Instagram")
                "YouTube Shorts" -> openApp("com.google.android.youtube", "YouTube")
                "Snapchat Stories" -> openApp("com.snapchat.android", "Snapchat")
                "LinkedIn Videos" -> openApp("com.linkedin.android", "LinkedIn")
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
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            100
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