package com.whatsapp.testing.ui.activity

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.iielse.switchbutton.SwitchView
import com.whatsapp.testing.R
import com.whatsapp.testing.database.StatsDatabase
import com.whatsapp.testing.database.StatsRepository
import com.whatsapp.testing.databinding.ActivityMainBinding
import com.whatsapp.testing.service.AutoScrollService
import com.whatsapp.testing.ui.activity.adapter.DailyStatsAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val ACCESSIBILITY_PERMISSION_REQUEST_CODE = 100
    private lateinit var statsRepository: StatsRepository
    private lateinit var statsAdapter: DailyStatsAdapter
    private var statsUpdateJob: Job? = null
    private var isInForeground = false

    companion object {
        private const val PREFS_NAME = "AutoScrollPrefs"
        private const val KEY_SKIP_ADS = "skip_ads"
        private const val KEY_SCROLL_INTERVAL = "scrollInterval"
        private const val KEY_REEL_LIMIT = "reelLimit"
        private const val KEY_SELECTED_APP = "selected_app"
    }

    private val apps = listOf(
        "Instagram Reels", "YouTube Shorts", "LinkedIn Videos", "Snapchat Stories"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setGreenStatusBar()
        setupAppSelector()
        loadSavedSettings()
        setupRecyclerView()
        initializeDatabase()
        startStatsUpdates()
        setupClickListeners()
        setupAutoSave()
    }

    override fun onResume() {
        super.onResume()
        if (!isInForeground) {
            isInForeground = true
            lifecycleScope.launch {
                updateStatsDisplay()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        isInForeground = false
//        statsUpdateJob?.cancel()
    }

    private fun setupAutoSave() {
        binding.editTextInterval.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                saveSettings()
            }
        })

        binding.editTextReelLimit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                saveSettings()
            }
        })
    }

    private fun setupClickListeners() {

        binding.btnStartService.setOnClickListener {
            if (isAccessibilityServiceEnabled()) {
                startService()
            } else {
                requestAccessibilityPermission()
            }
        }
        binding.settingBtn.setOnClickListener {
            startActivity(Intent(this@HomeActivity, StatsActivity::class.java))
        }

        binding.btnStopService.setOnClickListener {
            stopService()
        }

        binding.skipAdsSwitch.setOnStateChangedListener(object : SwitchView.OnStateChangedListener {
            override fun toggleToOn(view: SwitchView) {
                savePreference(KEY_SKIP_ADS, true)
            }

            override fun toggleToOff(view: SwitchView) {
                savePreference(KEY_SKIP_ADS, false)
            }
        })
    }

    private fun setupAppSelector() {
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, apps)
        binding.appSelector.setAdapter(adapter)

        val savedApp =
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_SELECTED_APP, null)
        if (!savedApp.isNullOrEmpty()) {
            binding.appSelector.setText(savedApp, false)
        }

        binding.appSelector.setOnItemClickListener { _, _, position, _ ->
            val selectedApp = apps[position]
            savePreference(KEY_SELECTED_APP, selectedApp)

        }
    }

    private fun loadSavedSettings() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        binding.editTextInterval.setText(sharedPrefs.getLong(KEY_SCROLL_INTERVAL, 5).toString())
        binding.editTextReelLimit.setText(sharedPrefs.getLong(KEY_REEL_LIMIT, 50).toString())
        binding.skipAdsSwitch.setOpened(sharedPrefs.getBoolean(KEY_SKIP_ADS, false))
    }

    private fun setGreenStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.apply {
                statusBarColor = ContextCompat.getColor(this@HomeActivity, R.color.teal_700)
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    private fun initializeDatabase() {
        val dao = StatsDatabase.getDatabase(this).dailyStatsDao()
        statsRepository = StatsRepository(dao)
    }

    private fun startStatsUpdates() {
        lifecycleScope.launch {
            updateStatsDisplay()
        }
    }

    suspend fun updateStatsDisplay() = withContext(Dispatchers.IO) {
        try {
            val allStats = statsRepository.getAllStatsOrderedByDate()
            val todayStats = statsRepository.getTodayStats()
            val reelLimit =
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(KEY_REEL_LIMIT, 50)
            val selectedApp = binding.appSelector.text.toString()

            val totalWatched = when (selectedApp) {
                "Instagram Reels" -> todayStats.instagramReelsWatched
                "YouTube Shorts" -> todayStats.youtubeReelsWatched
                "LinkedIn Videos" -> todayStats.linkedInVideosWatched
                "Snapchat Stories" -> todayStats.snapchatStoriesWatched
                else -> 0
            }

            val remainingContent = reelLimit - totalWatched
            val contentType = when (selectedApp) {
                "Instagram Reels" -> "reels"
                "YouTube Shorts" -> "shorts"
                "LinkedIn Videos" -> "videos"
                "Snapchat Stories" -> "stories"
                else -> "content"
            }

            withContext(Dispatchers.Main) {
                statsAdapter.submitList(allStats)
                binding.remainingReelsText.text = "Remaining $contentType: $remainingContent"
                binding.btnStartService.isEnabled = remainingContent > 0
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in updateStatsDisplay: ${e.stackTraceToString()}")
        }
    }

    private fun setupRecyclerView() {
        statsAdapter = DailyStatsAdapter()
        binding.statsRecyclerView.apply {
            adapter = statsAdapter
            layoutManager = LinearLayoutManager(
                this@HomeActivity, LinearLayoutManager.HORIZONTAL, false
            )
        }
    }

    private fun saveSettings() {
        val intervalText = binding.editTextInterval.text.toString()
        val reelLimitText = binding.editTextReelLimit.text.toString()
        val interval = intervalText.toLongOrNull()
        val reelLimit = reelLimitText.toLongOrNull()

        when {
            interval == null || interval < 3 -> {
                showToast("Scroll interval must be at least 3 seconds")
                return
            }

            reelLimit == null || reelLimit < 5 || reelLimit > 5000 -> {
                showToast("Reel limit must be between 5 and 5,000")
                return
            }
        }

        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            if (interval != null) {
                putLong(KEY_SCROLL_INTERVAL, interval)
            }
            if (reelLimit != null) {
                putLong(KEY_REEL_LIMIT, reelLimit)
            }
            apply()
        }
        showToast("Settings saved successfully")
    }

    private fun startService() {
        if (!validateAppSelection()) return

        if (isAccessibilityServiceEnabled()) {
            when (binding.appSelector.text.toString()) {
                "Instagram Reels" -> openInstagramForScrolling()
                "YouTube Shorts" -> openYoutubeForScrolling()
                "Snapchat Stories" -> openSnapchatForScrolling()
                "LinkedIn Videos" -> openLinkedInForScrolling()
            }
        } else {
            requestAccessibilityPermission()
        }
    }

    private fun validateAppSelection(): Boolean {
        val selectedApp = binding.appSelector.text.toString()
        if (selectedApp.isEmpty() || selectedApp !in apps) {
            showToast("Please select an app")
            return false
        }
        return true
    }

    private fun openLinkedInForScrolling() {
        openApp("com.linkedin.android", "LinkedIn")
    }

    private fun openSnapchatForScrolling() {
        openApp("com.snapchat.android", "Snapchat")
    }

    private fun openYoutubeForScrolling() {
        openApp("com.google.android.youtube", "YouTube")
    }

    private fun openInstagramForScrolling() {
        openApp("com.instagram.android", "Instagram")
    }

    private fun openApp(packageName: String, appName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            startActivity(intent)
        } else {
            showToast("$appName app not found")
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager =
            getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        // Use component name for exact matching
        val serviceComponentName = ComponentName(packageName, AutoScrollService::class.java.name)
        return enabledServices.any {
            ComponentName.unflattenFromString(it.id)?.equals(serviceComponentName) ?: false
        }
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivityForResult(intent, ACCESSIBILITY_PERMISSION_REQUEST_CODE)
    }

    private fun stopService() {
        val autoScrollService = AutoScrollService.getCurrentInstance()
        autoScrollService?.stopScrollService()
        val intent = Intent(this, AutoScrollService::class.java)
        stopService(intent)
        AutoScrollService.instance = null  // Force clear the instance
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService()
        statsUpdateJob?.cancel()
    }

    private fun savePreference(key: String, value: String) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(key, value).apply()
    }

    private fun savePreference(key: String, value: Boolean) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}