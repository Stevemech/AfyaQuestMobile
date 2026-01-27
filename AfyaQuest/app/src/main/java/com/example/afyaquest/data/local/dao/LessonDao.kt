package com.example.afyaquest.data.local.dao

import androidx.room.*
import com.example.afyaquest.data.local.entity.LessonEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Lesson operations.
 */
@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE id = :lessonId")
    suspend fun getLessonById(lessonId: String): LessonEntity?

    @Query("SELECT * FROM lessons WHERE id = :lessonId")
    fun getLessonByIdFlow(lessonId: String): Flow<LessonEntity?>

    @Query("SELECT * FROM lessons WHERE category = :category AND isPublished = 1 ORDER BY `order` ASC")
    suspend fun getLessonsByCategory(category: String): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE category = :category AND isPublished = 1 ORDER BY `order` ASC")
    fun getLessonsByCategoryFlow(category: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE isPublished = 1 ORDER BY category, `order` ASC")
    suspend fun getAllLessons(): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE isPublished = 1 ORDER BY category, `order` ASC")
    fun getAllLessonsFlow(): Flow<List<LessonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)

    @Update
    suspend fun updateLesson(lesson: LessonEntity)

    @Delete
    suspend fun deleteLesson(lesson: LessonEntity)

    @Query("DELETE FROM lessons")
    suspend fun deleteAllLessons()
}
