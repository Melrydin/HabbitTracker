package com.example.habbittracker.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habbittracker.data.HabitRepository
import com.example.habbittracker.domain.DayEvaluator
import com.example.habbittracker.domain.model.Day
import com.example.habbittracker.domain.model.Habit
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
    shownDate: LocalDate? = null,
) : ViewModel() {
    // TODO: reset when the date rolls over midnight, once the screen becomes visible again.
    private val date = MutableStateFlow(shownDate ?: LocalDate.now(clock))

    /**
     * While typing, the local draft wins so that the cursor does not jump when the
     * stored value flows back. `null` means: take the value from the repository.
     */
    private val themeDraft = MutableStateFlow<String?>(null)
    private val noteDraft = MutableStateFlow<String?>(null)

    private val eventChannel = Channel<TodayEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var lastPassed: Boolean? = null

    val uiState: StateFlow<TodayUiState> =
        date
            .flatMapLatest { day -> repository.observeDay(day) }
            .combine(themeDraft.combine(noteDraft, ::Pair)) { snapshot, (draft, noteDraft) ->
                TodayUiState(
                    date = snapshot.day.date,
                    theme = draft ?: snapshot.themeName.orEmpty(),
                    dayNote = noteDraft ?: snapshot.day.dayNote.orEmpty(),
                    goal = DayEvaluator.evaluate(snapshot.day, snapshot.entries),
                    habits = snapshot.entries.map(::HabitItem),
                    currentStreak = snapshot.currentStreak,
                    isToday = snapshot.day.date == LocalDate.now(clock),
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
        val limited = text.take(Habit.NAME_MAX_LENGTH)
        themeDraft.value = limited
        viewModelScope.launch { repository.setDayTheme(date.value, limited) }
    }

    fun onDayNoteChange(text: String) {
        val limited = text.take(Day.NOTE_MAX_LENGTH)
        noteDraft.value = limited
        viewModelScope.launch { repository.setDayNote(date.value, limited) }
    }

    private fun setProgress(item: HabitItem, progress: Int) {
        viewModelScope.launch { repository.setProgress(date.value, item.id, progress) }
    }

    /**
     * Larger targets step coarser, so a 30 minute habit is not 30 taps. The cut-off
     * is a judgement call: below it a single step still feels like one unit.
     */
    private val HabitItem.step: Int
        get() =
            when {
                entry.habit.type == HabitType.CHECK -> 1
                entry.habit.target > COARSE_STEP_FROM -> COARSE_STEP
                else -> 1
            }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val COARSE_STEP = 5
        const val COARSE_STEP_FROM = 12
    }
}
