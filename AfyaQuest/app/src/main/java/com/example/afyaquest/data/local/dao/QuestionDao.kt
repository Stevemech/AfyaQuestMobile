package com.example.afyaquest.data.local.dao

import androidx.room.*
import com.example.afyaquest.data.local.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Question operations.
 */
@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE id = :questionId")
    suspend fun getQuestionById(questionId: String): QuestionEntity?

    @Query("SELECT * FROM questions WHERE date = :date ORDER BY `order` ASC")
    suspend fun getQuestionsByDate(date: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE date = :date ORDER BY `order` ASC")
    fun getQuestionsByDateFlow(date: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE category = :category")
    suspend fun getQuestionsByCategory(category: String): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Delete
    suspend fun deleteQuestion(question: QuestionEntity)

    @Query("DELETE FROM questions WHERE date < :date")
    suspend fun deleteQuestionsOlderThan(date: String)

    @Query("DELETE FROM questions")
    suspend fun deleteAllQuestions()
}
