package com.example.habbittracker.data

import com.example.habbittracker.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

/**
 * Local reminders (F5). Storing one also schedules it, so a caller cannot save a
 * reminder and forget to arm it.
 */
interface ReminderRepository {
    fun observeAll(): Flow<List<Reminder>>

    suspend fun get(id: Long): Reminder?

    suspend fun upsert(reminder: Reminder): Long

    suspend fun delete(id: Long)

    /** Arms everything again, after a reboot or an app update. */
    suspend fun rescheduleAll()
}
