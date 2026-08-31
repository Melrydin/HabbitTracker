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

    fun evaluate(day: Day, entries: List<HabitEntry>): DayGoalProgress {
        val fulfilled = entries.filter { it.fulfilled }
        return when (day.goalType) {
            GoalType.ALL_REQUIRED -> {
                val required = entries.filter { it.habit.required }
                DayGoalProgress(
                    goalType = day.goalType,
                    current = required.count { it.fulfilled },
                    threshold = required.size,
                    // Ohne Pflicht-Habits gibt es nichts zu erfuellen: der Tag bleibt neutral.
                    passed = required.isNotEmpty() && required.all { it.fulfilled },
                )
            }

            GoalType.MIN_COUNT -> DayGoalProgress(
                goalType = day.goalType,
                current = fulfilled.size,
                threshold = day.goalThreshold,
                passed = day.goalThreshold > 0 && fulfilled.size >= day.goalThreshold,
            )

            GoalType.POINTS -> DayGoalProgress(
                goalType = day.goalType,
                current = fulfilled.sumOf { it.habit.points },
                threshold = day.goalThreshold,
                passed = day.goalThreshold > 0 && fulfilled.sumOf { it.habit.points } >= day.goalThreshold,
            )
        }
    }
}
