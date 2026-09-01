package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.Day
import com.example.habbittracker.domain.model.DayStatus
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
    val status: DayStatus,
) {
    /** 0f to 1f, for the progress bar. Without a threshold the bar stays empty. */
    val fraction: Float
        get() = if (threshold <= 0) 0f else (current.toFloat() / threshold).coerceIn(0f, 1f)

    val passed: Boolean get() = status == DayStatus.PASSED

    val isNeutral: Boolean get() = status == DayStatus.NEUTRAL
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
        return progress(
            day = day,
            current = required.count { it.fulfilled },
            threshold = required.size,
            reached = required.isNotEmpty() && required.all { it.fulfilled },
        )
    }

    private fun evaluateMinCount(day: Day, entries: List<HabitEntry>): DayGoalProgress {
        val fulfilled = entries.count { it.fulfilled }
        return progress(day, fulfilled, day.goalThreshold, fulfilled >= day.goalThreshold)
    }

    private fun evaluatePoints(day: Day, entries: List<HabitEntry>): DayGoalProgress {
        val points = entries.filter { it.fulfilled }.sumOf { it.habit.points }
        return progress(day, points, day.goalThreshold, points >= day.goalThreshold)
    }

    /**
     * A day is only judged once something was actually asked of it. Without a
     * reachable goal it stays [DayStatus.NEUTRAL] rather than counting as failed,
     * which is what keeps an empty day from breaking a streak.
     */
    private fun progress(day: Day, current: Int, threshold: Int, reached: Boolean) =
        DayGoalProgress(
            goalType = day.goalType,
            current = current,
            threshold = threshold,
            status =
                when {
                    threshold <= 0 -> DayStatus.NEUTRAL
                    reached -> DayStatus.PASSED
                    else -> DayStatus.FAILED
                },
        )
}
