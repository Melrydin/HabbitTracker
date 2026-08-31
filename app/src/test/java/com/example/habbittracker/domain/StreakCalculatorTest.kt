package com.example.habbittracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {
    private val today = LocalDate.of(2026, 8, 31)

    private fun daysBefore(vararg offsets: Long) = offsets.map { today.minusDays(it) }.toSet()

    @Test
    fun `a running streak includes today once the day has passed`() {
        val passed = daysBefore(0, 1, 2)

        assertEquals(3, StreakCalculator.currentStreak(passed, today))
    }

    @Test
    fun `a today that is still open does not break the streak`() {
        val passed = daysBefore(1, 2, 3)

        assertEquals(3, StreakCalculator.currentStreak(passed, today))
    }

    @Test
    fun `a gap resets the streak`() {
        val passed = daysBefore(1, 3, 4)

        assertEquals(1, StreakCalculator.currentStreak(passed, today))
    }

    @Test
    fun `without passed days the streak is zero`() {
        assertEquals(0, StreakCalculator.currentStreak(emptySet(), today))
    }

    @Test
    fun `the longest streak finds the biggest block`() {
        val passed = daysBefore(1, 2, 5, 6, 7, 8, 10)

        assertEquals(4, StreakCalculator.longestStreak(passed))
    }

    @Test
    fun `the longest streak is zero without data`() {
        assertEquals(0, StreakCalculator.longestStreak(emptySet()))
    }
}
