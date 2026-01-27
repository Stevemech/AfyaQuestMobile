package com.example.afyaquest.di

import android.content.Context
import androidx.room.Room
import com.example.afyaquest.data.local.AfyaQuestDatabase
import com.example.afyaquest.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database and DAO dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AfyaQuestDatabase {
        return Room.databaseBuilder(
            context,
            AfyaQuestDatabase::class.java,
            AfyaQuestDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AfyaQuestDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideLessonDao(database: AfyaQuestDatabase): LessonDao {
        return database.lessonDao()
    }

    @Provides
    @Singleton
    fun provideVideoDao(database: AfyaQuestDatabase): VideoDao {
        return database.videoDao()
    }

    @Provides
    @Singleton
    fun provideQuestionDao(database: AfyaQuestDatabase): QuestionDao {
        return database.questionDao()
    }

    @Provides
    @Singleton
    fun provideReportDao(database: AfyaQuestDatabase): ReportDao {
        return database.reportDao()
    }

    @Provides
    @Singleton
    fun provideChatMessageDao(database: AfyaQuestDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }

    @Provides
    @Singleton
    fun provideClientHouseDao(database: AfyaQuestDatabase): ClientHouseDao {
        return database.clientHouseDao()
    }

    @Provides
    @Singleton
    fun provideHealthFacilityDao(database: AfyaQuestDatabase): HealthFacilityDao {
        return database.healthFacilityDao()
    }

    @Provides
    @Singleton
    fun provideProgressDao(database: AfyaQuestDatabase): ProgressDao {
        return database.progressDao()
    }

    @Provides
    @Singleton
    fun provideAchievementDao(database: AfyaQuestDatabase): AchievementDao {
        return database.achievementDao()
    }
}
