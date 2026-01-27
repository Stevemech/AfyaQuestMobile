package com.example.afyaquest.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.afyaquest.data.local.converters.DateConverter
import com.example.afyaquest.data.local.converters.StringListConverter
import com.example.afyaquest.data.local.dao.*
import com.example.afyaquest.data.local.entity.*

/**
 * Main Room database for Afya Quest application.
 * Contains all entities and provides DAOs for data access.
 */
@Database(
    entities = [
        UserEntity::class,
        LessonEntity::class,
        VideoEntity::class,
        QuestionEntity::class,
        ReportEntity::class,
        ChatMessageEntity::class,
        ClientHouseEntity::class,
        HealthFacilityEntity::class,
        ProgressEntity::class,
        AchievementEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class, StringListConverter::class)
abstract class AfyaQuestDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun lessonDao(): LessonDao
    abstract fun videoDao(): VideoDao
    abstract fun questionDao(): QuestionDao
    abstract fun reportDao(): ReportDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun clientHouseDao(): ClientHouseDao
    abstract fun healthFacilityDao(): HealthFacilityDao
    abstract fun progressDao(): ProgressDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        const val DATABASE_NAME = "afyaquest_database"
    }
}
