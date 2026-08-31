package com.example.habbittracker.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habbittracker.data.HabitRepository
import com.example.habbittracker.domain.DayEvaluator
import com.example.habbittracker.domain.model.Day
import com.example.habbittracker.domain.model.HabitType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repository: HabitRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    // TODO: reset when the date rolls over midnight, once the screen becomes visible again.
    private val date = MutableStateFlow(LocalDate.now(clock))

    /**
     * While typing, the local draft wins so that the cursor does not jump when the
     * stored value flows back. `null` means: take the value from the repository.
     */
    private val themeDraft = MutableStateFlow<String?>(null)

    private val eventChannel = Channel<TodayEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var lastPassed: Boolean? = null

    val uiState: StateFlow<TodayUiState> =
        date
            .flatMapLatest { day -> repository.observeDay(day) }
            .combine(themeDraft) { snapshot, draft ->
                TodayUiState(
                    date = snapshot.day.date,
                    theme = draft ?: snapshot.day.theme.orEmpty(),
                    goal = DayEvaluator.evaluate(snapshot.day, snapshot.entries),
                    habits = snapshot.entries.map(::HabitItem),
                    currentStreak = snapshot.currentStreak,
                    loaded = true,
                )
            }.onEach { state ->
                // Only the transition from open to passed triggers the brief confirmation.
                if (lastPassed == false && state.goal.passed) eventChannel.trySend(TodayEvent.DayPassed)
                lastPassed = state.goal.passed
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = TodayUiState(date = date.value),
            )

    fun onToggleCheck(item: HabitItem) {
        setProgress(item, if (item.fulfilled) 0 else item.entry.habit.target)
    }

    fun onIncrement(item: HabitItem) = setProgress(item, item.entry.progress + item.step)

    fun onDecrement(item: HabitItem) = setProgress(item, item.entry.progress - item.step)

    fun onThemeChange(text: String) {
        val limited = text.take(Day.THEME_MAX_LENGTH)
        themeDraft.value = limited
        viewModelScope.launch { repository.setDayTheme(date.value, limited) }
    }

    private fun setProgress(item: HabitItem, progress: Int) {
        viewModelScope.launch { repository.setProgress(date.value, item.id, progress) }
    }

    /** Counters step by one, amounts step coarser so that 30 min is not 30 taps. */
    private val HabitItem.step: Int
        get() =
            when (entry.habit.type) {
                HabitType.CHECK -> 1
                HabitType.COUNTER -> 1
                HabitType.AMOUNT -> AMOUNT_STEP
            }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val AMOUNT_STEP = 5
    }
}
