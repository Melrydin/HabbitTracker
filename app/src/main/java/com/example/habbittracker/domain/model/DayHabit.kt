package com.example.habbittracker.domain.model

import java.time.LocalDate

/**
 * Welcher Habit an welchem Tag gilt, plus Ist-Wert (F3).
 * Schluessel ist das Paar aus [date] und [habitId].
 */
data class DayHabit(
    val date: LocalDate,
    val habitId: Long,
    val progress: Int = 0,
)

/** Ein Habit dieses Tages zusammen mit seinem Ist-Wert. */
data class HabitEntry(
    val habit: Habit,
    val progress: Int,
) {
    /** Erfuellt, sobald `progress >= target` (F2). */
    val fulfilled: Boolean get() = progress >= habit.target
}
