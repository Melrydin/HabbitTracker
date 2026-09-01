package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.Pause
import java.time.LocalDate

/**
 * Pause and holiday periods (F4).
 *
 * A pause without a habit stops the whole app for those days; one with a habit
 * stops only that habit. Either way the days are not held against the user: they
 * count as neutral and a streak steps over them.
 */
object Pauses {
    /** True while the whole app is paused on [date]. */
    fun isPaused(pauses: List<Pause>, date: LocalDate): Boolean =
        pauses.any { it.habitId == null && it.covers(date) }

    /** The habits that are individually paused on [date]. */
    fun pausedHabits(pauses: List<Pause>, date: LocalDate): Set<Long> =
        pauses.mapNotNull { pause -> pause.habitId?.takeIf { pause.covers(date) } }.toSet()

    /** Both ends belong to the period, which is how a holiday reads. */
    private fun Pause.covers(date: LocalDate): Boolean = date >= from && date <= to
}
