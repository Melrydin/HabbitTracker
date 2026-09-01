package com.example.habbittracker.data

import com.example.habbittracker.data.local.ReminderDao
import com.example.habbittracker.data.local.toDomain
import com.example.habbittracker.data.local.toEntity
import com.example.habbittracker.data.reminder.ReminderScheduler
import com.example.habbittracker.domain.model.Reminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Stores reminders and keeps the scheduler in step (F5).
 *
 * Saving and arming happen together on purpose: a reminder that exists in the
 * database but was never booked would simply never fire, and nothing would say so.
 */
class RoomReminderRepository(
    private val dao: ReminderDao,
    private val scheduler: ReminderScheduler,
) : ReminderRepository {
    override fun observeAll(): Flow<List<Reminder>> =
        dao.observeAll().map { reminders -> reminders.map { it.toDomain() } }

    override suspend fun get(id: Long): Reminder? = dao.getById(id)?.toDomain()

    override suspend fun upsert(reminder: Reminder): Long {
        val id = dao.upsert(reminder.toEntity())
        val stored = if (reminder.id == 0L) reminder.copy(id = id) else reminder
        scheduler.schedule(stored)
        return stored.id
    }

    override suspend fun delete(id: Long) {
        dao.getById(id)?.let { dao.delete(it) }
        scheduler.cancel(id)
    }

    override suspend fun rescheduleAll() {
        dao.getAll().forEach { scheduler.schedule(it.toDomain()) }
    }
}
