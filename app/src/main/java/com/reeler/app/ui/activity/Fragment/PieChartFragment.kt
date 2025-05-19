package com.reeler.app.ui.activity.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.lifecycle.lifecycleScope
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartView
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import com.reeler.app.R
import com.reeler.app.database.DailyStats
import com.reeler.app.database.DailyStatsDao
import com.reeler.app.database.StatsDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PieChartFragment : BaseChartFragment() {
    private lateinit var chartView: AAChartView
    private lateinit var platformSpinner: Spinner
    private lateinit var metricSpinner: Spinner
    private lateinit var timesSpinner: Spinner
    private lateinit var dailyStatsDao: DailyStatsDao

    // Metrics to display
    private val metrics = listOf(
        "Reels Watched",
        "Ads Skipped",
        "Time Saved (seconds)"
    )

    private val timesSpinList = listOf(
        "Total",
        "Today",
        "This Week",
        "This Month",
        "This Year"
    )

    // Platforms to display
    private val platforms = listOf(
        "All Platforms",
        "Instagram",
        "YouTube",
        "LinkedIn",
        "Snapchat"
    )

    // Date formatters
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_pie_chart, container, false)

        // Initialize the chart view
        chartView = view.findViewById(R.id.aa_chart_view)
        platformSpinner = view.findViewById(R.id.platform_spinner)
        metricSpinner = view.findViewById(R.id.metric_spinner)
        timesSpinner = view.findViewById(R.id.time_spinner)

        // Setup spinners
        setupSpinners()

        // Initialize the database and DAO
        val database = StatsDatabase.getDatabase(requireContext())
        dailyStatsDao = database.dailyStatsDao()

        return view
    }

    private fun setupSpinners() {
        // Platform Spinner
        val platformAdapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            platforms
        )
        platformAdapter.setDropDownViewResource(R.layout.dropdown_item)
        platformSpinner.adapter = platformAdapter

        // Metric Spinner
        val metricAdapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            metrics
        )
        metricAdapter.setDropDownViewResource(R.layout.dropdown_item)
        metricSpinner.adapter = metricAdapter

        // Time Spinner
        val timesAdapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            timesSpinList
        )
        timesAdapter.setDropDownViewResource(R.layout.dropdown_item)
        timesSpinner.adapter = timesAdapter

        // Add spinner listeners
        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                loadChartData(
                    platforms[platformSpinner.selectedItemPosition],
                    metrics[metricSpinner.selectedItemPosition],
                    timesSpinList[timesSpinner.selectedItemPosition]
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        platformSpinner.onItemSelectedListener = spinnerListener
        metricSpinner.onItemSelectedListener = spinnerListener
        timesSpinner.onItemSelectedListener = spinnerListener
    }

    private fun loadChartData(platform: String, metric: String, timeRange: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Fetch stats based on time range
                val statsList = withContext(Dispatchers.IO) {
                    when (timeRange) {
                        "Today" -> {
                            val today = dateFormat.format(System.currentTimeMillis())
//                            Log.d("PieChartDebug", "Today's date: $today")
                            val stats = dailyStatsDao.getTodayStats(today)
//                            Log.d("PieChartDebug", "Today's stats count: ${stats.size}")
                            stats
                        }

                        "This Week" -> {
                            val calendar = Calendar.getInstance()
                            val weekStart = dateFormat.format(calendar.apply {
                                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                            }.time)
                            val weekEnd = dateFormat.format(calendar.apply {
                                add(Calendar.DAY_OF_WEEK, 6)
                            }.time)
//                            Log.d("PieChartDebug", "Week range - Start: $weekStart, End: $weekEnd")
                            val stats = dailyStatsDao.getCurrentWeekStats(weekStart, weekEnd)
//                            Log.d("PieChartDebug", "Week stats count: ${stats.size}")
                            stats
                        }

                        "This Month" -> {
                            val currentMonth = monthFormat.format(System.currentTimeMillis())
//                            Log.d("PieChartDebug", "Current month: $currentMonth")
                            val stats = dailyStatsDao.getCurrentMonthStats(currentMonth)
//                            Log.d("PieChartDebug", "Month stats count: ${stats.size}")
                            stats
                        }

                        "This Year" -> {
                            val currentYear = yearFormat.format(System.currentTimeMillis())
//                            Log.d("PieChartDebug", "Current year: $currentYear")
                            val stats = dailyStatsDao.getCurrentYearStats(currentYear)
//                            Log.d("PieChartDebug", "Year stats count: ${stats.size}")
                            stats

                        }

                        else -> dailyStatsDao.getAllStatsOrderedByDate()
                    }
                }

                // Prepare pie chart data
                val pieData = preparePieData(statsList, platform, metric)

                // Create the chart model
                val chartModel = AAChartModel()
                    .chartType(AAChartType.Pie)
                    .backgroundColor("#00000000")
                    .colorsTheme(arrayOf("#FF4B6C", "#FF8959", "#FFB344", "#26FFFFFF"))
                    .dataLabelsEnabled(true)
                    .series(
                        arrayOf(
                            AASeriesElement()
                                .name(metric)
                                .data(pieData.map { arrayOf(it.key, it.value) }.toTypedArray())
                        )
                    )
                    .touchEventEnabled(true)
                    .yAxisTitle("")
                    .legendEnabled(true)
                    .tooltipEnabled(true)
                    .tooltipValueSuffix(getMetricSuffix(metric))
                    .polar(false)
                    .yAxisReversed(true)
                    .yAxisVisible(true)

                // Render the chart
                chartView.aa_drawChartWithChartModel(chartModel)

            } catch (e: Exception) {
                e.printStackTrace()
                // TODO: Show error message to user
            }
        }
    }

    private fun getMetricSuffix(metric: String): String {
        return when (metric) {
            "Time Saved (seconds)" -> "s"
            else -> ""
        }
    }

    private fun preparePieData(
        statsList: List<DailyStats>,
        platform: String,
        metric: String
    ): Map<String, Double> {
        // If not "All Platforms", return data only for selected platform
        if (platform != "All Platforms") {
            val value = calculatePlatformMetric(statsList, platform, metric)
            return if (value > 0) mapOf(platform to value) else emptyMap()
        }

        // Aggregate data for each platform
        val platformData = mapOf(
            "Instagram" to calculatePlatformMetric(statsList, "Instagram", metric),
            "YouTube" to calculatePlatformMetric(statsList, "YouTube", metric),
            "LinkedIn" to calculatePlatformMetric(statsList, "LinkedIn", metric),
            "Snapchat" to calculatePlatformMetric(statsList, "Snapchat", metric)
        )

        // Filter out platforms with zero values
        return platformData.filter { it.value > 0 }
    }

    private fun calculatePlatformMetric(
        statsList: List<DailyStats>,
        platform: String,
        metric: String
    ): Double {
        return statsList.sumOf { stats ->
            when (metric) {
                "Reels Watched" -> when (platform) {
                    "Instagram" -> stats.instagramReelsWatched.toDouble()
                    "YouTube" -> stats.youtubeReelsWatched.toDouble()
                    "LinkedIn" -> stats.linkedInVideosWatched.toDouble()
                    "Snapchat" -> stats.snapchatStoriesWatched.toDouble()
                    else -> 0.0
                }

                "Ads Skipped" -> when (platform) {
                    "Instagram" -> stats.instagramAdsSkipped.toDouble()
                    "YouTube" -> stats.youtubeAdsSkipped.toDouble()
                    "LinkedIn" -> stats.linkedInAdsSkipped.toDouble()
                    else -> 0.0
                }

                "Time Saved (seconds)" -> when (platform) {
                    "Snapchat" -> stats.snapchatTimeSpentSeconds.toDouble()
                    else -> stats.timeSavedInSeconds.toDouble()
                }

                else -> 0.0
            }
        }
    }
}