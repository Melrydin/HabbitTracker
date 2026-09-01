package com.example.habbittracker.ui.today

import com.example.habbittracker.domain.DayGoalProgress
import com.example.habbittracker.domain.model.DayStatus
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.HabitEntry
import java.time.LocalDate

/** A habit as the list on the today screen renders it. */
data class HabitItem(
    val entry: HabitEntry,
) {
    val id: Long get() = entry.habit.id
    val fulfilled: Boolean get() = entry.fulfilled
}

data class TodayUiState(
    val date: LocalDate,
    val theme: String = "",
    val dayNote: String = "",
    val goal: DayGoalProgress =
        DayGoalProgress(GoalType.POINTS, current = 0, threshold = 0, status = DayStatus.NEUTRAL),
    val habits: List<HabitItem> = emptyList(),
    val goalOverridden: Boolean = false,
    val currentStreak: Int = 0,
    val isToday: Boolean = true,
    val loaded: Boolean = false,
)

/** One-off feedback that is not part of the state. */
sealed interface TodayEvent {
    /** The day has just flipped to "passed" (F2). */
    data object DayPassed : TodayEvent
}
