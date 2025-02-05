package com.whatsapp.testing.database

import androidx.core.text.util.LocalePreferences.FirstDayOfWeek.Days
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")

data class DailyStats(
    @PrimaryKey
    val date: String, // Format: yyyy-MM-dd
    val days: String,
    val month:String,
    var reelsWatched: Int = 0,
    var adsSkipped: Int = 0,
    var timeSavedInSeconds: Long = 0,
    var totalScrolls: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var autoScrolledReels: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var instagramReelsWatched: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var youtubeReelsWatched: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var snapchatStoriesWatched: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var linkedInVideosWatched: Int = 0,  // New field
    @ColumnInfo(defaultValue = "0")
    var instagramAdsSkipped: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var youtubeAdsSkipped: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var linkedInAdsSkipped: Int = 0, // New field
    @ColumnInfo(defaultValue = "0")
    var snapchatStoriesAutoScrolled: Int = 0,  // New field for tracking auto-scrolled stories
    @ColumnInfo(defaultValue = "0")
    var snapchatTimeSpentSeconds: Long = 0

) {
    companion object {
        const val AVERAGE_REEL_DURATION_SECONDS = 30L
    }
}