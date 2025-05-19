package com.reeler.app.service



import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.reeler.app.database.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SnapchatHandler(
    private val service: AccessibilityService,
    private val statsRepository: StatsRepository,
    private val serviceScope: CoroutineScope,
    private val currentScrollInterval: Long
) {
    companion object {
        private const val TAG = "SnapchatHandler"
        private const val SNAPCHAT_PACKAGE = "com.snapchat.android"
        private const val SCROLL_DURATION = 300L
        private const val INITIAL_DELAY = 3000L
        private const val NODE_WAIT_DELAY = 1500L
        private const val CONTENT_LOAD_DELAY = 2000L
    }

    private var isScrolling = false
    private var currentReelCount = 0
    private val bounds = Rect()
    private var lastUpdateTime = System.currentTimeMillis()

    fun handleSnapchatNavigation() {
        Log.d(TAG, "🚀 Snapchat navigation handler started")
        serviceScope.launch {
            try {
                Log.d(TAG, "Step 1: Waiting for Snapchat to open properly...")
                delay(INITIAL_DELAY)
                initializeNavigation()
            } catch (e: Exception) {
                Log.e(TAG, "Error in handleSnapchatNavigation: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private suspend fun initializeNavigation() {
        var rootNode: AccessibilityNodeInfo? = null
        var attempts = 0
        val maxAttempts = 10

        while (rootNode == null && attempts < maxAttempts) {
            rootNode = service.rootInActiveWindow
            Log.d(TAG, "Attempt ${attempts + 1}: Getting root node")

            if (rootNode == null) {
                Log.d(TAG, "Root node null, waiting...")
                delay(NODE_WAIT_DELAY)
                attempts++
            } else {
                handleRootNode(rootNode)
            }
        }
    }

    private suspend fun handleRootNode(rootNode: AccessibilityNodeInfo) {
        val packageName = rootNode.packageName?.toString() ?: "unknown"
        Log.d(TAG, "Root node found! Package: $packageName")

        if (packageName != SNAPCHAT_PACKAGE) {
            Log.d(TAG, "Wrong package, waiting for Snapchat...")
            delay(NODE_WAIT_DELAY)
            return
        }

        if (findAndClickForYouButton(rootNode)) {
            delay(CONTENT_LOAD_DELAY)
            startScrolling()
        }

        logHierarchy()
    }

    private fun logHierarchy() {
        val newRoot = service.rootInActiveWindow
        if (newRoot != null) {
            Log.d(TAG, "============ SNAPCHAT SCREEN HIERARCHY AFTER CLICK ============")
            logNodeHierarchy(newRoot, 0)
            Log.d(TAG, "============ END HIERARCHY ============")
        }
    }

    private fun startScrolling() {
        if (isScrolling) return

        isScrolling = true
        lastUpdateTime = System.currentTimeMillis()

        serviceScope.launch {
            try {
                while (isScrolling) {
                    if (scrollToNextVideo()) {
                        currentReelCount++
                        updateStats()
                    }
                    delay(currentScrollInterval)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during scrolling: ${e.message}")
                stopScrolling()
            }
        }
    }

    private suspend fun scrollToNextVideo(): Boolean = suspendCoroutine { continuation ->
        try {
            val spotlightContainer = findSpotlightContainer() ?: run {
                Log.e(TAG, "Spotlight container not found")
                continuation.resume(false)
                return@suspendCoroutine
            }

            spotlightContainer.getBoundsInScreen(bounds)
            performScrollGesture(continuation)
        } catch (e: Exception) {
            Log.e(TAG, "Error during scroll: ${e.message}")
            continuation.resume(false)
        }
    }

    private fun performScrollGesture(continuation: kotlin.coroutines.Continuation<Boolean>) {
        val startX = bounds.centerX().toFloat()
        val startY = (bounds.bottom * 0.75f)
        val endX = startX
        val endY = (bounds.top + (bounds.height() * 0.25f))

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, SCROLL_DURATION))
            .build()

        service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                continuation.resume(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                continuation.resume(false)
            }
        }, null)
    }

    private fun findSpotlightContainer(): AccessibilityNodeInfo? {
        val rootNode = service.rootInActiveWindow ?: return null
        return findNodeRecursively(rootNode) { node ->
            node.viewIdResourceName == "com.snapchat.android:id/spotlight_container"
        }
    }

    private fun updateStats() {
        val currentTime = System.currentTimeMillis()
        val timeSpent = (currentTime - lastUpdateTime) / 1000 // Convert to seconds
        lastUpdateTime = currentTime

        serviceScope.launch {
            try {
                 statsRepository.updateSnapchatStats(
                    storiesScrolled = 1,
                    timeSpentSeconds = timeSpent,
                    scrollInterval = currentScrollInterval
                )

                Log.d(TAG, "Stats updated - Story #$currentReelCount, Time spent: $timeSpent seconds")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating stats: ${e.message}")
            }
        }
    }
    private fun findAndClickForYouButton(rootNode: AccessibilityNodeInfo): Boolean {
        val forYouButton = findNodeRecursively(rootNode) { node ->
            node.className?.toString() == "android.view.ViewGroup" &&
                    node.viewIdResourceName == "com.snapchat.android:id/ngs_spotlight_icon_container" &&
                    node.isClickable && node.isVisibleToUser && node.isEnabled
        }

        return if (forYouButton != null) {
            Log.d(TAG, "Found For You button, clicking...")
            val clickResult = forYouButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d(TAG, "Click result: ${if (clickResult) "SUCCESS" else "FAILED"}")
            clickResult
        } else {
            Log.e(TAG, "Could not find For You button")
            false
        }
    }

    private fun findNodeRecursively(
        node: AccessibilityNodeInfo?,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (node == null) return null

        if (predicate(node)) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = findNodeRecursively(child, predicate)
            if (result != null) return result
            child?.recycle()
        }
        return null
    }

    private fun logNodeHierarchy(node: AccessibilityNodeInfo?, depth: Int) {
        if (node == null || node.className == null) {
            Log.d(TAG, "${" ".repeat(depth * 2)}Node is null or has null className")
            return
        }

        try {
            val nodeDetails = constructNodeDetails(node, depth)
            Log.d(TAG, nodeDetails)

            for (i in 0 until node.childCount) {
                try {
                    val child = node.getChild(i)
                    logNodeHierarchy(child, depth + 1)
                } catch (e: Exception) {
                    Log.e(TAG, "${" ".repeat(depth * 2)}Error processing child $i: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in logNodeHierarchy at depth $depth: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun constructNodeDetails(node: AccessibilityNodeInfo, depth: Int): String {
        val prefix = " ".repeat(depth * 2)
        return """
            $prefix Class Name: ${node.className}
            $prefix Text: ${node.text ?: "null"}
            $prefix Content Description: ${node.contentDescription ?: "null"}
            $prefix View ID: ${node.viewIdResourceName ?: "null"}
            $prefix Clickable: ${node.isClickable}
            $prefix Visible: ${node.isVisibleToUser}
            $prefix Enabled: ${node.isEnabled}
            $prefix Child Count: ${node.childCount}
        """.trimIndent()
    }

    fun stopScrolling() {
        isScrolling = false
    }

    fun isActive(): Boolean = isScrolling
}