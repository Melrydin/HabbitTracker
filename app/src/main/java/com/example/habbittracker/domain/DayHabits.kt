package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitEntry

/**
 * Which habits belong to a given day (F1, F3).
 *
 * A pure function shared by every repository implementation, so that storage
 * never gets to reinvent the rule.
 *
 * The keys of `progressByHabitId` carry meaning of their own: a key present with
 * a value of zero means the day has a row for that habit, which is not the same
 * as the habit having no row at all.
 */
object DayHabits {
    fun entriesFor(
        habits: List<Habit>,
        progressByHabitId: Map<Long, Int>,
        pausedHabits: Set<Long> = emptySet(),
    ): List<HabitEntry> =
        habits
            // A paused habit is not asked for that day, so it leaves the goal alone (F4).
            .filterNot { it.id in pausedHabits }
            .filter { it.belongsTo(progressByHabitId) }
            .map { habit -> HabitEntry(habit, progressByHabitId[habit.id] ?: 0) }

    /**
     * A habit generated from a day theme is day-local (F8): it exists for the one
     * day it was created for and must not turn up on every other day.
     *
     * An archived habit stays on days that already have a value recorded, so
     * archiving cannot rewrite a day that had passed, but disappears from new ones.
     */
    private fun Habit.belongsTo(progressByHabitId: Map<Long, Int>): Boolean =
        when {
            isThemeGenerated -> progressByHabitId.containsKey(id)
            archived -> (progressByHabitId[id] ?: 0) > 0
            else -> true
        }
}
