package com.reeler.app.service


import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.reeler.app.AppController.SharedPrefConst
import com.reeler.app.database.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LinkedInHandler(
    private val service: AccessibilityService,
    private val statsRepository: StatsRepository,
    private val serviceScope: CoroutineScope,
    private val currentScrollInterval: Long
) {
    companion object {
        private const val TAG = "LinkedInHandler"
    }

    fun  handleLinkedInNavigation() {
        Log.d(TAG, "🚀 Starting LinkedIn navigation")
        serviceScope.launch {
            try {
                // First click the video tab
                var attempts = 0
                var maxAttempts = 3
                var clickSuccess = false

                while (!clickSuccess && attempts < maxAttempts) {
                    attempts++
                    Log.d(TAG, "Attempt $attempts to find and click video tab...")

                    val rootNode = service.rootInActiveWindow
                    if (rootNode != null) {
                        val videoTab = findAndClickVideoTab(rootNode)
                        if (videoTab != null) {
                            clickSuccess = videoTab.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            if (clickSuccess) {
                                Log.d(TAG, "✅ Successfully clicked video tab!")
                                break
                            }
                        }
                    }
                    delay(1000)
                }

                if (!clickSuccess) {
                    Log.e(TAG, "Failed to access video feed")
                    return@launch
                }

                // Wait for video feed to load
                delay(2000)

                // Start continuous scrolling
                while (true) {
                    val currentRoot = service.rootInActiveWindow
                    if (currentRoot == null) {
                        Log.e(TAG, "Root node is null")
                        delay(1000)
                        continue
                    }

                    // Check if we should skip this video (if it's sponsored and skip_ads is enabled)
                    val skipAds = service.getSharedPreferences(SharedPrefConst.PREF_NAME, Context.MODE_PRIVATE)
                        .getBoolean(SharedPrefConst.KEY_SKIP_ADS, false)
                    val isSponsored = if (skipAds) checkForSponsoredContent(currentRoot) else false

                    if (skipAds && isSponsored) {
                        Log.d(TAG, "Sponsored content detected, scrolling immediately")
                        scrollVideos(currentRoot)
                        delay(500) // Short delay before next scroll
                    } else {
                        // Normal video - scroll after interval
                        scrollVideos(currentRoot)
                        delay(currentScrollInterval)
                    }

                    // Optional: Check if we've reached our daily limit
                    val todayStats = statsRepository.getTodayStats()
                    val reelLimit = service.getSharedPreferences(SharedPrefConst.PREF_NAME, Context.MODE_PRIVATE)
                        .getLong(SharedPrefConst.KEY_REEL_LIMIT, 50)

                    if (todayStats.linkedInVideosWatched >= reelLimit) {
                        Log.d(TAG, "Daily limit reached, stopping service")
                        break
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in LinkedIn navigation: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private suspend fun scrollVideos(rootNode: AccessibilityNodeInfo) {
        try {
            // Find the ViewPager that contains the videos
            val viewPager = findNodeRecursively(rootNode) { node ->
                node.className?.toString() == "androidx.viewpager.widget.ViewPager" &&
                        node.viewIdResourceName == "com.linkedin.android:id/viewPager2"
            }

            if (viewPager != null) {
                Log.d(TAG, "Found ViewPager, attempting scroll...")

                // Try gesture scroll since ViewPager might need swipe gesture
                val rect = android.graphics.Rect()
                viewPager.getBoundsInScreen(rect)

                val gestureBuilder = android.accessibilityservice.GestureDescription.Builder()
                val path = android.graphics.Path()

                // Calculate swipe coordinates (bottom to top)
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
                    Log.d(TAG, "Successfully scrolled to next video")

                    val isSponsored = checkForSponsoredContent(rootNode)
                    statsRepository.updateLinkedInStats(
                        reelsScrolled = 1,
                        adSkipped = isSponsored,
                        scrollInterval = currentScrollInterval
                    )


                }
            } else {
                Log.e(TAG, "ViewPager not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scrolling videos: ${e.message}")
        }
    }

    private fun checkForSponsoredContent(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        // Keywords that might indicate sponsored content
        val sponsoredKeywords = listOf("Promoted", "Sponsored", "Ad")

        return findNodeRecursively(node) { currentNode ->
            val text = currentNode.text?.toString()?.lowercase() ?: ""
            val description = currentNode.contentDescription?.toString()?.lowercase() ?: ""

            sponsoredKeywords.any { keyword ->
                text.contains(keyword.lowercase()) || description.contains(keyword.lowercase())
            }
        } != null
    }

    private fun findAndClickVideoTab(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        Log.d(TAG, "🔍 Starting video tab search with relaxed criteria...")

        // First try to find by specific ID and content description
        val videoTabById = findNodeRecursively(root) { node ->
            val isVideoTab =
                node.viewIdResourceName == "com.linkedin.android:id/tab_video" && node.contentDescription?.toString()
                    ?.contains(
                        "Video", ignoreCase = true
                    ) == true && node.isClickable && node.isEnabled

            // Log every potential tab for debugging
            if (node.viewIdResourceName?.contains("tab_") == true) {
                Log.d(
                    TAG, """
                Checking tab:
                ID: ${node.viewIdResourceName}
                ContentDesc: ${node.contentDescription}
                Class: ${node.className}
                Clickable: ${node.isClickable}
                Enabled: ${node.isEnabled}
            """.trimIndent()
                )
            }

            isVideoTab
        }

        if (videoTabById != null) {
            Log.d(TAG, "✅ Found video tab by ID!")
            return videoTabById
        }

        // Fallback: Look for any tab with "Video" in content description
        Log.d(TAG, "⚠️ ID search failed, trying content description search...")
        return findNodeRecursively(root) { node ->
            val isTab = node.className?.toString()?.contains("Tab", ignoreCase = true) == true
            val hasVideoDesc =
                node.contentDescription?.toString()?.contains("Video", ignoreCase = true) == true
            val isClickable = node.isClickable && node.isEnabled && node.isVisibleToUser

            if (isTab && hasVideoDesc) {
                Log.d(TAG, "Found potential video tab by description: ${node.contentDescription}")
            }

            isTab && hasVideoDesc && isClickable
        }?.also {
            Log.d(TAG, "✅ Found video tab by content description!")
        }
    }

    private fun findNodeRecursively(
        node: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (node == null) return null

        // Check current node
        if (predicate(node)) {
            Log.d(TAG, "✅ Found matching node!")
            return node
        }

        // Search children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = findNodeRecursively(child, predicate)
            if (result != null) return result
            child?.recycle()
        }
        return null
    }

    private fun logNodeHierarchy(node: AccessibilityNodeInfo?, depth: Int) {
        if (node == null || node.className == null) return

        val prefix = " ".repeat(depth * 2)
        val nodeDetails = """
        $prefix Class Name: ${node.className}
        $prefix Text: ${node.text ?: "null"}
        $prefix Content Description: ${node.contentDescription ?: "null"}
        $prefix View ID: ${node.viewIdResourceName ?: "null"}
        $prefix Clickable: ${node.isClickable}
        $prefix Visible: ${node.isVisibleToUser}
        $prefix Enabled: ${node.isEnabled}
        $prefix Child Count: ${node.childCount}
        """.trimIndent()

        Log.d(TAG, nodeDetails)

        // Print children recursively
        for (i in 0 until node.childCount) {
            logNodeHierarchy(node.getChild(i), depth + 1)
        }
    }
}