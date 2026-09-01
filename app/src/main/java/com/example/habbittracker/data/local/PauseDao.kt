package com.example.habbittracker.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Pause and holiday periods (F4). */
@Dao
interface PauseDao {
    @Query("SELECT * FROM pauses")
    fun observeAll(): Flow<List<PauseEntity>>

    @Query("SELECT * FROM pauses")
    suspend fun getAll(): List<PauseEntity>

    @Upsert
    suspend fun upsert(pause: PauseEntity): Long

    @Query("DELETE FROM pauses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pauses")
    suspend fun deleteAll()
}
