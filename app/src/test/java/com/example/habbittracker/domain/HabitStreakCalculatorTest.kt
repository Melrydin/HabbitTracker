package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitType
import com.example.habbittracker.domain.model.StreakRule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HabitStreakCalculatorTest {
    // A Monday, so week boundaries are easy to reason about.
    private val today = LocalDate.of(2026, 8, 31)

    private fun daily() =
        Habit(1, "Exercise", HabitType.CHECK, target = 1, icon = "task_alt")

    private fun weekly(perWeek: Int?) =
        Habit(
            id = 1,
            name = "Exercise",
            type = HabitType.CHECK,
            target = 1,
            icon = "task_alt",
            streakRule = StreakRule.WEEKLY_COUNT,
            perWeekTarget = perWeek,
        )

    private fun history(vararg entries: Pair<Long, Boolean>) =
        entries.associate { (back, done) -> today.minusDays(back) to done }

    @Test
    fun `a daily streak counts consecutive fulfilled days`() {
        val fulfillment = history(0L to true, 1L to true, 2L to true, 3L to false)

        assertEquals(3, HabitStreakCalculator.currentStreak(daily(), fulfillment, today))
    }

    @Test
    fun `a day still open does not break the daily streak`() {
        val fulfillment = history(0L to false, 1L to true, 2L to true)

        assertEquals(2, HabitStreakCalculator.currentStreak(daily(), fulfillment, today))
    }

    @Test
    fun `a day the habit did not apply to is skipped`() {
        // Day 2 is missing entirely, so the run reaches across it.
        val fulfillment = history(0L to true, 1L to true, 3L to true)

        assertEquals(3, HabitStreakCalculator.currentStreak(daily(), fulfillment, today))
    }

    @Test
    fun `without history the daily streak is zero`() {
        assertEquals(0, HabitStreakCalculator.currentStreak(daily(), emptyMap(), today))
    }

    @Test
    fun `a weekly streak counts weeks that reached their target`() {
        // Today is a Monday, so days 1..7 are last week and 8..14 the week before.
        val fulfillment =
            history(1L to true, 2L to true, 3L to true, 8L to true, 9L to true, 10L to true)

        assertEquals(2, HabitStreakCalculator.currentStreak(weekly(3), fulfillment, today))
    }

    @Test
    fun `a week short of its target ends the weekly streak`() {
        // The last finished week reached two of three, so the run stops there and
        // the complete week before it is out of reach.
        val fulfillment = history(1L to true, 2L to true, 8L to true, 9L to true, 10L to true)

        assertEquals(0, HabitStreakCalculator.currentStreak(weekly(3), fulfillment, today))
    }

    @Test
    fun `the running week does not break the weekly streak`() {
        // Nothing done this week yet, but the two before were complete.
        val fulfillment = history(1L to true, 2L to true, 8L to true, 9L to true)

        assertEquals(2, HabitStreakCalculator.currentStreak(weekly(2), fulfillment, today))
    }

    @Test
    fun `the running week counts once it has reached its target`() {
        val fulfillment = history(0L to true, 1L to true, 2L to true)

        // Today alone is this week; last week holds days 1 and 2.
        assertEquals(2, HabitStreakCalculator.currentStreak(weekly(1), fulfillment, today))
    }

    @Test
    fun `a weekly rule without a target counts nothing`() {
        val fulfillment = history(1L to true, 2L to true)

        assertEquals(0, HabitStreakCalculator.currentStreak(weekly(null), fulfillment, today))
    }
}
