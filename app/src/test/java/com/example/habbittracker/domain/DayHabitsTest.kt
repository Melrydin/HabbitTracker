package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitKind
import com.example.habbittracker.domain.model.HabitType
import com.example.habbittracker.domain.model.Recurrence
import com.example.habbittracker.domain.model.WeekSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The materialization of a day: which habits it holds and which it does not (F1, F8). */
class DayHabitsTest {
    private val monday = LocalDate.of(2026, 8, 31)
    private val saturday = monday.plusDays(5)

    private fun simple(id: Long, name: String = "Habit") =
        Habit(id = id, name = name, type = HabitType.CHECK, target = 1, icon = "task_alt")

    private fun weekly(
        id: Long,
        recurrence: Recurrence = Recurrence.EVERY_DAY,
        span: WeekSpan = WeekSpan.FULL,
        start: LocalDate = monday,
    ) = simple(id, "Week").copy(
        kind = HabitKind.WEEKLY,
        weekStart = start,
        weekSpan = span,
        recurrence = recurrence,
    )

    private fun sub(id: Long, parentId: Long, vararg dows: Int) =
        simple(id, "Sub").copy(kind = HabitKind.SUB, parentId = parentId, assignedDows = dows.toSet())

    private fun idsOn(date: LocalDate, habits: List<Habit>, progress: Map<Long, Int> = emptyMap()) =
        DayHabits.entriesFor(date, habits, progress).map { it.habit.id }

    @Test
    fun `a simple habit belongs to every day`() {
        assertEquals(listOf(1L), idsOn(monday, listOf(simple(1))))
        assertEquals(listOf(1L), idsOn(monday.plusWeeks(3), listOf(simple(1))))
    }

    @Test
    fun `a week habit only appears inside its own week`() {
        val habits = listOf(weekly(1))

        assertEquals(listOf(1L), idsOn(monday, habits))
        assertEquals(listOf(1L), idsOn(monday.plusDays(6), habits))
        assertTrue(idsOn(monday.minusDays(1), habits).isEmpty())
        assertTrue(idsOn(monday.plusDays(7), habits).isEmpty())
    }

    @Test
    fun `a workweek stops after friday`() {
        val habits = listOf(weekly(1, span = WeekSpan.WORKWEEK))

        assertEquals(listOf(1L), idsOn(monday.plusDays(4), habits))
        assertTrue(idsOn(saturday, habits).isEmpty())
    }

    @Test
    fun `a week habit run by its subs does not appear itself`() {
        val habits = listOf(weekly(1, recurrence = Recurrence.BY_SUBS), sub(2, parentId = 1, 1))

        assertEquals(listOf(2L), idsOn(monday, habits))
    }

    @Test
    fun `a sub habit appears on the weekdays it was assigned to`() {
        val habits = listOf(weekly(1, recurrence = Recurrence.BY_SUBS), sub(2, parentId = 1, 1, 3))

        assertEquals(listOf(2L), idsOn(monday, habits))
        assertTrue(idsOn(monday.plusDays(1), habits).isEmpty())
        assertEquals(listOf(2L), idsOn(monday.plusDays(2), habits))
    }

    @Test
    fun `a sub habit stays inside the week of its parent`() {
        val habits = listOf(weekly(1, recurrence = Recurrence.BY_SUBS), sub(2, parentId = 1, 1))

        // Same weekday, but the parent is bound to the week before.
        assertTrue(idsOn(monday.plusWeeks(1), habits).isEmpty())
    }

    @Test
    fun `a saturday sub of a workweek parent never materializes`() {
        val parent = weekly(1, recurrence = Recurrence.BY_SUBS, span = WeekSpan.WORKWEEK)
        val habits = listOf(parent, sub(2, parentId = 1, 6))

        assertTrue(idsOn(saturday, habits).isEmpty())
    }

    @Test
    fun `archiving the week takes its subs out of the coming days`() {
        val parent = weekly(1, recurrence = Recurrence.BY_SUBS).copy(archived = true)
        val habits = listOf(parent, sub(2, parentId = 1, 1))

        assertTrue(idsOn(monday, habits).isEmpty())
    }

    @Test
    fun `a recorded value keeps a habit on the day whatever the rules say now`() {
        val habits = listOf(weekly(1, start = monday.plusWeeks(1)))

        // The week was moved after the fact; the finished day must not change.
        assertEquals(listOf(1L), idsOn(monday, habits, progress = mapOf(1L to 1)))
    }

    @Test
    fun `an archived habit disappears from a day it was never tracked on`() {
        val habits = listOf(simple(1).copy(archived = true))

        assertTrue(idsOn(monday, habits).isEmpty())
        assertEquals(listOf(1L), idsOn(monday, habits, progress = mapOf(1L to 1)))
    }

    @Test
    fun `a theme habit stays on the day it was created for`() {
        val habits = listOf(simple(1).copy(isThemeGenerated = true))

        assertTrue(idsOn(monday, habits).isEmpty())
        assertEquals(listOf(1L), idsOn(monday, habits, progress = mapOf(1L to 0)))
    }

    @Test
    fun `a paused habit is not asked for`() {
        val habits = listOf(simple(1))

        assertTrue(DayHabits.entriesFor(monday, habits, emptyMap(), pausedHabits = setOf(1L)).isEmpty())
    }
}
