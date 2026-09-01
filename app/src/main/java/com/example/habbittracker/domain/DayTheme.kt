package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitEntry

/**
 * Which habit gives a day its theme (F8). Exactly one wins, or none.
 *
 * A theme is always backed by a habit, so it counts towards the daily goal like
 * anything else instead of being a label on the side.
 */
object DayTheme {
    /**
     * A choice made by hand always wins, because it is the explicit one. Without
     * one, a single habit offering a theme takes it; several and the day stays
     * without a theme until the user picks.
     */
    fun of(chosenHabitId: Long?, entries: List<HabitEntry>): Habit? {
        val chosen = entries.firstOrNull { it.habit.id == chosenHabitId }?.habit
        return chosen ?: candidates(entries).singleOrNull()
    }

    /** The habits offering a theme on that day, in the order they appear. */
    fun candidates(entries: List<HabitEntry>): List<Habit> =
        entries.map { it.habit }.filter { it.givesTheme }

    /** The choice the user still has to make, empty while the day decides on its own. */
    fun openChoice(chosenHabitId: Long?, entries: List<HabitEntry>): List<Habit> =
        if (of(chosenHabitId, entries) != null) emptyList() else candidates(entries)
}
