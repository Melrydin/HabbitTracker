package com.example.habbittracker.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Pause and holiday periods (F4, V2). The table exists from the MVP on so that
 * the feature needs no migration; only reads are defined until it lands.
 */
@Dao
interface PauseDao {
    @Query("SELECT * FROM pauses")
    fun observeAll(): Flow<List<PauseEntity>>

    @Query("SELECT * FROM pauses")
    suspend fun getAll(): List<PauseEntity>

    @Query("DELETE FROM pauses")
    suspend fun deleteAll()
}
