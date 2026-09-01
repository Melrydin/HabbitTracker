package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.DayStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class StreakProtectionTest {
    private val today = LocalDate.of(2026, 8, 31)

    private fun statuses(vararg pairs: Pair<Long, DayStatus>) =
        pairs.associate { (back, status) -> today.minusDays(back) to status }

    private fun shouldFreeze(
        statuses: Map<LocalDate, DayStatus>,
        frozen: Set<LocalDate> = emptySet(),
        budget: Int = 1,
        date: LocalDate = today,
    ) = StreakProtection.shouldFreeze(date, statuses[date] ?: DayStatus.NEUTRAL, statuses, frozen, budget)

    @Test
    fun `a missed day behind a run spends a grace day`() {
        val statuses = statuses(0L to DayStatus.FAILED, 1L to DayStatus.PASSED)

        assertTrue(shouldFreeze(statuses))
    }

    @Test
    fun `a missed day with nothing behind it spends nothing`() {
        val statuses = statuses(0L to DayStatus.FAILED)

        assertFalse(shouldFreeze(statuses))
    }

    @Test
    fun `a second miss in a row spends nothing`() {
        val statuses = statuses(0L to DayStatus.FAILED, 1L to DayStatus.FAILED, 2L to DayStatus.PASSED)

        assertFalse(shouldFreeze(statuses))
    }

    @Test
    fun `neutral days between are stepped over`() {
        val statuses =
            statuses(0L to DayStatus.FAILED, 1L to DayStatus.NEUTRAL, 2L to DayStatus.PASSED)

        assertTrue(shouldFreeze(statuses))
    }

    @Test
    fun `a passed day never spends one`() {
        assertFalse(shouldFreeze(statuses(0L to DayStatus.PASSED, 1L to DayStatus.PASSED)))
    }

    @Test
    fun `the month budget is respected`() {
        val statuses = statuses(0L to DayStatus.FAILED, 1L to DayStatus.PASSED)
        val alreadySpent = setOf(today.withDayOfMonth(2))

        assertFalse(shouldFreeze(statuses, frozen = alreadySpent, budget = 1))
        assertTrue(shouldFreeze(statuses, frozen = alreadySpent, budget = 2))
    }

    @Test
    fun `a budget of zero switches the protection off`() {
        val statuses = statuses(0L to DayStatus.FAILED, 1L to DayStatus.PASSED)

        assertFalse(shouldFreeze(statuses, budget = 0))
    }

    @Test
    fun `a day that already spent one keeps it`() {
        val statuses = statuses(0L to DayStatus.FAILED, 1L to DayStatus.FAILED)

        assertTrue(shouldFreeze(statuses, frozen = setOf(today)))
    }

    @Test
    fun `spending is counted per calendar month`() {
        val frozen = setOf(LocalDate.of(2026, 8, 2), LocalDate.of(2026, 7, 30))

        assertEquals(1, StreakProtection.spentIn(YearMonth.of(2026, 8), frozen))
    }

    @Test
    fun `a frozen day does not break the streak`() {
        val statuses =
            statuses(1L to DayStatus.FAILED, 2L to DayStatus.PASSED, 3L to DayStatus.PASSED)

        assertEquals(0, StreakCalculator.currentStreak(statuses, today))
        assertEquals(2, StreakCalculator.currentStreak(statuses, today, frozen = setOf(today.minusDays(1))))
    }
}
