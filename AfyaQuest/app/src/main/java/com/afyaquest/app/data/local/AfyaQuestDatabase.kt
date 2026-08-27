package com.afyaquest.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.afyaquest.app.data.local.converters.DateConverter
import com.afyaquest.app.data.local.converters.StringListConverter
import com.afyaquest.app.data.local.dao.*
import com.afyaquest.app.data.local.entity.*

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
        AchievementEntity::class,
        PendingReportEntity::class,
        PendingQuizEntity::class,
        PendingChatEntity::class,
        PendingClientVisitEntity::class,
        CaseLogEntity::class
    ],
    version = 5,
    exportSchema = true
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
    abstract fun pendingSyncDao(): PendingSyncDao
    abstract fun caseLogDao(): CaseLogDao

    companion object {
        const val DATABASE_NAME = "afyaquest_database"

        /**
         * v4 -> v5: add the `case_logs` table for the Emergency Response Guide.
         * Non-destructive — only creates the new table, preserving all existing
         * offline data. SQL is taken verbatim from Room's exported schema
         * (schemas/.../5.json) so it matches the generated TableInfo exactly.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `case_logs` (" +
                        "`id` TEXT NOT NULL, " +
                        "`userId` TEXT NOT NULL, " +
                        "`treeVersion` TEXT NOT NULL, " +
                        "`language` TEXT NOT NULL, " +
                        "`mechanism` TEXT, " +
                        "`dispositionId` TEXT NOT NULL, " +
                        "`dispositionLevel` TEXT NOT NULL, " +
                        "`flagsJson` TEXT NOT NULL, " +
                        "`pathJson` TEXT NOT NULL, " +
                        "`completedAt` INTEGER NOT NULL, " +
                        "`isSynced` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
            }
        }
    }
}
