package com.example.habbittracker.domain.model

/** A habit of the current day together with its recorded value (F3). */
data class HabitEntry(
    val habit: Habit,
    val progress: Int,
) {
    /** Fulfilled as soon as `progress >= target` (F2). */
    val fulfilled: Boolean get() = progress >= habit.target
}
