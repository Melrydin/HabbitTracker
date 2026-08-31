package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.Day
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.HabitEntry

/**
 * Fortschritt gegen das Tagesziel. [current] und [threshold] haengen von der
 * Regel ab: Punkte bei [GoalType.POINTS], Anzahl Habits sonst.
 */
data class DayGoalProgress(
    val goalType: GoalType,
    val current: Int,
    val threshold: Int,
    val passed: Boolean,
) {
    /** 0f bis 1f, fuer die Fortschrittsanzeige. Ohne Schwelle bleibt der Balken leer. */
    val fraction: Float
        get() = if (threshold <= 0) 0f else (current.toFloat() / threshold).coerceIn(0f, 1f)

    /** Kein Ziel erreichbar, weil der Tag keine passenden Habits hat: neutral statt "nicht bestanden". */
    val isNeutral: Boolean get() = threshold <= 0
}

/**
 * Wertet ein Tagesziel aus (F2). Reine Funktion ohne Zustand, damit Repository,
 * ViewModel und Tests dieselbe Regel benutzen.
 */
object DayEvaluator {

    fun evaluate(day: Day, entries: List<HabitEntry>): DayGoalProgress = when (day.goalType) {
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
            // Ohne Pflicht-Habits gibt es nichts zu erfuellen: der Tag bleibt neutral.
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
