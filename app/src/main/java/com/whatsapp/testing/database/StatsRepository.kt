package com.whatsapp.testing.database

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class  StatsRepository(private val dailyStatsDao: DailyStatsDao) {
    private val currentDate: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    suspend fun updateSnapchatStats(
        storiesScrolled: Int = 0,
        timeSpentSeconds: Long = 0,
        scrollInterval: Long = 0
    ) {
        val today = currentDate
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(today)
        val day = getFormattedDay(date)
        val month = getFormattedMonth(date)
        var stats = dailyStatsDao.getStatsForDate(today) ?: DailyStats(date = today, days = day, month = month)

        // Ensure stats exist in database
        dailyStatsDao.insert(stats)

        // Update Snapchat specific stats
        dailyStatsDao.updateSnapchatStats(
            date = today,
            storiesCount = storiesScrolled,
            autoScrolled = storiesScrolled, // All stories are auto-scrolled
            timeSpent = timeSpentSeconds
        )

        // Update total counts
        dailyStatsDao.updateTotalStats(
            date = today,
            reelsCount = storiesScrolled,
            adsSkipped = 0, // Snapchat doesn't have ad skipping
            timeSaved = calculateTimeSaved(timeSpentSeconds, scrollInterval),
            autoScrolled = storiesScrolled
        )
    }
    private fun getFormattedDay(date: Date): String {
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        return dayFormat.format(date)
    }

    private fun getFormattedMonth(date: Date): String {
        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        return monthFormat.format(date)
    }

    private fun calculateTimeSaved(actualTime: Long, scrollInterval: Long): Long {
        val normalViewingTime = DailyStats.AVERAGE_REEL_DURATION_SECONDS
        val actualViewingTime = scrollInterval / 1000
        return (normalViewingTime - actualViewingTime).coerceAtLeast(0)
    }

    suspend fun updateTodayStats(
        reelsScrolled: Int = 0,
        adSkipped: Boolean = false,
        scrollInterval: Long = 0
    ) {
        val today = currentDate
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(today)
        val day = getFormattedDay(date)
        val month = getFormattedMonth(date)
        val stats = dailyStatsDao.getStatsForDate(today) ?: DailyStats(date = today, days = day, month = month)

        stats.apply {
            reelsWatched += reelsScrolled
            if (adSkipped) adsSkipped++

            val normalViewingTime = DailyStats.AVERAGE_REEL_DURATION_SECONDS
            val actualViewingTime = scrollInterval / 1000

            timeSavedInSeconds += if (adSkipped) {
                normalViewingTime
            } else {
                (normalViewingTime - actualViewingTime).coerceAtLeast(0)
            }

            totalScrolls++
            if (!adSkipped) autoScrolledReels++
        }

        dailyStatsDao.insert(stats)
    }

    suspend fun getTodayStats(): DailyStats {
        val today = currentDate
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(today)
        val day = getFormattedDay(date)
        val month = getFormattedMonth(date)
        return dailyStatsDao.getStatsForDate(currentDate) ?: DailyStats(date = currentDate, days = day, month = month)
    }

    suspend fun updateInstagramStats(reelsScrolled: Int = 0, adSkipped: Boolean = false, scrollInterval: Long = 0) {
        val today = currentDate
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(today)
        val day = getFormattedDay(date)
        val month = getFormattedMonth(date)
        var stats = dailyStatsDao.getStatsForDate(today) ?: DailyStats(date = today, days = day, month = month)

        // Ensure stats exist in database
        dailyStatsDao.insert(stats)

        // Update Instagram specific stats
        dailyStatsDao.updateInstagramStats(
            date = today,
            reelsCount = reelsScrolled,
            adsSkipped = if (adSkipped) 1 else 0
        )

        // Update total counts
        dailyStatsDao.updateTotalStats(
            date = today,
            reelsCount = reelsScrolled,
            adsSkipped = if (adSkipped) 1 else 0,
            timeSaved = if (adSkipped) DailyStats.AVERAGE_REEL_DURATION_SECONDS else 0,
            autoScrolled = reelsScrolled
        )
    }

    suspend fun updateYoutubeStats(reelsScrolled: Int = 0, adSkipped: Boolean = false, scrollInterval: Long = 0) {
        val today = currentDate
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(today)
        val day = getFormattedDay(date)
        val month = getFormattedMonth(date)
        var stats = dailyStatsDao.getStatsForDate(today) ?: DailyStats(date = today, days = day, month = month)
        // Ensure stats exist in database
        dailyStatsDao.insert(stats)

        // Update YouTube specific stats
        dailyStatsDao.updateYoutubeStats(
            date = today,
            reelsCount = reelsScrolled,
            adsSkipped = if (adSkipped) 1 else 0
        )

        // Update total counts
        dailyStatsDao.updateTotalStats(
            date = today,
            reelsCount = reelsScrolled,
            adsSkipped = if (adSkipped) 1 else 0,
            timeSaved = if (adSkipped) DailyStats.AVERAGE_REEL_DURATION_SECONDS else 0,
            autoScrolled = reelsScrolled
        )
    }

    suspend fun updateLinkedInStats(reelsScrolled: Int = 0, adSkipped: Boolean = false, scrollInterval: Long = 0) {
        val today = currentDate
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(today)
        val day = getFormattedDay(date)
        val month = getFormattedMonth(date)
        var stats = dailyStatsDao.getStatsForDate(today) ?: DailyStats(date = today, days = day, month = month)

        // Ensure stats exist in database
        dailyStatsDao.insert(stats)

        // Update LinkedIn specific stats
        dailyStatsDao.updateLinkedInStats(
            date = today,
            videosCount = reelsScrolled,
            adsSkipped = if (adSkipped) 1 else 0
        )

        // Update total counts
        dailyStatsDao.updateTotalStats(
            date = today,
            reelsCount = reelsScrolled,
            adsSkipped = if (adSkipped) 1 else 0,
            timeSaved = if (adSkipped) DailyStats.AVERAGE_REEL_DURATION_SECONDS else 0,
            autoScrolled = reelsScrolled
        )
    }

    suspend fun getAllStatsOrderedByDate(): List<DailyStats> {
        return dailyStatsDao.getAllStatsOrderedByDate()
    }

    suspend fun getAllTimeStats(): AllTimeStats {
        return try {
            AllTimeStats(
                totalReels = dailyStatsDao.getTotalReelsWatched(),
                totalAdsSkipped = dailyStatsDao.getTotalAdsSkipped(),
                totalTimeSaved = dailyStatsDao.getTotalTimeSaved()
            )
        } catch (e: Exception) {
            Log.e("StatsRepository", "Error getting all time stats: ${e.message}")
            AllTimeStats()
        }
    }
}

