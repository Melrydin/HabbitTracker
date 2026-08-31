package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.Day
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.HabitEntry

/**
 * Progress towards the daily goal. [current] and [threshold] depend on the rule:
 * points for [GoalType.POINTS], a habit count otherwise.
 */
data class DayGoalProgress(
    val goalType: GoalType,
    val current: Int,
    val threshold: Int,
    val passed: Boolean,
) {
    /** 0f to 1f, for the progress bar. Without a threshold the bar stays empty. */
    val fraction: Float
        get() = if (threshold <= 0) 0f else (current.toFloat() / threshold).coerceIn(0f, 1f)

    /** No goal is reachable because the day has no matching habits: neutral rather than failed. */
    val isNeutral: Boolean get() = threshold <= 0
}

/**
 * Evaluates a daily goal (F2). A pure function without state, so that repository,
 * view model and tests all share the same rule.
 */
object DayEvaluator {
    fun evaluate(day: Day, entries: List<HabitEntry>): DayGoalProgress =
        when (day.goalType) {
            GoalType.ALL_REQUIRED -> evaluateAllRequired(day, entries)
            GoalType.MIN_COUNT -> evaluateMinCount(day, entries)
            GoalType.POINTS -> evaluatePoints(day, entries)
        }

    private fun evaluateAllRequired(day: Day, entries: List<HabitEntry>): DayGoalProgress {
        val required = entries.filter { it.habit.required }
        return DayGoalProgress(
            goalType = day.goalType,
            current = required.count { it.fulfilled },
            threshold = required.size,
            // Without required habits there is nothing to fulfill, so the day stays neutral.
            passed = required.isNotEmpty() && required.all { it.fulfilled },
        )
    }

    private fun evaluateMinCount(day: Day, entries: List<HabitEntry>): DayGoalProgress {
        val fulfilled = entries.count { it.fulfilled }
        return DayGoalProgress(
            goalType = day.goalType,
            current = fulfilled,
            threshold = day.goalThreshold,
            passed = day.goalThreshold > 0 && fulfilled >= day.goalThreshold,
        )
    }

    private fun evaluatePoints(day: Day, entries: List<HabitEntry>): DayGoalProgress {
        val points = entries.filter { it.fulfilled }.sumOf { it.habit.points }
        return DayGoalProgress(
            goalType = day.goalType,
            current = points,
            threshold = day.goalThreshold,
            passed = day.goalThreshold > 0 && points >= day.goalThreshold,
        )
    }
}
