package com.example.habbittracker.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DayHabitDao {
    @Query("SELECT * FROM day_habits WHERE date = :date")
    fun observeForDate(date: LocalDate): Flow<List<DayHabitEntity>>

    @Query("SELECT * FROM day_habits WHERE date = :date")
    suspend fun getForDate(date: LocalDate): List<DayHabitEntity>

    /** Every recorded value, used when a change forces a recalculation of all days. */
    @Query("SELECT * FROM day_habits")
    suspend fun getAll(): List<DayHabitEntity>

    @Upsert
    suspend fun upsert(entry: DayHabitEntity)
}
