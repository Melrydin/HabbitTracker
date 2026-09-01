package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.Polarity
import com.example.habbittracker.domain.model.StreakRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * A habit's own streak (F4), next to the day streak in [StreakCalculator].
 *
 * The input maps every day the habit applied to whether it was fulfilled. Days
 * missing from it are days the habit did not belong to, and they are skipped
 * rather than counted against the run, the same way a neutral day is.
 */
object HabitStreakCalculator {
    /**
     * The run ending now: days under [StreakRule.DAILY], weeks under
     * [StreakRule.WEEKLY_COUNT].
     *
     * The day or week in progress never breaks the run. It is not over yet, and a
     * streak that reads zero every morning would tell the user nothing.
     */
    fun currentStreak(habit: Habit, fulfillment: Map<LocalDate, Boolean>, today: LocalDate): Int =
        when {
            habit.polarity == Polarity.BAD -> daysSinceSlip(fulfillment, today)
            habit.streakRule == StreakRule.WEEKLY_COUNT -> weeklyStreak(habit, fulfillment, today)
            else -> dailyStreak(fulfillment, today)
        }

    /**
     * Days since the last slip, for a habit that is avoided (F11).
     *
     * Read off the calendar rather than counted over stored days: an abstinence
     * habit is only ever written to when something goes wrong, so counting stored
     * days would leave the number at zero on exactly the days it should reward.
     * Without a slip on record the run starts at the earliest day known of the
     * habit, which is the most that can honestly be claimed.
     */
    private fun daysSinceSlip(fulfillment: Map<LocalDate, Boolean>, today: LocalDate): Int {
        val lastSlip =
            fulfillment
                .filterValues { !it }
                .keys
                .filterNot { it.isAfter(today) }
                .maxOrNull()
        val start = lastSlip ?: fulfillment.keys.minOrNull()?.minusDays(1) ?: return 0
        return ChronoUnit.DAYS
            .between(start, today)
            .toInt()
            .coerceAtLeast(0)
    }

    private fun dailyStreak(fulfillment: Map<LocalDate, Boolean>, today: LocalDate): Int {
        val earliest = fulfillment.keys.minOrNull() ?: return 0
        var cursor = if (fulfillment[today] == true) today else today.minusDays(1)
        var streak = 0
        while (!cursor.isBefore(earliest)) {
            when (fulfillment[cursor]) {
                true -> streak++
                false -> return streak
                null -> Unit
            }
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun weeklyStreak(habit: Habit, fulfillment: Map<LocalDate, Boolean>, today: LocalDate): Int {
        val target = habit.perWeekTarget ?: return 0
        if (target < 1) return 0
        val byWeek =
            fulfillment
                .filterValues { it }
                .keys
                .groupingBy { it.weekStart() }
                .eachCount()
        val earliest = fulfillment.keys.minOrNull()?.weekStart() ?: return 0

        var cursor = today.weekStart()
        // The running week counts only once it is already done; it never breaks the run.
        if ((byWeek[cursor] ?: 0) < target) cursor = cursor.minusWeeks(1)
        var streak = 0
        while (!cursor.isBefore(earliest)) {
            if ((byWeek[cursor] ?: 0) < target) return streak
            streak++
            cursor = cursor.minusWeeks(1)
        }
        return streak
    }

    /** Weeks run Monday to Sunday, as the week habits of F8 do. */
    private fun LocalDate.weekStart(): LocalDate =
        with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
