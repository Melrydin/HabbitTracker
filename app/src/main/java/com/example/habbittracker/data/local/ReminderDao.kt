package com.example.habbittracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY time ASC, id ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY time ASC, id ASC")
    suspend fun getAll(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Upsert
    suspend fun upsert(reminder: ReminderEntity): Long

    @Delete
    suspend fun delete(reminder: ReminderEntity)
}
