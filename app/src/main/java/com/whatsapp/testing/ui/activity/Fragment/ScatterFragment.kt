package com.whatsapp.testing.ui.activity.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.lifecycle.lifecycleScope
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartSymbolType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartView
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import com.whatsapp.testing.R
import com.whatsapp.testing.database.DailyStats
import com.whatsapp.testing.database.DailyStatsDao
import com.whatsapp.testing.database.StatsDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ScatterFragment : BaseChartFragment() {
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
        val view = inflater.inflate(R.layout.fragment_scatter, container, false)

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

        // Load initial data
        loadChartData(
            platforms[0],
            metrics[0],
            timesSpinList[0]
        )

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
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
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
                // Fetch all daily stats
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

                // Prepare scatter chart data
                val scatterData = prepareScatterData(statsList, platform, metric)

                // Create series
                val seriesElements = scatterData.map { (platformName, dataPoints) ->
                    AASeriesElement()
                        .name("$platformName - $metric")
                        .data(dataPoints.map { it.toList() }.toTypedArray())
                            as Any  // Cast each series element to Any
                }.toTypedArray()

// Create the chart model
                val chartModel = AAChartModel()
                    .chartType(AAChartType.Scatter)
                    //.title("$metric Distribution by Platform - $timeRange")
                    .series(seriesElements)  // This will now work with the Any array
                    .markerRadius(6.0)
                    .markerSymbol(AAChartSymbolType.Circle)
                    .dataLabelsEnabled(true)

                // Render the chart
                chartView.aa_drawChartWithChartModel(chartModel)

            } catch (e: Exception) {
                // Handle any errors in data retrieval or chart creation
                e.printStackTrace()
            }
        }
    }

    private fun filterByCurrentMonth(statsList: List<DailyStats>): List<DailyStats> {
        val currentMonth = monthFormat.format(System.currentTimeMillis())
        return statsList.filter {
            monthFormat.format(dateFormat.parse(it.date)) == currentMonth
        }
    }

    private fun filterByCurrentYear(statsList: List<DailyStats>): List<DailyStats> {
        val currentYear = yearFormat.format(System.currentTimeMillis())
        return statsList.filter {
            yearFormat.format(dateFormat.parse(it.date)) == currentYear
        }
    }

    private fun prepareScatterData(
        statsList: List<DailyStats>,
        platform: String,
        metric: String
    ): Map<String, List<Array<Any>>> {
        // Prepare data for scatter plot
        val platformList = if (platform == "All Platforms") {
            listOf("Instagram", "YouTube", "LinkedIn", "Snapchat")
        } else {
            listOf(platform)
        }

        return platformList.associate { platformName ->
            val dataPoints = statsList.mapIndexedNotNull { index, stats ->
                val value = when (metric) {
                    "Reels Watched" -> when (platformName) {
                        "Instagram" -> stats.instagramReelsWatched.toDouble()
                        "YouTube" -> stats.youtubeReelsWatched.toDouble()
                        "LinkedIn" -> stats.linkedInVideosWatched.toDouble()
                        "Snapchat" -> stats.snapchatStoriesWatched.toDouble()
                        else -> 0.0
                    }
                    "Ads Skipped" -> when (platformName) {
                        "Instagram" -> stats.instagramAdsSkipped.toDouble()
                        "YouTube" -> stats.youtubeAdsSkipped.toDouble()
                        "LinkedIn" -> stats.linkedInAdsSkipped.toDouble()
                        else -> 0.0
                    }
                    "Time Saved (seconds)" -> when (platformName) {
                        "Snapchat" -> stats.snapchatTimeSpentSeconds.toDouble()
                        else -> stats.timeSavedInSeconds.toDouble()
                    }
                    else -> 0.0
                }

                // Only include non-zero values
                if (value > 0) {
                    arrayOf<Any>(index.toDouble(), value)
                } else {
                    null
                }
            }

            platformName to dataPoints
        }
    }
}