package com.example.habbittracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {
    private val today = LocalDate.of(2026, 8, 31)

    private fun daysBefore(vararg offsets: Long) = offsets.map { today.minusDays(it) }.toSet()

    @Test
    fun `laufende serie zaehlt heute mit sobald der tag bestanden ist`() {
        val passed = daysBefore(0, 1, 2)

        assertEquals(3, StreakCalculator.currentStreak(passed, today))
    }

    @Test
    fun `heute noch offen bricht die serie nicht`() {
        val passed = daysBefore(1, 2, 3)

        assertEquals(3, StreakCalculator.currentStreak(passed, today))
    }

    @Test
    fun `eine luecke setzt die serie zurueck`() {
        val passed = daysBefore(1, 3, 4)

        assertEquals(1, StreakCalculator.currentStreak(passed, today))
    }

    @Test
    fun `ohne bestandene tage ist die serie null`() {
        assertEquals(0, StreakCalculator.currentStreak(emptySet(), today))
    }

    @Test
    fun `laengste serie findet den groessten block`() {
        val passed = daysBefore(1, 2, 5, 6, 7, 8, 10)

        assertEquals(4, StreakCalculator.longestStreak(passed))
    }

    @Test
    fun `laengste serie ist ohne daten null`() {
        assertEquals(0, StreakCalculator.longestStreak(emptySet()))
    }
}
