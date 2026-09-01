package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.Reminder
import java.time.LocalDateTime

/**
 * When a reminder is next due (F5).
 *
 * A pure function so the awkward parts — today's slot already gone, a reminder
 * that only runs on one weekday, one that runs on none — are settled in a test
 * rather than against a real clock.
 */
object ReminderSchedule {
    /** The first moment at or after [from] that [reminder] fires, or null if it never does. */
    fun nextOccurrence(reminder: Reminder, from: LocalDateTime): LocalDateTime? {
        if (!reminder.fires) return null
        return (0..DAYS_IN_WEEK)
            .map { from.toLocalDate().plusDays(it.toLong()).atTime(reminder.time) }
            .firstOrNull { it >= from && it.dayOfWeek.value in reminder.daysOfWeek }
    }

    private const val DAYS_IN_WEEK = 7
}
