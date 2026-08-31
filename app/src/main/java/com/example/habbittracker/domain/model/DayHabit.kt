package com.example.habbittracker.domain.model

import java.time.LocalDate

/**
 * Which habit applies on which day, together with its recorded value (F3).
 * The key is the pair of [date] and [habitId].
 */
data class DayHabit(
    val date: LocalDate,
    val habitId: Long,
    val progress: Int = 0,
)

/** A habit of the current day together with its recorded value. */
data class HabitEntry(
    val habit: Habit,
    val progress: Int,
) {
    /** Fulfilled as soon as `progress >= target` (F2). */
    val fulfilled: Boolean get() = progress >= habit.target
}
