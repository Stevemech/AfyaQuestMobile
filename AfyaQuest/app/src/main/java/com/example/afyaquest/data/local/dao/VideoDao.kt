package com.example.afyaquest.data.local.dao

import androidx.room.*
import com.example.afyaquest.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Video operations.
 */
@Dao
interface VideoDao {
    @Query("SELECT * FROM videos WHERE id = :videoId")
    suspend fun getVideoById(videoId: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE id = :videoId")
    fun getVideoByIdFlow(videoId: String): Flow<VideoEntity?>

    @Query("SELECT * FROM videos WHERE category = :category ORDER BY `order` ASC")
    suspend fun getVideosByCategory(category: String): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE category = :category ORDER BY `order` ASC")
    fun getVideosByCategoryFlow(category: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isDownloaded = 1")
    suspend fun getDownloadedVideos(): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE isDownloaded = 1")
    fun getDownloadedVideosFlow(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY category, `order` ASC")
    suspend fun getAllVideos(): List<VideoEntity>

    @Query("SELECT * FROM videos ORDER BY category, `order` ASC")
    fun getAllVideosFlow(): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("UPDATE videos SET isDownloaded = :isDownloaded, localFilePath = :localFilePath WHERE id = :videoId")
    suspend fun updateVideoDownloadStatus(videoId: String, isDownloaded: Boolean, localFilePath: String?)

    @Delete
    suspend fun deleteVideo(video: VideoEntity)

    @Query("DELETE FROM videos")
    suspend fun deleteAllVideos()
}
