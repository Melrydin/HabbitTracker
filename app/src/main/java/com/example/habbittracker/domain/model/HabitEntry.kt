package com.example.habbittracker.domain.model

/** A habit of the current day together with its recorded value (F3). */
data class HabitEntry(
    val habit: Habit,
    val progress: Int,
) {
    /** What counts as done depends on the polarity of the habit (F2, F11). */
    val fulfilled: Boolean get() = habit.isFulfilledBy(progress)

    /** A habit to avoid is not "open" but slipped, which no later tap can undo (F11). */
    val open: Boolean get() = !fulfilled && habit.polarity == Polarity.GOOD
}
