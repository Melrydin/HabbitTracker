package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.DayStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {
    private val today = LocalDate.of(2026, 8, 31)

    /** Builds a status map from offsets before today, newest first. */
    private fun statuses(vararg pairs: Pair<Long, DayStatus>) =
        pairs.associate { (back, status) -> today.minusDays(back) to status }

    private fun passed(vararg back: Long) = statuses(*back.map { it to DayStatus.PASSED }.toTypedArray())

    @Test
    fun `a running streak includes today once the day has passed`() {
        assertEquals(3, StreakCalculator.currentStreak(passed(0, 1, 2), today))
    }

    @Test
    fun `a today that is still open does not break the streak`() {
        val statuses = passed(1, 2, 3) + (today to DayStatus.FAILED)

        assertEquals(3, StreakCalculator.currentStreak(statuses, today))
    }

    @Test
    fun `a failed day resets the streak`() {
        val statuses = passed(1, 2) + (today.minusDays(3) to DayStatus.FAILED) + passed(4, 5)

        assertEquals(2, StreakCalculator.currentStreak(statuses, today))
    }

    @Test
    fun `a neutral day is skipped instead of breaking the streak`() {
        val statuses = passed(1, 2) + (today.minusDays(3) to DayStatus.NEUTRAL) + passed(4, 5)

        assertEquals(4, StreakCalculator.currentStreak(statuses, today))
    }

    @Test
    fun `a day without a stored entry counts as neutral`() {
        // Nothing is stored for day 3, so the run reaches across the gap.
        assertEquals(4, StreakCalculator.currentStreak(passed(1, 2, 4, 5), today))
    }

    @Test
    fun `without any days the streak is zero`() {
        assertEquals(0, StreakCalculator.currentStreak(emptyMap(), today))
    }

    @Test
    fun `the longest streak finds the biggest block`() {
        val statuses =
            passed(1, 2) +
                (today.minusDays(3) to DayStatus.FAILED) +
                passed(4, 5, 6, 7) +
                (today.minusDays(8) to DayStatus.FAILED)

        assertEquals(4, StreakCalculator.longestStreak(statuses))
    }

    @Test
    fun `the longest streak ignores neutral days`() {
        val statuses = passed(1, 2) + (today.minusDays(3) to DayStatus.NEUTRAL) + passed(4)

        assertEquals(3, StreakCalculator.longestStreak(statuses))
    }

    @Test
    fun `the longest streak is zero without data`() {
        assertEquals(0, StreakCalculator.longestStreak(emptyMap()))
    }
}
