package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.DayStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CompletionRateTest {
    private val from = LocalDate.of(2026, 8, 1)
    private val to = LocalDate.of(2026, 8, 31)

    private fun day(dayOfMonth: Int) = LocalDate.of(2026, 8, dayOfMonth)

    @Test
    fun `the rate counts passed against judged days`() {
        val statuses =
            mapOf(
                day(1) to DayStatus.PASSED,
                day(2) to DayStatus.PASSED,
                day(3) to DayStatus.PASSED,
                day(4) to DayStatus.FAILED,
            )

        val rate = completionRate(statuses, from, to)

        assertEquals(3, rate.passed)
        assertEquals(4, rate.evaluated)
        assertEquals(75, rate.percent)
    }

    @Test
    fun `neutral days are left out of both numbers`() {
        val statuses =
            mapOf(
                day(1) to DayStatus.PASSED,
                day(2) to DayStatus.NEUTRAL,
                day(3) to DayStatus.NEUTRAL,
            )

        val rate = completionRate(statuses, from, to)

        assertEquals(1, rate.evaluated)
        assertEquals(100, rate.percent)
    }

    @Test
    fun `days outside the range do not count`() {
        val statuses =
            mapOf(
                LocalDate.of(2026, 7, 31) to DayStatus.FAILED,
                day(1) to DayStatus.PASSED,
                LocalDate.of(2026, 9, 1) to DayStatus.FAILED,
            )

        val rate = completionRate(statuses, from, to)

        assertEquals(1, rate.evaluated)
        assertEquals(100, rate.percent)
    }

    @Test
    fun `without judged days the rate is zero rather than undefined`() {
        val rate = completionRate(mapOf(day(1) to DayStatus.NEUTRAL), from, to)

        assertEquals(0, rate.evaluated)
        assertEquals(0, rate.percent)
        assertEquals(0f, rate.fraction, 0f)
    }
}
