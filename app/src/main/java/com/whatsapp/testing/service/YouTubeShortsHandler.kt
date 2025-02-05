package com.whatsapp.testing.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.whatsapp.testing.database.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class YouTubeShortsHandler(
    private val service: AccessibilityService,
    private val statsRepository: StatsRepository,
    private val serviceScope: CoroutineScope,
    private val currentScrollInterval: Long
) {
    companion object {
        private const val TAG = "AutoScrollService"
        private const val PREFS_NAME = "AutoScrollPrefs"
        private const val KEY_SKIP_ADS = "skip_ads"
        private const val KEY_REEL_LIMIT = "reelLimit"
    }

    fun handleYouTubeNavigation() {
        Log.d(TAG, "🚀 YouTube navigation handler started")
        serviceScope.launch {
            try {
                delay(2000) // Initial delay for app to load

                var rootNode: AccessibilityNodeInfo? = null

                var attempts = 0

                while (rootNode == null && attempts < 5) {
                    rootNode = service.rootInActiveWindow
                    Log.d(
                        TAG,
                        "Attempt ${attempts + 1}: Root node is ${if (rootNode == null) "null" else "found"}"
                    )
                    if (rootNode == null) {
                        delay(1000)
                        attempts++
                    }
                }

                if (rootNode == null) {
                    Log.e(TAG, "Failed to get root node after 5 attempts")
                    return@launch
                }

                // Find and click the Shorts button
                findShortsButton(rootNode)?.let { shortsButton ->
                    Log.d(TAG, "Found Shorts button, clicking...")
                    shortsButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    delay(1000) // Wait for click to register
                    val newRoot = service.rootInActiveWindow
                    if (newRoot != null) {
                        initializeScrolling(newRoot)
                    }
                } ?: Log.e(TAG, "Shorts button not found")

                Log.d(TAG, "=== End of YouTube Screen Hierarchy ===")

            } catch (e: Exception) {
                Log.e(TAG, "Error in handleYouTubeNavigation: ${e.stackTraceToString()}")
            }
        }
    }

    private fun findShortsButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Check if current node is the Shorts button
        if (node.className?.toString() == "android.widget.Button" && node.contentDescription?.toString() == "Shorts" && node.isClickable && node.isVisibleToUser && node.isEnabled) {
            Log.d(TAG, "Found Shorts button!")
            return node
        }

        // Recursively search children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findShortsButton(child)?.let { return it }
            }
        }

        return null
    }

    private suspend fun scrollShorts(node: AccessibilityNodeInfo, isSponsored: Boolean) {
        try {
            var recyclerView: AccessibilityNodeInfo? = null

            // First try to find by exact ID since we know this works
            recyclerView = findNodeByViewIdAndText(
                node, "com.google.android.youtube:id/reel_recycler", null
            )

            val foundRecyclerView = recyclerView
            if (foundRecyclerView != null) {
                Log.d(TAG, "Found RecyclerView, attempting vertical scroll...")

                // Try gesture scroll
                val rect = android.graphics.Rect()
                foundRecyclerView.getBoundsInScreen(rect)

                val gestureBuilder = android.accessibilityservice.GestureDescription.Builder()
                val path = android.graphics.Path()

                // Start from bottom center and move to top
                val startY = (rect.bottom - 100).toFloat()
                val endY = (rect.top + 100).toFloat()
                val centerX = rect.centerX().toFloat()

                path.moveTo(centerX, startY)
                path.lineTo(centerX, endY)

                val gesture = gestureBuilder.addStroke(
                        android.accessibilityservice.GestureDescription.StrokeDescription(
                            path, 0, 300 // Duration in milliseconds
                        )
                    ).build()

                val scrollSuccess = service.dispatchGesture(gesture, null, null)
                Log.d(TAG, "Gesture scroll result: $scrollSuccess")

                if (scrollSuccess) {
                    // Update stats for YouTube specifically
                    statsRepository.updateYoutubeStats(
                        reelsScrolled = 1,
                        adSkipped = isSponsored,
                        scrollInterval = currentScrollInterval
                    )
                    Log.d(TAG, "Successfully scrolled and updated YouTube stats")
                } else {
                    Log.e(TAG, "Failed to scroll shorts")
                }
            } else {
                Log.e(TAG, "Could not find RecyclerView")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error scrolling shorts: ${e.stackTraceToString()}")
        }
    }

    private fun checkForSponsoredReel(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        // Function to check if node is an ad by checking for "stars" and "ad" keywords
        fun isAdViewGroup(node: AccessibilityNodeInfo): Boolean {
            val description = node.contentDescription?.toString()?.lowercase() ?: ""
            val isMatch =
                node.className?.toString() == "android.view.ViewGroup" && description.contains("stars") && description.contains(
                    "ad"
                )

            if (isMatch) {
                Log.d(
                    TAG, """
                Found Ad ViewGroup:
                Class: ${node.className}
                Description: ${node.contentDescription}
                Clickable: ${node.isClickable}
                Visible: ${node.isVisibleToUser}
                Enabled: ${node.isEnabled}
            """.trimIndent()
                )
            }
            return isMatch
        }

        // Function to check for action menu image view
        fun isActionMenuNode(node: AccessibilityNodeInfo): Boolean {
            return node.className?.toString() == "android.widget.ImageView" && node.contentDescription?.toString() == "Action menu" && node.isClickable && !node.isVisibleToUser && node.isEnabled
        }

        // Recursive function to search for either type of sponsored indicator
        fun findSponsoredContent(node: AccessibilityNodeInfo): Boolean {
            // Check current node
            if (isActionMenuNode(node) || isAdViewGroup(node)) {
                Log.d(TAG, "Found sponsored content indicator")
                return true
            }

            // Search children
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    if (findSponsoredContent(child)) {
                        return true
                    }
                }
            }
            return false
        }

        val isSponsored = findSponsoredContent(node)
        if (isSponsored) {
            Log.d(TAG, "Sponsored content detected, will scroll immediately")
        } else {
            Log.d(TAG, "No sponsored content detected")
        }

        return isSponsored
    }

    private suspend fun initializeScrolling(rootNode: AccessibilityNodeInfo) {
        withContext(Dispatchers.IO) {
            val sharedPrefs = service.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val scrollInterval = sharedPrefs.getLong("scrollInterval", 5) * 1000
            val reelLimit = sharedPrefs.getLong(KEY_REEL_LIMIT, 50)
            val todayStats = statsRepository.getTodayStats()

            if (todayStats.youtubeReelsWatched + todayStats.instagramReelsWatched >= reelLimit) {
                Log.d(TAG, "Already at daily limit (${todayStats.youtubeReelsWatched + todayStats.instagramReelsWatched}/$reelLimit)")
                withContext(Dispatchers.Main) {
                    Toast.makeText(service, "Daily shorts limit reached!", Toast.LENGTH_LONG).show()
                }
                service.stopSelf()
                return@withContext
            }

            while (serviceScope.isActive) {
                try {
                    val currentRoot = service.rootInActiveWindow
                    if (currentRoot == null) {
                        Log.e(TAG, "Root node is null")
                        delay(1000)
                        continue
                    }
                    val skipAdsEnabled = sharedPrefs.getBoolean(KEY_SKIP_ADS, false)
                    val isSponsored = checkForSponsoredReel(currentRoot)

                    Log.d(
                        TAG,
                        "Checking short - Skip Ads: $skipAdsEnabled, Is Sponsored: $isSponsored"
                    )

                    if (skipAdsEnabled && isSponsored) {
                        Log.d(TAG, "Sponsored short found - immediate scroll")
                        scrollShorts(currentRoot, true)
                        delay(500) // Small delay before next scroll
                    } else {
                        Log.d(TAG, "Regular short - scrolling with delay")
                        scrollShorts(currentRoot, false)
                        delay(scrollInterval)
                    }
                    val updatedStats = statsRepository.getTodayStats()
                    if (updatedStats.youtubeReelsWatched + updatedStats.instagramReelsWatched >= reelLimit) {
                        Log.d(TAG, "Limit reached after scroll")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(service, "Daily shorts limit reached!", Toast.LENGTH_LONG).show()
                        }
                        service.stopSelf()
                        break
                    }


                    delay(500) // Small delay for UI update

                } catch (e: Exception) {
                    Log.e(TAG, "Error in scroll cycle: ${e.message}")
                    delay(1000)
                }
            }
        }
    }

//    private fun checkForSponsoredShort(node: AccessibilityNodeInfo): Boolean {
//        try {
//            // Look for common YouTube ad indicators
//            val adIndicators = listOf("Ad", "Advertisement", "Sponsored")
//
//            for (indicator in adIndicators) {
//                val sponsoredNodes = findNodesWithDescription(node, indicator)
//                if (sponsoredNodes.isNotEmpty()) {
//                    Log.d(TAG, "Found sponsored content: $indicator")
//                    return true
//                }
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Error checking for sponsored short: ${e.message}")
//        }
//        return false
//    }


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

}