package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderScheduleTest {
    // A Monday.
    private val monday = LocalDateTime.of(2026, 8, 31, 9, 0)

    private fun reminder(hour: Int, days: Set<Int> = Reminder.ALL_DAYS, enabled: Boolean = true) =
        Reminder(time = LocalTime.of(hour, 0), daysOfWeek = days, enabled = enabled)

    @Test
    fun `a slot later today is the next one`() {
        assertEquals(
            LocalDateTime.of(2026, 8, 31, 20, 0),
            ReminderSchedule.nextOccurrence(reminder(20), monday),
        )
    }

    @Test
    fun `a slot already gone moves to tomorrow`() {
        assertEquals(
            LocalDateTime.of(2026, 9, 1, 8, 0),
            ReminderSchedule.nextOccurrence(reminder(8), monday),
        )
    }

    @Test
    fun `the slot exactly now still counts`() {
        assertEquals(monday, ReminderSchedule.nextOccurrence(reminder(9), monday))
    }

    @Test
    fun `a weekday only reminder skips to that weekday`() {
        // Saturday is day 6.
        assertEquals(
            LocalDateTime.of(2026, 9, 5, 8, 0),
            ReminderSchedule.nextOccurrence(reminder(8, days = setOf(6)), monday),
        )
    }

    @Test
    fun `a reminder on today's weekday whose time has passed waits a week`() {
        assertEquals(
            LocalDateTime.of(2026, 9, 7, 8, 0),
            ReminderSchedule.nextOccurrence(reminder(8, days = setOf(1)), monday),
        )
    }

    @Test
    fun `a reminder without a weekday never fires`() {
        assertNull(ReminderSchedule.nextOccurrence(reminder(20, days = emptySet()), monday))
    }

    @Test
    fun `a disabled reminder never fires`() {
        assertNull(ReminderSchedule.nextOccurrence(reminder(20, enabled = false), monday))
    }
}
