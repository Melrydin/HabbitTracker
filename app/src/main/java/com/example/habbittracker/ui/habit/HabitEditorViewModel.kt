package com.example.habbittracker.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habbittracker.data.HabitRepository
import com.example.habbittracker.data.HabitRepository.Companion.NEW_HABIT_ID
import com.example.habbittracker.domain.CompletionRate
import com.example.habbittracker.domain.HabitStreakCalculator
import com.example.habbittracker.domain.Statistics
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitType
import com.example.habbittracker.domain.model.StreakRule
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** What the habit has done lately (F4), shown next to the form for existing habits. */
data class HabitStats(val currentStreak: Int, val rate: CompletionRate)

data class HabitEditorUiState(
    val form: HabitFormState = HabitFormState(),
    val stats: HabitStats? = null,
    val loading: Boolean = false,
)

/**
 * The editor has run its course and the caller should close it.
 *
 * Saving, archiving, deleting and a habit that no longer exists all end the same
 * way, so they are deliberately not told apart. Should any of them ever need its
 * own feedback, this becomes a sealed interface again.
 */
data object HabitEditorFinished

class HabitEditorViewModel(
    private val repository: HabitRepository,
    private val habitId: Long = NEW_HABIT_ID,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(HabitEditorUiState(loading = habitId != NEW_HABIT_ID))
    val uiState: StateFlow<HabitEditorUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<HabitEditorFinished>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        if (habitId != NEW_HABIT_ID) {
            viewModelScope.launch {
                val habit = repository.getHabit(habitId)
                if (habit == null) {
                    eventChannel.send(HabitEditorFinished)
                } else {
                    _uiState.value = HabitEditorUiState(HabitFormState.from(habit), loading = false)
                    observeStats(habit)
                }
            }
        }
    }

    fun onNameChange(value: String) = updateForm { it.withName(value) }

    fun onTypeChange(value: HabitType) = updateForm { it.withType(value) }

    fun onTargetChange(value: String) = updateForm { it.withTarget(value) }

    fun onUnitChange(value: String) = updateForm { it.withUnit(value) }

    fun onNoteChange(value: String) = updateForm { it.withNote(value) }

    fun onStreakRuleChange(value: StreakRule) = updateForm { it.withStreakRule(value) }

    fun onPerWeekTargetChange(value: Int) = updateForm { it.withPerWeekTarget(value) }

    fun onPointsChange(value: Int) = updateForm { it.withPoints(value) }

    fun onRequiredChange(value: Boolean) = updateForm { it.copy(required = value) }

    fun onIconChange(value: String) = updateForm { it.copy(icon = value) }

    fun onSave() {
        val form = _uiState.value.form
        if (!form.canSave) return
        viewModelScope.launch {
            repository.upsertHabit(form.toHabit())
            eventChannel.send(HabitEditorFinished)
        }
    }

    /** Archiving also saves pending edits, so that nothing is silently lost. */
    fun onToggleArchived() {
        val form = _uiState.value.form
        if (form.isNew || !form.canSave) return
        viewModelScope.launch {
            repository.upsertHabit(form.toHabit().copy(archived = !form.archived))
            eventChannel.send(HabitEditorFinished)
        }
    }

    fun onDelete() {
        val form = _uiState.value.form
        if (form.isNew) return
        viewModelScope.launch {
            repository.deleteHabit(form.id)
            eventChannel.send(HabitEditorFinished)
        }
    }

    private inline fun updateForm(transform: (HabitFormState) -> HabitFormState) {
        _uiState.value = _uiState.value.let { it.copy(form = transform(it.form)) }
    }

    /**
     * The numbers follow the stored habit, not the form: they describe what has
     * happened, and an unsaved edit must not rewrite the past.
     */
    private fun observeStats(habit: Habit) {
        viewModelScope.launch {
            repository.observeHabitHistory(habit.id).collect { history ->
                val today = LocalDate.now(clock)
                _uiState.value =
                    _uiState.value.copy(
                        stats =
                            HabitStats(
                                currentStreak = HabitStreakCalculator.currentStreak(habit, history, today),
                                rate = Statistics.habitRate(history, today.minusDays(RATE_WINDOW_DAYS), today),
                            ),
                    )
            }
        }
    }

    private companion object {
        const val RATE_WINDOW_DAYS = 29L
    }
}
