package com.whatsapp.testing.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.whatsapp.testing.database.StatsDatabase
import com.whatsapp.testing.database.StatsRepository
import com.whatsapp.testing.ui.activity.HomeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AutoScrollService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var statsRepository: StatsRepository
    private var currentScrollInterval: Long = 5000 // Default 5 seconds
    private lateinit var youTubeShortsHandler: YouTubeShortsHandler
    private lateinit var linkedInHandler: LinkedInHandler
    private lateinit var snapchatHandler: SnapchatHandler

    companion object {
        private const val TAG = "AutoScrollService"
        private const val KEY_SKIP_ADS = "skip_ads"
        private const val PREFS_NAME = "AutoScrollPrefs"
        private const val KEY_REEL_LIMIT = "reelLimit"


        @JvmStatic
        var instance: AutoScrollService? = null

        @JvmStatic



        fun getCurrentInstance(): AutoScrollService? = instance

    }

    fun stopScrollService() {
        try {
            instance?.let { service ->
                service.disableSelf()
                instance = null
                Log.e(TAG, " stopping service: ")

            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping service: ${e.message}")
        }
    }
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Service Connected - Initializing components")

    }
    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "Service Created - Initializing components")

        try {
            val dao = StatsDatabase.getDatabase(this).dailyStatsDao()
            statsRepository = StatsRepository(dao)
            youTubeShortsHandler = YouTubeShortsHandler(this, statsRepository, serviceScope, currentScrollInterval)
            linkedInHandler = LinkedInHandler(this, statsRepository, serviceScope, currentScrollInterval)
            snapchatHandler = SnapchatHandler(this, statsRepository, serviceScope, currentScrollInterval)
            Log.d(TAG, "Service initialization completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing service: ${e.stackTraceToString()}")
        }


    }


    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.d(TAG, "Accessibility Event Received: Type=${event.eventType}, Package=${event.packageName}")

        when (event.packageName) {
            "com.instagram.android" -> {
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    Log.d(TAG, "Starting Instagram navigation handler")
                    handleInstagramNavigation()
                }
            }
            "com.google.android.youtube" -> {
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    Log.d(TAG, "Starting YouTube navigation handler")
                    youTubeShortsHandler.handleYouTubeNavigation()
                } else {
                    Log.d(TAG, "Received YouTube event but not window state change: ${event.eventType}")
                }
            }
            "com.linkedin.android" -> {
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    Log.d(TAG, "Starting LinkedIn navigation handler")
                    linkedInHandler.handleLinkedInNavigation()
                }
            }"com.snapchat.android" -> {
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    Log.d(TAG, "Starting Snapchat navigation handler")
                    snapchatHandler.handleSnapchatNavigation()
                }
            }
            else -> {
                Log.d(TAG, "Unhandled package: ${event.packageName}, Event type: ${event.eventType}")
            }
        }
    }
    private fun handleInstagramNavigation() {
        serviceScope.launch {
            try {
                val rootNode = rootInActiveWindow ?: return@launch
                // Find and click reels button
                findReelsButton(rootNode)?.let { reelsButton ->
                    Log.d(TAG, "Found Reels button, attempting to click...")
                    reelsButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)

                    // Wait for reels screen to load
                    delay(currentScrollInterval)

                    // Log the screen hierarchy after loading
                    withContext(Dispatchers.Default) {
                        rootInActiveWindow?.let { newRoot ->
                            Log.d(TAG, "=== Screen Hierarchy After Clicking Reels ===")
                            logNodeHierarchy(newRoot, 0)
                            Log.d(TAG, "=== End of Screen Hierarchy ===")

                            // Initialize scrolling
                            initializeScrolling(newRoot)
                        }
                    }
                } ?: Log.e(TAG, "Reels button not found")

            } catch (e: Exception) {
                Log.e(TAG, "Error in handleInstagramNavigation: ${e.message}", e)
            }
        }
    }


    private suspend fun initializeScrolling(rootNode: AccessibilityNodeInfo) {
        withContext(Dispatchers.IO) {
            val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val scrollInterval = sharedPrefs.getLong("scrollInterval", 5) * 1000
            val reelLimit = sharedPrefs.getLong("reelLimit", 50)
            currentScrollInterval = scrollInterval
            val todayStats = statsRepository.getTodayStats()
            if (todayStats.reelsWatched >= reelLimit) {
                Log.d(TAG, "Already at daily limit (${todayStats.reelsWatched}/$reelLimit)")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AutoScrollService,
                        "Daily reel limit reached!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                stopSelf()
                return@withContext
            }

            while (isActive) {
                try {
                    // Get fresh root node for each check
                    val currentRoot = rootInActiveWindow
                    if (currentRoot == null) {
                        Log.e(TAG, "Root node is null")
                        delay(1000)
                        continue
                    }

                    val skipAdsEnabled = sharedPrefs.getBoolean(KEY_SKIP_ADS, false)
                    val isSponsored = checkForSponsoredReel(currentRoot)

                    Log.d(
                        TAG,
                        "Checking reel - Skip Ads: $skipAdsEnabled, Is Sponsored: $isSponsored"
                    )

                    if (skipAdsEnabled && isSponsored) {
                        Log.d(TAG, "Sponsored reel found - immediate scroll")
                        scrollReels(currentRoot)
                    } else {
                        Log.d(TAG, "Regular reel - scrolling with delay")
                        scrollReels(currentRoot)
                        delay(scrollInterval)
                    }

                    // Small delay to ensure UI updates
                    delay(500)

                } catch (e: Exception) {
                    Log.e(TAG, "Error in scroll cycle: ${e.message}")
                    delay(1000)
                }
            }
        }
    }

    private fun checkForSponsoredReel(node: AccessibilityNodeInfo): Boolean {
        try {
            // First find the video container
            val videoContainer = findNodeByViewIdAndText(
                node,
                "com.instagram.android:id/clips_video_container",
                null
            )

            videoContainer?.let { container ->
                val description = container.contentDescription?.toString() ?: ""
                if (description.startsWith("Sponsored")) {
                    Log.d(TAG, "Found sponsored reel: $description")
                    return true
                } else {
                    Log.d(TAG, "Current reel description: $description")
                }
            } ?: Log.d(TAG, "Video container not found")

            // Additional check for any node containing "Sponsored"
            val sponsoredNodes = findNodesWithDescription(node, "Sponsored")
            if (sponsoredNodes.isNotEmpty()) {
                Log.d(TAG, "Found sponsored content through alternate check")
                return true
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking for sponsored reel: ${e.message}")
        }
        return false
    }

    private fun findNodesWithDescription(
        node: AccessibilityNodeInfo,
        searchText: String,
        results: MutableList<AccessibilityNodeInfo> = mutableListOf()
    ): List<AccessibilityNodeInfo> {
        try {
            val description = node.contentDescription?.toString() ?: ""
            if (description.contains(searchText, ignoreCase = true)) {
                results.add(node)
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    findNodesWithDescription(child, searchText, results)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in findNodesWithDescription: ${e.message}")
        }
        return results
    }

    private fun findReelsButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.className == "android.widget.FrameLayout" && node.contentDescription == "Reels" && node.viewIdResourceName == "com.instagram.android:id/clips_tab" && node.isClickable && node.isVisibleToUser && node.isEnabled) {
            return node
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findReelsButton(child)?.let { return it }
            }
        }
        return null
    }

    private suspend fun scrollReels(node: AccessibilityNodeInfo) {
        try {
            val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val reelLimit = sharedPrefs.getLong(KEY_REEL_LIMIT, 50)
            val todayStats = statsRepository.getTodayStats()

            // Check if limit reached
            if (todayStats.reelsWatched >= reelLimit) {
                Log.d(TAG, "Daily limit reached: ${todayStats.reelsWatched}/$reelLimit reels")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AutoScrollService,
                        "Daily reel limit reached!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                stopSelf()
                return
            }

            val remainingReels = reelLimit - todayStats.reelsWatched
            Log.d(TAG, "Remaining reels for today: $remainingReels")

            val isSponsored = checkForSponsoredReel(node)
            var scrollSuccessful = false

            val viewPager = findNodeByViewIdAndText(
                node, "com.instagram.android:id/clips_viewer_view_pager", null
            )

            viewPager?.let { vp ->
                // Try finding RecyclerView as direct child
                for (i in 0 until vp.childCount) {
                    val child = vp.getChild(i)
                    if (child?.className == "androidx.recyclerview.widget.RecyclerView") {
                        child.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                        scrollSuccessful = true
                        break
                    }
                }

                // If not found as direct child, try deeper search
                if (!scrollSuccessful) {
                    findRecyclerViewInHierarchy(vp)?.let { recyclerView ->
                        recyclerView.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                        scrollSuccessful = true
                    }
                }

                if (scrollSuccessful) {
                    val skipAdsEnabled = sharedPrefs.getBoolean(KEY_SKIP_ADS, false)

                    // Only count as ad skipped if the feature is enabled
                    val adSkipped = isSponsored && skipAdsEnabled
                    Log.d(
                        TAG,
                        "Updating stats - Reel scrolled, isSponsored: $isSponsored, skipAdsEnabled: $skipAdsEnabled"
                    )
                    statsRepository.updateInstagramStats(
                        reelsScrolled = 1,
                        adSkipped = adSkipped,
                        scrollInterval = currentScrollInterval
                    )
                    // Log remaining reels
                    // Check if this scroll hit the limit
                    if (todayStats.reelsWatched + 1 >= reelLimit) {
                        Log.d(TAG, "Limit reached after this scroll")
                        withContext(Dispatchers.Main) {
                            navigateToMainActivity()
                            Toast.makeText(
                                this@AutoScrollService,
                                "Daily reel limit reached!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        stopSelf()
                    } else {
                        Log.d(TAG, "Limit reached after this scroll")
                    }
                } else {
                    Log.e(TAG, "RecyclerView not found in ViewPager hierarchy")
                }
            } ?: Log.e(TAG, "ViewPager not found")

        } catch (e: Exception) {
            Log.e(TAG, "Error scrolling reels: ${e.message}")
        }
    }
    private fun navigateToMainActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }
    private fun findRecyclerViewInHierarchy(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.className == "androidx.recyclerview.widget.RecyclerView") {
            return node
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findRecyclerViewInHierarchy(child)?.let { return it }
            }
        }
        return null
    }

    private fun findNodeByViewIdAndText(
        node: AccessibilityNodeInfo, viewId: String, text: String?
    ): AccessibilityNodeInfo? {
        if (node.viewIdResourceName == viewId && (text == null || node.text?.contains(text) == true)) {
            return node
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findNodeByViewIdAndText(child, viewId, text)?.let { return it }
            }
        }
        return null
    }

    private fun logNodeHierarchy(node: AccessibilityNodeInfo, depth: Int) {
        if (node.className == null) return

        val prefix = " ".repeat(depth * 2)
        val text = node.text ?: "null"
        val contentDescription = node.contentDescription ?: "null"
        val viewId = node.viewIdResourceName ?: "null"
        val className = node.className.toString()
        val clickable = node.isClickable
        val visible = node.isVisibleToUser
        val enabled = node.isEnabled

        val nodeDetails = """
        $prefix Class Name: $className
        $prefix Text: $text
        $prefix Content Description: $contentDescription
        $prefix View ID: $viewId
        $prefix Clickable: $clickable
        $prefix Visible: $visible
        $prefix Enabled: $enabled
    """.trimIndent()

        Log.d(TAG, nodeDetails)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                logNodeHierarchy(child, depth + 1)
            }
        }
    }


    override fun onInterrupt() {
        Log.d(TAG, "Service Interrupted")
        serviceScope.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        serviceScope.cancel()
        Log.d(TAG, "Service Destroyed")
    }
}