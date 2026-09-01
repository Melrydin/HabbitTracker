package com.example.habbittracker.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.habbittracker.domain.model.DayStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Just the status of a day, which is all streaks and statistics need (F4). */
data class DayStatusRow(val date: LocalDate, val status: DayStatus)

@Dao
interface DayDao {
    @Query("SELECT * FROM days WHERE date = :date")
    fun observe(date: LocalDate): Flow<DayEntity?>

    @Query("SELECT * FROM days WHERE date = :date")
    suspend fun get(date: LocalDate): DayEntity?

    @Query("SELECT * FROM days")
    suspend fun getAll(): List<DayEntity>

    @Query("SELECT date, status FROM days")
    fun observeStatuses(): Flow<List<DayStatusRow>>

    @Query("SELECT date FROM days WHERE freeze_used = 1")
    fun observeFrozen(): Flow<List<LocalDate>>

    @Upsert
    suspend fun upsert(day: DayEntity)

    @Upsert
    suspend fun upsertAll(days: List<DayEntity>)

    @Query("DELETE FROM days")
    suspend fun deleteAll()

    /** Clears the theme link of every day that pointed at a habit that is now gone. */
    @Query("UPDATE days SET theme_habit_id = NULL WHERE theme_habit_id = :habitId")
    suspend fun clearThemeHabit(habitId: Long)
}
