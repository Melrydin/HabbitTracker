package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitEntry

/**
 * Which habits belong to a given day (F1, F3).
 *
 * A pure function shared by every repository implementation, so that storage
 * never gets to reinvent the rule.
 */
object DayHabits {
    /**
     * Every active habit, plus archived ones that already have a value recorded on
     * that day. An archived habit therefore disappears from new days while older
     * entries stay visible, and archiving cannot rewrite a day that already passed.
     */
    fun entriesFor(habits: List<Habit>, progressByHabitId: Map<Long, Int>): List<HabitEntry> =
        habits
            .filter { !it.archived || (progressByHabitId[it.id] ?: 0) > 0 }
            .map { habit -> HabitEntry(habit, progressByHabitId[habit.id] ?: 0) }
}
