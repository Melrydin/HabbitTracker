package com.example.habbittracker.domain.model

import java.time.LocalTime

/**
 * A local reminder (F5). Nothing leaves the device; these become notifications
 * scheduled by the app itself.
 *
 * A null [habitId] makes it a general nudge to record the day. [daysOfWeek] holds
 * ISO weekdays, 1 for Monday through 7 for Sunday, matching `assignedDows` in
 * [Habit].
 */
data class Reminder(
    val id: Long = 0,
    val time: LocalTime,
    val daysOfWeek: Set<Int> = ALL_DAYS,
    val habitId: Long? = null,
    val enabled: Boolean = true,
) {
    init {
        require(daysOfWeek.all { it in 1..7 }) { "daysOfWeek holds weekdays 1..7" }
    }

    /** A reminder without a day never fires, which is worth telling the user. */
    val fires: Boolean get() = enabled && daysOfWeek.isNotEmpty()

    companion object {
        val ALL_DAYS = (1..7).toSet()
    }
}
