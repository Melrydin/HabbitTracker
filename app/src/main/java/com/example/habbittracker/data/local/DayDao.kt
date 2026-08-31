package com.example.habbittracker.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DayDao {
    @Query("SELECT * FROM days WHERE date = :date")
    fun observe(date: LocalDate): Flow<DayEntity?>

    @Query("SELECT * FROM days WHERE date = :date")
    suspend fun get(date: LocalDate): DayEntity?

    @Query("SELECT * FROM days")
    suspend fun getAll(): List<DayEntity>

    /** Only the passed days, which is all the streak calculation needs (F4). */
    @Query("SELECT date FROM days WHERE passed = 1")
    fun observePassedDates(): Flow<List<LocalDate>>

    @Upsert
    suspend fun upsert(day: DayEntity)

    @Upsert
    suspend fun upsertAll(days: List<DayEntity>)
}
