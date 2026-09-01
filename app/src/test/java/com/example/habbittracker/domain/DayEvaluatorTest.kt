package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.Day
import com.example.habbittracker.domain.model.DayStatus
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

    /** Only POINTS reads the threshold; the other rules derive theirs. */
    private fun day(goalType: GoalType, threshold: Int = 0) =
        Day(date, goalType = goalType, goalThreshold = threshold)

    @Test
    fun `a points goal passes once the sum reaches the threshold`() {
        val entries =
            listOf(
                HabitEntry(check(1, points = 3), progress = 1),
                HabitEntry(check(2, points = 2), progress = 1),
                HabitEntry(check(3, points = 4), progress = 0),
            )

        val result = DayEvaluator.evaluate(day(GoalType.POINTS, threshold = 5), entries)

        assertEquals(5, result.threshold)
        assertEquals(5, result.current)
        assertEquals(DayStatus.PASSED, result.status)
    }

    @Test
    fun `a day is failed while a point is still missing`() {
        val entries =
            listOf(
                HabitEntry(check(1, points = 3), progress = 1),
                HabitEntry(check(2, points = 4), progress = 0),
            )

        val result = DayEvaluator.evaluate(day(GoalType.POINTS, threshold = 7), entries)

        assertEquals(7, result.threshold)
        assertEquals(3, result.current)
        assertEquals(DayStatus.FAILED, result.status)
    }

    @Test
    fun `a threshold beyond reach is capped at what the day offers`() {
        // Six points cannot be gathered from a single one-point habit, so the ask
        // becomes that one point rather than an impossible day.
        val entries = listOf(HabitEntry(check(1, points = 1), progress = 1))

        val result = DayEvaluator.evaluate(day(GoalType.POINTS, threshold = 6), entries)

        assertEquals(1, result.threshold)
        assertEquals(DayStatus.PASSED, result.status)
    }

    @Test
    fun `points only count once the habit is fulfilled`() {
        // 7 out of 8 is not enough: fulfilled starts at progress >= target.
        val entries = listOf(HabitEntry(counter(1, target = 8, points = 2), progress = 7))

        val result = DayEvaluator.evaluate(day(GoalType.POINTS, threshold = 2), entries)

        assertEquals(0, result.current)
        assertEquals(2, result.threshold)
        assertFalse(result.passed)
    }

    @Test
    fun `a counter may exceed its target and stays fulfilled`() {
        val entries = listOf(HabitEntry(counter(1, target = 8), progress = 12))

        assertTrue(DayEvaluator.evaluate(day(GoalType.MIN_COUNT), entries).passed)
    }

    @Test
    fun `min count asks for every habit of the day`() {
        val entries =
            listOf(
                HabitEntry(check(1), progress = 1),
                HabitEntry(check(2), progress = 1),
                HabitEntry(check(3), progress = 0),
            )

        val result = DayEvaluator.evaluate(day(GoalType.MIN_COUNT), entries)

        assertEquals(3, result.threshold)
        assertEquals(2, result.current)
        assertEquals(DayStatus.FAILED, result.status)
    }

    @Test
    fun `min count counts habits regardless of points`() {
        val entries =
            listOf(
                HabitEntry(check(1, points = 9), progress = 1),
                HabitEntry(check(2, points = 1), progress = 1),
            )

        val result = DayEvaluator.evaluate(day(GoalType.MIN_COUNT), entries)

        assertEquals(2, result.threshold)
        assertEquals(DayStatus.PASSED, result.status)
    }

    @Test
    fun `all required ignores habits that are not required`() {
        val entries =
            listOf(
                HabitEntry(check(1, required = true), progress = 1),
                HabitEntry(check(2, required = true), progress = 1),
                HabitEntry(check(3, required = false), progress = 0),
            )

        val result = DayEvaluator.evaluate(day(GoalType.ALL_REQUIRED), entries)

        assertEquals(2, result.current)
        assertEquals(2, result.threshold)
        assertEquals(DayStatus.PASSED, result.status)
    }

    @Test
    fun `all required without required habits is neutral rather than passed`() {
        val entries = listOf(HabitEntry(check(1), progress = 1))

        val result = DayEvaluator.evaluate(day(GoalType.ALL_REQUIRED), entries)

        assertFalse(result.passed)
        assertTrue(result.isNeutral)
    }

    @Test
    fun `an empty day is neutral and not passed`() {
        val result = DayEvaluator.evaluate(day(GoalType.POINTS, threshold = 6), entries = emptyList())

        assertTrue(result.isNeutral)
        assertFalse(result.passed)
        assertEquals(0f, result.fraction, 0f)
    }

    @Test
    fun `the fraction is the share of the day that is done`() {
        val entries =
            listOf(
                HabitEntry(check(1, points = 1), progress = 1),
                HabitEntry(check(2, points = 3), progress = 0),
            )

        val result = DayEvaluator.evaluate(day(GoalType.POINTS, threshold = 4), entries)

        assertEquals(0.25f, result.fraction, 0.001f)
    }
}
