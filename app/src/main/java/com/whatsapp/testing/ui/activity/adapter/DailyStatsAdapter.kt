package com.whatsapp.testing.ui.activity.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.whatsapp.testing.R
import com.whatsapp.testing.database.DailyStats
import com.whatsapp.testing.databinding.ItemDailyStatsBinding
import java.text.SimpleDateFormat
import java.util.*
import com.github.mikephil.charting.formatter.DefaultValueFormatter


class DailyStatsAdapter : RecyclerView.Adapter<DailyStatsAdapter.StatsViewHolder>() {
    private var statsList = emptyList<DailyStats>()
    private var selectedChartType = ChartType.WEEKLY

    enum class ChartType {
        WEEKLY, MONTHLY
    }

    inner class StatsViewHolder(private val binding: ItemDailyStatsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(stats: DailyStats) {
            setupDate(stats)
            setupToggleButtons()
            updateChart(stats)
            updateTotalStats(stats)
        }

        private fun setupDate(stats: DailyStats) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(stats.date)
            binding.dateText.text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
            binding.dayText.text = stats.days
            binding.monthText.text = stats.month
        }

        private fun setupToggleButtons() {
            binding.toggleGroup.check(
                when (selectedChartType) {
                    ChartType.WEEKLY -> R.id.weeklyButton
                    ChartType.MONTHLY -> R.id.monthlyButton
                }
            )

            binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    selectedChartType = when (checkedId) {
                        R.id.weeklyButton -> ChartType.WEEKLY
                        R.id.monthlyButton -> ChartType.MONTHLY
                        else -> ChartType.WEEKLY
                    }
                    updateChart(statsList[adapterPosition])
                    updateTotalStats(statsList[adapterPosition])
                }
            }
        }

        private fun getWeeklyData(): List<BarEntry> {
            val today = Calendar.getInstance()
            val lastSevenDays = (0..6).map { index ->
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today.apply { add(Calendar.DATE, -index) }.time)
                statsList.find { it.date == date } ?: DailyStats(date = date, days = "", month = "")
            }.reversed()

            return lastSevenDays.mapIndexed { index, dayStats ->
                BarEntry(
                    index.toFloat(),
                    floatArrayOf(
                        dayStats.instagramReelsWatched.toFloat(),
                        dayStats.youtubeReelsWatched.toFloat(),
                        dayStats.linkedInVideosWatched.toFloat(),
                        dayStats.snapchatStoriesWatched.toFloat()
                    )
                )
            }
        }

        private fun getMonthlyData(): List<BarEntry> {
            val lastThirtyDays = statsList.takeLast(30).reversed()
            val monthlyStats = lastThirtyDays.groupBy {
                SimpleDateFormat("MMM", Locale.getDefault()).format(
                    SimpleDateFormat("yyyy-MM-dd").parse(it.date)
                )
            }

            return monthlyStats.keys.mapIndexed { index, month ->
                val monthData = monthlyStats[month]
                BarEntry(
                    index.toFloat(),
                    floatArrayOf(
                        monthData?.sumOf { it.instagramReelsWatched }?.toFloat() ?: 0f,
                        monthData?.sumOf { it.youtubeReelsWatched }?.toFloat() ?: 0f,
                        monthData?.sumOf { it.linkedInVideosWatched }?.toFloat() ?: 0f,
                        monthData?.sumOf { it.snapchatStoriesWatched }?.toFloat() ?: 0f
                    )
                )
            }
        }

        private fun updateChart(stats: DailyStats) {
            val entries = when (selectedChartType) {
                ChartType.WEEKLY -> getWeeklyData()
                ChartType.MONTHLY -> getMonthlyData()
            }

            val dataSets = entries.map { entry ->
                BarDataSet(listOf(entry), "").apply {
                    colors = getPlatformColors()
                    valueTextSize = 12f
                    valueFormatter = DefaultValueFormatter(0)
                    setDrawValues(true)
                }
            }

            binding.chart.apply {
                if (dataSets.size < 2) {
                    val emptyDataSet = BarDataSet(emptyList(), "").apply {
                        colors = listOf(Color.TRANSPARENT)
                    }
                    data = BarData(dataSets + emptyDataSet)
                } else {
                    data = BarData(dataSets)
                }
                setupBarChartStyle(this)
                if (dataSets.size > 1) {
                    groupBars(0f, 0.1f, 0.05f)
                }
                animateY(1500)
            }
        }

        private fun setupBarChartStyle(chart: BarChart) {
            chart.apply {
                setDrawGridBackground(false)
                setDrawBorders(false)
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    setDrawAxisLine(false)
                    labelCount = when (selectedChartType) {
                        ChartType.WEEKLY -> 7 // For days
                        ChartType.MONTHLY -> getMonthlyData().size // Based on actual monthly data
                    }
                    valueFormatter = object : IndexAxisValueFormatter() {
                        override fun getFormattedValue(value: Float, axis: AxisBase?): String? {
                            return when (selectedChartType) {
                                ChartType.WEEKLY -> getWeekDayName(value.toInt())
                                ChartType.MONTHLY -> getMonthName(value.toInt())
                            }
                        }
                    }
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    setDrawAxisLine(false)
                    gridColor = Color.parseColor("#E0E0E0")
                    gridLineWidth = 0.2f
                    axisMinimum = 0f
                }

                axisRight.isEnabled = false
                description.isEnabled = false
                legend.isEnabled = false

                setTouchEnabled(true)
                setPinchZoom(false)
            }
        }


        private fun getWeekDayName(index: Int): String {
            val today = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
            return (0..6).map { i ->
                today.apply { add(Calendar.DATE, -i) }.time
            }.reversed()[index].let { dateFormat.format(it) }
        }


        private fun getMonthName(index: Int): String {
            val months = statsList.takeLast(30).map { it.month }.distinct()
            return months.getOrElse(index) { "" }
        }

        private fun getChartTitle() = when (selectedChartType) {
            ChartType.WEEKLY -> "Weekly Activity"
            ChartType.MONTHLY -> "Monthly Activity"
        }

        private fun getDailyData(stats: DailyStats) = listOf(
            BarEntry(0f, stats.instagramReelsWatched.toFloat()),
            BarEntry(1f, stats.youtubeReelsWatched.toFloat()),
            BarEntry(2f, stats.linkedInVideosWatched.toFloat()),
            BarEntry(3f, stats.snapchatStoriesWatched.toFloat())
        )

        private fun getTotalDaily(stats: DailyStats) =
            stats.instagramReelsWatched + stats.youtubeReelsWatched +
                    stats.linkedInVideosWatched + stats.snapchatStoriesWatched

        private fun getTotalWeekly() = statsList.takeLast(7).sumOf { getTotalDaily(it) }

        private fun getTotalMonthly() = statsList.takeLast(30).sumOf { getTotalDaily(it) }

        private fun updateTotalStats(stats: DailyStats) {
            val total = when (selectedChartType) {
                ChartType.WEEKLY -> getTotalWeekly()
                ChartType.MONTHLY -> getTotalMonthly()
            }
            binding.totalStatsText.text = "Total Content Viewed: $total"
        }

        private fun getPlatformName(index: Int) = when (index) {
            0 -> "Instagram"
            1 -> "YouTube"
            2 -> "LinkedIn"
            3 -> "Snapchat"
            else -> ""
        }

        private fun getPlatformColors() = listOf(
            Color.parseColor("#E91E63"), // Instagram
            Color.parseColor("#FF0000"), // YouTube
            Color.parseColor("#0A66C2"), // LinkedIn
            Color.parseColor("#FFFC00")  // Snapchat
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StatsViewHolder(
            ItemDailyStatsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: StatsViewHolder, position: Int) {
        holder.bind(statsList[position])
    }

    override fun getItemCount() = statsList.size

    fun submitList(list: List<DailyStats>) {
        statsList = list
        notifyDataSetChanged()
    }
}