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
    /**
     * [paused] makes the day neutral whatever was recorded (F4): during a break
     * nothing is asked, so nothing can be missed.
     */
    fun evaluate(day: Day, entries: List<HabitEntry>, paused: Boolean = false): DayGoalProgress =
        if (paused) {
            paused(day, entries)
        } else {
            evaluateByRule(day, entries)
        }

    private fun paused(day: Day, entries: List<HabitEntry>) =
        evaluateByRule(day, entries).copy(status = DayStatus.NEUTRAL)

    private fun evaluateByRule(day: Day, entries: List<HabitEntry>): DayGoalProgress =
        when (day.goalType) {
            GoalType.ALL_REQUIRED -> evaluateAllRequired(day, entries)
            GoalType.MIN_COUNT -> evaluateMinCount(day, entries)
            GoalType.POINTS -> evaluatePoints(day, entries)
        }

    private fun evaluateAllRequired(day: Day, entries: List<HabitEntry>): DayGoalProgress {
        val required = entries.filter { it.habit.required }
        return progress(day, required.count { it.fulfilled }, required.size)
    }

    private fun evaluateMinCount(day: Day, entries: List<HabitEntry>): DayGoalProgress =
        progress(day, entries.count { it.fulfilled }, entries.size)

    private fun evaluatePoints(day: Day, entries: List<HabitEntry>): DayGoalProgress =
        progress(
            day = day,
            current = entries.filter { it.fulfilled }.sumOf { it.habit.points },
            // Capped at what the day holds: a threshold beyond that would make the
            // day impossible to pass no matter what the user did.
            threshold = day.goalThreshold.coerceAtMost(entries.sumOf { it.habit.points }),
        )

    /**
     * Points are the only rule with a threshold of its own; the other two derive
     * theirs from the day. The count rule asks for all of its habits and the
     * required rule for the ones marked as such, so neither can be set beyond
     * reach.
     *
     * A day is only judged once something was actually asked of it. Without a
     * reachable goal it stays [DayStatus.NEUTRAL] rather than counting as failed,
     * which is what keeps an empty day from breaking a streak.
     */
    private fun progress(day: Day, current: Int, threshold: Int) =
        DayGoalProgress(
            goalType = day.goalType,
            current = current,
            threshold = threshold,
            status =
                when {
                    threshold <= 0 -> DayStatus.NEUTRAL
                    current >= threshold -> DayStatus.PASSED
                    else -> DayStatus.FAILED
                },
        )
}
