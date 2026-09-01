package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitEntry
import com.example.habbittracker.domain.model.HabitKind
import com.example.habbittracker.domain.model.Recurrence
import com.example.habbittracker.domain.model.WeekSpan
import java.time.LocalDate

/**
 * Which habits belong to a given day (F1, F3, F8).
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
        date: LocalDate,
        habits: List<Habit>,
        progressByHabitId: Map<Long, Int>,
        pausedHabits: Set<Long> = emptySet(),
    ): List<HabitEntry> {
        val weeks = habits.filter { it.kind == HabitKind.WEEKLY }.associateBy { it.id }
        return habits
            // A paused habit is not asked for that day, so it leaves the goal alone (F4).
            .filterNot { it.id in pausedHabits }
            .filter { it.belongsTo(date, progressByHabitId, weeks) }
            .map { habit -> HabitEntry(habit, progressByHabitId[habit.id] ?: 0) }
    }

    /**
     * A habit generated from a day theme is day-local (F8): it exists for the one
     * day it was created for and must not turn up on every other day.
     *
     * An archived habit stays on days that already have a value recorded, so
     * archiving cannot rewrite a day that had passed, but disappears from new ones.
     * A recorded value holds a habit on the day whatever the rules say now — an
     * edited week must not rewrite a day that had already passed either.
     */
    private fun Habit.belongsTo(date: LocalDate, progressByHabitId: Map<Long, Int>, weeks: Map<Long, Habit>): Boolean =
        when {
            (progressByHabitId[id] ?: 0) > 0 -> true
            isThemeGenerated -> progressByHabitId.containsKey(id)
            archived -> false
            else -> appearsOn(date, weeks)
        }

    /**
     * The materialization of F8: a weekly habit only shows up inside the week it is
     * bound to, and a sub habit only on the weekdays it was assigned within its
     * parent's week. A weekly habit that runs `BY_SUBS` never appears itself.
     */
    private fun Habit.appearsOn(date: LocalDate, weeks: Map<Long, Habit>): Boolean =
        when (kind) {
            HabitKind.SIMPLE -> true
            HabitKind.WEEKLY -> recurrence != Recurrence.BY_SUBS && covers(date)
            HabitKind.SUB -> date.dayOfWeek.value in assignedDows && weeks[parentId]?.materializes(date) == true
        }

    /** An archived week stops producing days, exactly as an archived habit does. */
    private fun Habit.materializes(date: LocalDate): Boolean = !archived && covers(date)

    /** The week a [HabitKind.WEEKLY] habit is bound to, Monday to Friday or to Sunday. */
    private fun Habit.covers(date: LocalDate): Boolean {
        val start = weekStart ?: return false
        val days = if (weekSpan == WeekSpan.WORKWEEK) WORKWEEK_DAYS else FULL_WEEK_DAYS
        return !date.isBefore(start) && date.isBefore(start.plusDays(days))
    }

    private const val WORKWEEK_DAYS = 5L
    private const val FULL_WEEK_DAYS = 7L
}
