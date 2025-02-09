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
import java.util.Locale

class BarChartFragment : BaseChartFragment() {
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
        "Days",
        "Weekly",
        "Monthly",
        "Yearly"
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
    private val weekFormat = SimpleDateFormat("yyyy-w", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_bar_chart, container, false)

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
            android.R.layout.simple_spinner_item,
            platforms
        )
        platformAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        platformSpinner.adapter = platformAdapter

        // Metric Spinner
        val metricAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            metrics
        )
        metricAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        metricSpinner.adapter = metricAdapter

        // Time Spinner
        val timesAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            timesSpinList
        )
        timesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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
                // Fetch all daily stats
                val statsList = withContext(Dispatchers.IO) {
                    dailyStatsDao.getAllStatsOrderedByDate()
                }

                // Group and aggregate data based on time range
                val groupedData = when (timeRange) {
                    "Days" -> groupByDays(statsList, platform, metric)
                    "Weekly" -> groupByWeeks(statsList, platform, metric)
                    "Monthly" -> groupByMonths(statsList, platform, metric)
                    "Yearly" -> groupByYears(statsList, platform, metric)
                    else -> mapOf()
                }

                // Prepare chart data
                val chartData = groupedData.values.toList()
                val categories = groupedData.keys.toList()

                // Create the chart model
                val chartModel = AAChartModel()
                    .chartType(AAChartType.Bar)
                    .categories(categories.toTypedArray())
                    .series(
                        arrayOf(
                            AASeriesElement()
                                .name(metric)
                                .data(chartData.toTypedArray())
                        )
                    )

                // Render the chart
                chartView.aa_drawChartWithChartModel(chartModel)

            } catch (e: Exception) {
                // Handle any errors in data retrieval or chart creation
                e.printStackTrace()
            }
        }
    }

    private fun groupByDays(
        statsList: List<DailyStats>,
        platform: String,
        metric: String
    ): Map<String, Double> {
        return statsList.associate { stats ->
            stats.date to when (metric) {
                "Reels Watched" -> when (platform) {
                    "Instagram" -> stats.instagramReelsWatched.toDouble()
                    "YouTube" -> stats.youtubeReelsWatched.toDouble()
                    "LinkedIn" -> stats.linkedInVideosWatched.toDouble()
                    "Snapchat" -> stats.snapchatStoriesWatched.toDouble()
                    else -> stats.reelsWatched.toDouble()
                }

                "Ads Skipped" -> when (platform) {
                    "Instagram" -> stats.instagramAdsSkipped.toDouble()
                    "YouTube" -> stats.youtubeAdsSkipped.toDouble()
                    "LinkedIn" -> stats.linkedInAdsSkipped.toDouble()
                    else -> stats.adsSkipped.toDouble()
                }

                "Time Saved (seconds)" -> when (platform) {
                    "Snapchat" -> stats.snapchatTimeSpentSeconds.toDouble()
                    else -> stats.timeSavedInSeconds.toDouble()
                }

                else -> 0.0
            }
        }
    }

    private fun groupByWeeks(
        statsList: List<DailyStats>,
        platform: String,
        metric: String
    ): Map<String, Double> {
        return statsList.groupBy { stats ->
            val date = dateFormat.parse(stats.date)
            weekFormat.format(date)
        }.mapValues { (_, weekStats) ->
            weekStats.sumOf { stats ->
                when (metric) {
                    "Reels Watched" -> when (platform) {
                        "Instagram" -> stats.instagramReelsWatched.toDouble()
                        "YouTube" -> stats.youtubeReelsWatched.toDouble()
                        "LinkedIn" -> stats.linkedInVideosWatched.toDouble()
                        "Snapchat" -> stats.snapchatStoriesWatched.toDouble()
                        else -> stats.reelsWatched.toDouble()
                    }

                    "Ads Skipped" -> when (platform) {
                        "Instagram" -> stats.instagramAdsSkipped.toDouble()
                        "YouTube" -> stats.youtubeAdsSkipped.toDouble()
                        "LinkedIn" -> stats.linkedInAdsSkipped.toDouble()
                        else -> stats.adsSkipped.toDouble()
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

    private fun groupByMonths(
        statsList: List<DailyStats>,
        platform: String,
        metric: String
    ): Map<String, Double> {
        return statsList.groupBy { stats ->
            val date = dateFormat.parse(stats.date)
            monthFormat.format(date)
        }.mapValues { (_, monthStats) ->
            monthStats.sumOf { stats ->
                when (metric) {
                    "Reels Watched" -> when (platform) {
                        "Instagram" -> stats.instagramReelsWatched.toDouble()
                        "YouTube" -> stats.youtubeReelsWatched.toDouble()
                        "LinkedIn" -> stats.linkedInVideosWatched.toDouble()
                        "Snapchat" -> stats.snapchatStoriesWatched.toDouble()
                        else -> stats.reelsWatched.toDouble()
                    }

                    "Ads Skipped" -> when (platform) {
                        "Instagram" -> stats.instagramAdsSkipped.toDouble()
                        "YouTube" -> stats.youtubeAdsSkipped.toDouble()
                        "LinkedIn" -> stats.linkedInAdsSkipped.toDouble()
                        else -> stats.adsSkipped.toDouble()
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

    private fun groupByYears(
        statsList: List<DailyStats>,
        platform: String,
        metric: String
    ): Map<String, Double> {
        return statsList.groupBy { stats ->
            val date = dateFormat.parse(stats.date)
            yearFormat.format(date)
        }.mapValues { (_, yearStats) ->
            yearStats.sumOf { stats ->
                when (metric) {
                    "Reels Watched" -> when (platform) {
                        "Instagram" -> stats.instagramReelsWatched.toDouble()
                        "YouTube" -> stats.youtubeReelsWatched.toDouble()
                        "LinkedIn" -> stats.linkedInVideosWatched.toDouble()
                        "Snapchat" -> stats.snapchatStoriesWatched.toDouble()
                        else -> stats.reelsWatched.toDouble()
                    }

                    "Ads Skipped" -> when (platform) {
                        "Instagram" -> stats.instagramAdsSkipped.toDouble()
                        "YouTube" -> stats.youtubeAdsSkipped.toDouble()
                        "LinkedIn" -> stats.linkedInAdsSkipped.toDouble()
                        else -> stats.adsSkipped.toDouble()
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
}