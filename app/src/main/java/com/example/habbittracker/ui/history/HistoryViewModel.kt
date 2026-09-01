package com.example.habbittracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habbittracker.data.HabitRepository
import com.example.habbittracker.domain.CompletionRate
import com.example.habbittracker.domain.RateComparison
import com.example.habbittracker.domain.Statistics
import com.example.habbittracker.domain.StreakCalculator
import com.example.habbittracker.domain.WeekdayRate
import com.example.habbittracker.domain.completionRate
import com.example.habbittracker.domain.model.DayStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class HistoryUiState(
    val month: YearMonth,
    val today: LocalDate,
    val statuses: Map<LocalDate, DayStatus> = emptyMap(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val monthRate: CompletionRate = CompletionRate(passed = 0, failed = 0),
    val bestWeekday: WeekdayRate? = null,
    val weakestWeekday: WeekdayRate? = null,
    val weekComparison: RateComparison? = null,
    val monthComparison: RateComparison? = null,
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
                bestWeekday = Statistics.bestWeekday(statuses),
                weakestWeekday = Statistics.weakestWeekday(statuses),
                weekComparison = weekComparison(statuses, today),
                monthComparison = monthComparison(statuses, today),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HistoryUiState(month = month.value, today = LocalDate.now(clock)),
        )

    /** This week against the one before, both starting on Monday (F4). */
    private fun weekComparison(statuses: Map<LocalDate, DayStatus>, today: LocalDate): RateComparison {
        val thisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val lastWeek = thisWeek.minusWeeks(1)
        return RateComparison(
            current = completionRate(statuses, thisWeek, thisWeek.plusDays(6)),
            previous = completionRate(statuses, lastWeek, lastWeek.plusDays(6)),
        )
    }

    private fun monthComparison(statuses: Map<LocalDate, DayStatus>, today: LocalDate): RateComparison {
        val thisMonth = YearMonth.from(today)
        val lastMonth = thisMonth.minusMonths(1)
        return RateComparison(
            current = completionRate(statuses, thisMonth.atDay(1), thisMonth.atEndOfMonth()),
            previous = completionRate(statuses, lastMonth.atDay(1), lastMonth.atEndOfMonth()),
        )
    }

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
