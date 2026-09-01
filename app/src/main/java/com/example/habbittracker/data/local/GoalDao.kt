package com.example.habbittracker.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Long term goals (F10, V3). The table exists from the MVP on so that F10 needs
 * no migration; only reads are defined until the feature lands.
 */
@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE habit_id = :habitId")
    fun observeForHabit(habitId: Long): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals")
    suspend fun getAll(): List<GoalEntity>

    @Upsert
    suspend fun upsertAll(goals: List<GoalEntity>)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}
