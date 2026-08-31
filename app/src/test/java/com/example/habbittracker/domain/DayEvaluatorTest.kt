package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.Day
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitEntry
import com.example.habbittracker.domain.model.HabitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DayEvaluatorTest {

    private val date = LocalDate.of(2026, 8, 31)

    private fun check(id: Long, points: Int = 1, required: Boolean = false) =
        Habit(id, "Habit $id", HabitType.CHECK, target = 1, points = points, required = required, icon = "task_alt")

    private fun counter(id: Long, target: Int, points: Int = 1) =
        Habit(id, "Habit $id", HabitType.COUNTER, target = target, unit = "x", points = points, icon = "task_alt")

    @Test
    fun `points ziel ist bestanden wenn die summe die schwelle erreicht`() {
        val day = Day(date, goalType = GoalType.POINTS, goalThreshold = 5)
        val entries = listOf(
            HabitEntry(check(1, points = 3), progress = 1),
            HabitEntry(check(2, points = 2), progress = 1),
            HabitEntry(check(3, points = 4), progress = 0),
        )

        val result = DayEvaluator.evaluate(day, entries)

        assertEquals(5, result.current)
        assertTrue(result.passed)
    }

    @Test
    fun `punkte zaehlen erst wenn der habit erfuellt ist`() {
        val day = Day(date, goalType = GoalType.POINTS, goalThreshold = 2)
        // 7 von 8 reicht nicht: erfuellt ist erst progress >= target.
        val entries = listOf(HabitEntry(counter(1, target = 8, points = 2), progress = 7))

        val result = DayEvaluator.evaluate(day, entries)

        assertEquals(0, result.current)
        assertFalse(result.passed)
    }

    @Test
    fun `zaehler darf sein ziel ueberschreiten und bleibt erfuellt`() {
        val day = Day(date, goalType = GoalType.MIN_COUNT, goalThreshold = 1)
        val entries = listOf(HabitEntry(counter(1, target = 8), progress = 12))

        assertTrue(DayEvaluator.evaluate(day, entries).passed)
    }

    @Test
    fun `all required ignoriert habits ohne pflicht`() {
        val day = Day(date, goalType = GoalType.ALL_REQUIRED)
        val entries = listOf(
            HabitEntry(check(1, required = true), progress = 1),
            HabitEntry(check(2, required = true), progress = 1),
            HabitEntry(check(3, required = false), progress = 0),
        )

        val result = DayEvaluator.evaluate(day, entries)

        assertEquals(2, result.current)
        assertEquals(2, result.threshold)
        assertTrue(result.passed)
    }

    @Test
    fun `all required ohne pflicht habits ist neutral statt bestanden`() {
        val day = Day(date, goalType = GoalType.ALL_REQUIRED)
        val entries = listOf(HabitEntry(check(1), progress = 1))

        val result = DayEvaluator.evaluate(day, entries)

        assertFalse(result.passed)
        assertTrue(result.isNeutral)
    }

    @Test
    fun `leerer tag ist neutral und nicht bestanden`() {
        val day = Day(date, goalType = GoalType.POINTS, goalThreshold = 6)
        val result = DayEvaluator.evaluate(day, entries = emptyList())

        assertFalse(result.passed)
        assertEquals(0f, result.fraction, 0f)
    }

    @Test
    fun `fraction ist bei uebererfuellung auf eins begrenzt`() {
        val day = Day(date, goalType = GoalType.POINTS, goalThreshold = 2)
        val entries = listOf(HabitEntry(check(1, points = 10), progress = 1))

        assertEquals(1f, DayEvaluator.evaluate(day, entries).fraction, 0f)
    }

    @Test
    fun `min count zaehlt erfuellte habits unabhaengig von punkten`() {
        val day = Day(date, goalType = GoalType.MIN_COUNT, goalThreshold = 3)
        val entries = listOf(
            HabitEntry(check(1, points = 9), progress = 1),
            HabitEntry(check(2, points = 9), progress = 1),
            HabitEntry(check(3, points = 9), progress = 0),
        )

        val result = DayEvaluator.evaluate(day, entries)

        assertEquals(2, result.current)
        assertFalse(result.passed)
    }
}
