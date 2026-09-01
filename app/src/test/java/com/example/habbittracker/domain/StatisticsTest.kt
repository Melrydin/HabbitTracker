package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.DayStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class StatisticsTest {
    // 2026-08-31 is a Monday.
    private fun monday(week: Int) = LocalDate.of(2026, 8, 31).plusWeeks(week.toLong())

    @Test
    fun `a weekday rate counts only that weekday`() {
        val statuses =
            mapOf(
                monday(0) to DayStatus.PASSED,
                monday(1) to DayStatus.FAILED,
                monday(0).plusDays(1) to DayStatus.FAILED,
            )

        val monday = Statistics.weekdayRates(statuses).first { it.day == DayOfWeek.MONDAY }

        assertEquals(2, monday.rate.evaluated)
        assertEquals(50, monday.rate.percent)
    }

    @Test
    fun `every weekday appears even without data`() {
        assertEquals(7, Statistics.weekdayRates(emptyMap()).size)
    }

    @Test
    fun `the best and weakest weekday ignore days nothing was judged on`() {
        val statuses =
            mapOf(
                monday(0) to DayStatus.PASSED,
                monday(0).plusDays(1) to DayStatus.FAILED,
                monday(0).plusDays(2) to DayStatus.NEUTRAL,
            )

        assertEquals(DayOfWeek.MONDAY, Statistics.bestWeekday(statuses)?.day)
        assertEquals(DayOfWeek.TUESDAY, Statistics.weakestWeekday(statuses)?.day)
    }

    @Test
    fun `without any judged day there is no best weekday`() {
        assertNull(Statistics.bestWeekday(mapOf(monday(0) to DayStatus.NEUTRAL)))
    }

    @Test
    fun `a habit rate counts fulfilled days against the days it applied`() {
        val fulfillment =
            mapOf(
                monday(0) to true,
                monday(0).plusDays(1) to true,
                monday(0).plusDays(2) to false,
            )

        val rate = Statistics.habitRate(fulfillment, monday(0), monday(0).plusDays(6))

        assertEquals(3, rate.evaluated)
        assertEquals(67, rate.percent)
    }

    @Test
    fun `a habit rate ignores days outside the period`() {
        val fulfillment = mapOf(monday(0).minusDays(1) to false, monday(0) to true)

        val rate = Statistics.habitRate(fulfillment, monday(0), monday(0).plusDays(6))

        assertEquals(1, rate.evaluated)
        assertEquals(100, rate.percent)
    }

    @Test
    fun `a comparison states the difference in percentage points`() {
        val comparison =
            RateComparison(
                current = CompletionRate(passed = 3, failed = 1),
                previous = CompletionRate(passed = 1, failed = 1),
            )

        assertEquals(25, comparison.differenceInPercent)
    }
}
