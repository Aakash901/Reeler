package com.whatsapp.testing.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DailyStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stats: DailyStats)

    @Update
    suspend fun update(stats: DailyStats)

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getStatsForDate(date: String): DailyStats?

    @Query("SELECT * FROM daily_stats ORDER BY date DESC")
    suspend fun getAllStats(): List<DailyStats>

    @Query("SELECT COALESCE(SUM(reelsWatched), 0) as total FROM daily_stats")
    suspend fun getTotalReelsWatched(): Int

    @Query("SELECT COALESCE(SUM(adsSkipped), 0) as total FROM daily_stats")
    suspend fun getTotalAdsSkipped(): Int

    @Query("SELECT COALESCE(SUM(timeSavedInSeconds), 0) as total FROM daily_stats")
    suspend fun getTotalTimeSaved(): Long

    @Query("SELECT * FROM daily_stats ORDER BY date DESC")
    suspend fun getAllStatsOrderedByDate(): List<DailyStats>

    @Query("""
        UPDATE daily_stats SET 
        reelsWatched = reelsWatched + :reelsCount,
        adsSkipped = adsSkipped + :adsSkipped,
        timeSavedInSeconds = timeSavedInSeconds + :timeSaved,
        autoScrolledReels = autoScrolledReels + :autoScrolled
        WHERE date = :date
    """)
    suspend fun updateTotalStats(
        date: String,
        reelsCount: Int,
        adsSkipped: Int,
        timeSaved: Long,
        autoScrolled: Int
    )

    @Query("UPDATE daily_stats SET instagramReelsWatched = instagramReelsWatched + :reelsCount, " +
            "instagramAdsSkipped = instagramAdsSkipped + :adsSkipped WHERE date = :date")
    suspend fun updateInstagramStats(date: String, reelsCount: Int, adsSkipped: Int)

    @Query("UPDATE daily_stats SET youtubeReelsWatched = youtubeReelsWatched + :reelsCount, " +
            "youtubeAdsSkipped = youtubeAdsSkipped + :adsSkipped WHERE date = :date")
    suspend fun updateYoutubeStats(date: String, reelsCount: Int, adsSkipped: Int)

    @Query("""
        UPDATE daily_stats SET 
        linkedInVideosWatched = linkedInVideosWatched + :videosCount,
        linkedInAdsSkipped = linkedInAdsSkipped + :adsSkipped 
        WHERE date = :date
    """)
    suspend fun updateLinkedInStats(date: String, videosCount: Int, adsSkipped: Int)
    @Query("""
        UPDATE daily_stats SET 
        snapchatStoriesWatched = snapchatStoriesWatched + :storiesCount,
        snapchatStoriesAutoScrolled = snapchatStoriesAutoScrolled + :autoScrolled,
        snapchatTimeSpentSeconds = snapchatTimeSpentSeconds + :timeSpent
        WHERE date = :date
    """)
    suspend fun updateSnapchatStats(
        date: String,
        storiesCount: Int,
        autoScrolled: Int,
        timeSpent: Long
    )
    @Query("SELECT COALESCE(SUM(snapchatStoriesWatched), 0) FROM daily_stats")
    suspend fun getTotalSnapchatStoriesWatched(): Int

    @Query("SELECT COALESCE(SUM(snapchatTimeSpentSeconds), 0) FROM daily_stats")
    suspend fun getTotalSnapchatTimeSpent(): Long
}