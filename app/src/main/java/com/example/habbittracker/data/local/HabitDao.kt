package com.example.habbittracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    /** Every habit including archived ones, ordered so the list stays stable. */
    @Query("SELECT * FROM habits ORDER BY archived ASC, id ASC")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY archived ASC, id ASC")
    suspend fun getAll(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: Long): HabitEntity?

    /** Inserts when the id is 0 and updates otherwise; returns the stored id. */
    @Upsert
    suspend fun upsert(habit: HabitEntity): Long

    @Query("UPDATE habits SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Delete
    suspend fun delete(habit: HabitEntity)

    /** Restore only. Parents must be inserted before their sub habits. */
    @Insert
    suspend fun insertAll(habits: List<HabitEntity>)

    @Query("DELETE FROM habits")
    suspend fun deleteAll()
}
