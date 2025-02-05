package com.whatsapp.testing.ui.activity

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import android.graphics.drawable.GradientDrawable
import android.view.Menu
import android.view.MenuItem
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.IMarker
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.utils.ViewPortHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.whatsapp.testing.R
import com.whatsapp.testing.database.DailyStats
import com.whatsapp.testing.database.StatsDatabase
import com.whatsapp.testing.database.StatsRepository
import com.whatsapp.testing.databinding.ActivityStatsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStatsBinding
    private lateinit var statsRepository: StatsRepository
    private lateinit var lineChart: LineChart
    private var currentChartType = ChartType.LINE
    private var selectedTimeFilter = TimeFilter.DAILY
    private var selectedPlatform = "all"

    private enum class ChartType {
        LINE, BAR
    }

    private enum class TimeFilter {
        DAILY, WEEKLY, MONTHLY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        initializeRepository()
        setupTimeFilterTabs()
        setupPlatformChips()
        setupChartTypeToggle()
        setupChart()
        loadInitialData()
    }
    private fun initializeRepository() {
        val dao = StatsDatabase.getDatabase(this).dailyStatsDao()
        statsRepository = StatsRepository(dao)
    }

    private fun setupTimeFilterTabs() {
        with(binding.timeFilterTabs) {
            addTab(newTab().setText("Daily"))
            addTab(newTab().setText("Weekly"))
            addTab(newTab().setText("Monthly"))

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    selectedTimeFilter = when (tab.position) {
                        0 -> TimeFilter.DAILY
                        1 -> TimeFilter.WEEKLY
                        else -> TimeFilter.MONTHLY
                    }
                    lifecycleScope.launch {
                        updateChartData()
                    }
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }
    }

    private fun setupPlatformChips() {
        val platforms = listOf(
            "All Platforms" to "all",
            "Instagram" to "instagram",
            "YouTube" to "youtube",
            "LinkedIn" to "linkedin",
            "Snapchat" to "snapchat"
        )

        binding.platformChipGroup.apply {
            platforms.forEach { (label, id) ->
                addView(createChip(label, id))
            }
            setOnCheckedStateChangeListener { group, checkedIds ->
                checkedIds.firstOrNull()?.let { chipId ->
                    selectedPlatform = group.findViewById<Chip>(chipId).tag.toString()
                    lifecycleScope.launch {
                        updateChartData()
                    }
                }
            }
            check(childCount) // Select first chip by default
        }
    }

    private fun createChip(label: String, id: String): Chip {
        return Chip(this).apply {
            text = label
            tag = id
            isCheckable = true
            setChipBackgroundColorResource(R.color.teal_200)
            setTextColor(resources.getColorStateList(R.color.colorPrimary, null))
        }
    }


    private fun setupChartTypeToggle() {
        binding.lineChartButton.setOnClickListener {
            lifecycleScope.launch {
                updateChartType(ChartType.LINE)
            }
        }
        binding.barChartButton.setOnClickListener {
            lifecycleScope.launch {
                updateChartType(ChartType.BAR)
            }
        }
    }

    private suspend fun updateChartType(type: ChartType) {
        currentChartType = type
        binding.lineChartButton.setTextColor(
            if (type == ChartType.LINE) getColor(R.color.teal_200)
            else getColor(R.color.darkerGrey)
        )
        binding.barChartButton.setTextColor(
            if (type == ChartType.BAR) getColor(R.color.teal_200)
            else getColor(R.color.darkerGrey)
        )
        updateChartData()
    }



    // Replace the setupChart() method with this updated version
    private fun setupChart() {
        lineChart = binding.mainChart.apply {
            // Basic Setup
            setBackgroundColor(getColor(R.color.white))
            description.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)

            // Enable scaling and dragging
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(false)

            // Animation
            animateX(1500)

            // Legend customization
            legend.apply {
                form = Legend.LegendForm.LINE
                textSize = 12f
                textColor = getColor(R.color.colorPrimary)
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }

            // X-Axis styling
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = getColor(R.color.darkerGrey)
                setDrawGridLines(false)
                setDrawAxisLine(true)
                axisLineColor = getColor(R.color.grey)
                axisLineWidth = 1f
                textSize = 10f
                labelRotationAngle = -45f
                valueFormatter = IAxisValueFormatter { value, _ ->
                    when (selectedTimeFilter) {
                        TimeFilter.DAILY -> getDayLabel(value.toInt())
                        TimeFilter.WEEKLY -> getWeekLabel(value.toInt())
                        TimeFilter.MONTHLY -> getMonthLabel(value.toInt())
                    }
                }
            }

            // Y-Axis styling
            axisLeft.apply {
                textColor = getColor(R.color.darkerGrey)
                setDrawGridLines(true)
                gridColor = getColor(R.color.grey)
                gridLineWidth = 0.5f
                setDrawAxisLine(true)
                axisLineColor = getColor(R.color.grey)
                axisLineWidth = 1f
                textSize = 10f
                granularity = 1f
                setDrawZeroLine(true)
                zeroLineWidth = 1f
                zeroLineColor = getColor(R.color.grey)
            }

            // Disable right axis
            axisRight.isEnabled = false

            // Add marker view for better value display
            marker = CustomMarkerView(context, R.layout.marker_view)
        }
    }

    private fun loadInitialData() {
        lifecycleScope.launch {
            try {
                val stats = statsRepository.getAllStatsOrderedByDate()
                updateSummaryCards(stats)
                updatePlatformDistribution(stats)
                updateChartData()
            } catch (e: Exception) {
                Toast.makeText(this@StatsActivity, "Error loading stats", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSummaryCards(stats: List<DailyStats>) {
        val lastStats = stats.firstOrNull() ?: return
        val previousStats = stats.getOrNull(1)

        // Update Total Content
        val totalContent = lastStats.reelsWatched
        val totalContentChange = calculatePercentageChange(
            previousStats?.reelsWatched ?: 0,
            totalContent
        )
        binding.totalContentValue.text = totalContent.toString()
        binding.totalContentTrend.apply {
            text = formatTrendText(totalContentChange)
            setTextColor(getTrendColor(totalContentChange))
        }

        // Update Time Saved
        val timeSaved = lastStats.timeSavedInSeconds
        val timeSavedChange = calculatePercentageChange(
            previousStats?.timeSavedInSeconds ?: 0,
            timeSaved
        )
        binding.timeSavedValue.text = formatTimeString(timeSaved)
        binding.timeSavedTrend.apply {
            text = formatTrendText(timeSavedChange)
            setTextColor(getTrendColor(timeSavedChange))
        }
    }


    private fun updatePlatformDistribution(stats: List<DailyStats>) {
        val lastStats = stats.firstOrNull() ?: return
        val total = lastStats.run {
            instagramReelsWatched + youtubeReelsWatched + linkedInVideosWatched + snapchatStoriesWatched
        }.toFloat()

        if (total == 0f) return

        val instagramPercentage = (lastStats.instagramReelsWatched / total * 100).toFloat()
        val youtubePercentage = (lastStats.youtubeReelsWatched / total * 100).toFloat()
        val linkedinPercentage = (lastStats.linkedInVideosWatched / total * 100).toFloat()
        val snapchatPercentage = (lastStats.snapchatStoriesWatched / total * 100).toFloat()

        // Log values for debugging
        Log.d("StatsActivity", """
        Total: $total
        Instagram: $instagramPercentage%
        YouTube: $youtubePercentage%
        LinkedIn: $linkedinPercentage%
        Snapchat: $snapchatPercentage%
    """.trimIndent())

        // Update progress bars and text
        updatePlatformProgress(
            binding.instagramProgress,
            binding.instagramPercent,
            instagramPercentage,
            lastStats.instagramReelsWatched
        )
        updatePlatformProgress(
            binding.youtubeProgress,
            binding.youtubePercent,
            youtubePercentage,
            lastStats.youtubeReelsWatched
        )
        updatePlatformProgress(
            binding.linkedinProgress,
            binding.linkedinPercent,
            linkedinPercentage,
            lastStats.linkedInVideosWatched
        )
        updatePlatformProgress(
            binding.snapchatProgress,
            binding.snapchatPercent,
            snapchatPercentage,
            lastStats.snapchatStoriesWatched
        )
    }

    private fun updatePlatformProgress(
        progressBar: ProgressBar,
        textView: TextView,
        percentage: Float,
        actualCount: Int
    ) {
        progressBar.apply {
            max = 100  // Set maximum to 100 for percentage
            progress = percentage.toInt()
        }

        // Update text to show both percentage and actual count
        textView.text = String.format("%.1f%% (%d)", percentage, actualCount)
    }
    private suspend fun updateChartData() = withContext(Dispatchers.IO) {
        val stats = statsRepository.getAllStatsOrderedByDate()
        val entries = when (selectedTimeFilter) {
            TimeFilter.DAILY -> prepareDailyData(stats)
            TimeFilter.WEEKLY -> prepareWeeklyData(stats)
            TimeFilter.MONTHLY -> prepareMonthlyData(stats)
        }

        withContext(Dispatchers.Main) {
            updateChart(entries)
        }
    }
    private fun prepareDailyData(stats: List<DailyStats>): List<Entry> {
        return stats.take(7).reversed().mapIndexed { index, stat ->
            val value = when (selectedPlatform) {
                "instagram" -> stat.instagramReelsWatched
                "youtube" -> stat.youtubeReelsWatched
                "linkedin" -> stat.linkedInVideosWatched
                "snapchat" -> stat.snapchatStoriesWatched
                else -> stat.reelsWatched
            }
            Entry(index.toFloat(), value.toFloat())
        }
    }

    private fun prepareWeeklyData(stats: List<DailyStats>): List<Entry> {
        // Group stats by week
        val weeklyStats = stats.groupBy { stat ->
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(stat.date)
            val calendar = Calendar.getInstance().apply { time = date }
            calendar.get(Calendar.WEEK_OF_YEAR)
        }.toList().take(4)

        return weeklyStats.mapIndexed { index, (_, weekStats) ->
            val weeklySum = when (selectedPlatform) {
                "instagram" -> weekStats.sumOf { it.instagramReelsWatched }
                "youtube" -> weekStats.sumOf { it.youtubeReelsWatched }
                "linkedin" -> weekStats.sumOf { it.linkedInVideosWatched }
                "snapchat" -> weekStats.sumOf { it.snapchatStoriesWatched }
                else -> weekStats.sumOf { it.reelsWatched }
            }
            Entry(index.toFloat(), weeklySum.toFloat())
        }
    }
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.stats_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_reset -> {
                resetToDefault()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun resetToDefault() {
        lifecycleScope.launch {
            // Reset time filter to Daily
            binding.timeFilterTabs.getTabAt(0)?.select()
            selectedTimeFilter = TimeFilter.DAILY

            // Reset platform selection to All
            binding.platformChipGroup.check(binding.platformChipGroup.getChildAt(0).id)
            selectedPlatform = "all"

            // Reset chart type to Line
            updateChartType(ChartType.LINE)

            // Update chart data
            updateChartData()
        }

        // Show reset confirmation
        Toast.makeText(this, "Graph reset to default", Toast.LENGTH_SHORT).show()
    }
    private fun prepareMonthlyData(stats: List<DailyStats>): List<Entry> {
        // Group stats by month
        val monthlyStats = stats.groupBy { stat ->
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(stat.date)
            val calendar = Calendar.getInstance().apply { time = date }
            calendar.get(Calendar.MONTH)
        }.toList().take(3)

        return monthlyStats.mapIndexed { index, (_, monthStats) ->
            val monthlySum = when (selectedPlatform) {
                "instagram" -> monthStats.sumOf { it.instagramReelsWatched }
                "youtube" -> monthStats.sumOf { it.youtubeReelsWatched }
                "linkedin" -> monthStats.sumOf { it.linkedInVideosWatched }
                "snapchat" -> monthStats.sumOf { it.snapchatStoriesWatched }
                else -> monthStats.sumOf { it.reelsWatched }
            }
            Entry(index.toFloat(), monthlySum.toFloat())
        }
    }


    // Update the updateChart() method
    private fun updateChart(entries: List<Entry>) {
        if (entries.isEmpty()) {
            lineChart.clear()
            lineChart.invalidate()
            return
        }

        val dataSet = LineDataSet(entries, getChartLabel()).apply {
            // Line styling
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            lineWidth = 2.5f
            color = getColor(R.color.teal_200)

            // Fill styling
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(
                this@StatsActivity,
                R.drawable.chart_gradient_background
            )

            // Circle styling
            setDrawCircles(true)
            setCircleColor(getColor(R.color.teal_200))
            circleRadius = 4f
            setDrawCircleHole(true)
            circleHoleRadius = 2f
//            circleHoleColor = getColor(R.color.white)

            // Value styling
            setDrawValues(false)
            valueTextSize = 12f
            valueTextColor = getColor(R.color.colorPrimary)
            valueFormatter = IValueFormatter { value, _, _, _ ->
                value.toInt().toString()
            }
        }

        lineChart.apply {
            data = LineData(dataSet)
            notifyDataSetChanged()
            invalidate()
            animateY(1000)
        }
    }

    private fun getChartLabel(): String {
        return when (selectedPlatform) {
            "instagram" -> "Instagram Reels"
            "youtube" -> "YouTube Shorts"
            "linkedin" -> "LinkedIn Videos"
            "snapchat" -> "Snapchat Stories"
            else -> "All Content"
        }
    }

    private fun getDayLabel(index: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6 + index)
        return SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time)
    }

    private fun getWeekLabel(index: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, -3 + index)
        return "Week ${calendar.get(Calendar.WEEK_OF_YEAR)}"
    }

    private fun getMonthLabel(index: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -2 + index)
        return SimpleDateFormat("MMM", Locale.getDefault()).format(calendar.time)
    }


    private fun calculatePercentageChange(old: Number, new: Number): Float {
        if (old.toFloat() == 0f) return 0f
        return ((new.toFloat() - old.toFloat()) / old.toFloat() * 100)
    }

    private fun formatTrendText(percentage: Float): String {
        return if (percentage >= 0) {
            "↑ %.1f%% vs last period".format(percentage)
        } else {
            "↓ %.1f%% vs last period".format(-percentage)
        }
    }

    private fun getTrendColor(percentage: Float): Int {
        return getColor(if (percentage >= 0) R.color.darkGreen else R.color.red)
    }

    private fun formatTimeString(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }


    // Update the CustomMarkerView class
    private class CustomMarkerView(context: Context, layoutResource: Int) :
        MarkerView(context, layoutResource), IMarker {

        private val tvContent: TextView = findViewById(R.id.tvContent)

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            e?.let {
                tvContent.text = "${it.y.toInt()}"
            }
            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF {
            return MPPointF((-(width / 2)).toFloat(), (-height - 10).toFloat())
        }
    }
}