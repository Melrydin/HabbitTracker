package com.example.habbittracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habbittracker.data.HabitRepository
import com.example.habbittracker.domain.CompletionRate
import com.example.habbittracker.domain.StreakCalculator
import com.example.habbittracker.domain.completionRate
import com.example.habbittracker.domain.model.DayStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

data class HistoryUiState(
    val month: YearMonth,
    val today: LocalDate,
    val statuses: Map<LocalDate, DayStatus> = emptyMap(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val monthRate: CompletionRate = CompletionRate(passed = 0, failed = 0),
) {
    /** There is nothing to look at beyond the current month. */
    val canGoForward: Boolean get() = month < YearMonth.from(today)
}

class HistoryViewModel(
    repository: HabitRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now(clock))

    val uiState: StateFlow<HistoryUiState> =
        combine(month, repository.observeDayStatuses()) { shown, statuses ->
            val today = LocalDate.now(clock)
            HistoryUiState(
                month = shown,
                today = today,
                statuses = statuses,
                currentStreak = StreakCalculator.currentStreak(statuses, today),
                longestStreak = StreakCalculator.longestStreak(statuses),
                monthRate = completionRate(statuses, shown.atDay(1), shown.atEndOfMonth()),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HistoryUiState(month = month.value, today = LocalDate.now(clock)),
        )

    fun onPreviousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun onNextMonth() {
        if (uiState.value.canGoForward) month.value = month.value.plusMonths(1)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
