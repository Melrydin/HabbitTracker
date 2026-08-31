package com.example.habbittracker.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habbittracker.data.HabitRepository
import com.example.habbittracker.data.HabitRepository.Companion.NEW_HABIT_ID
import com.example.habbittracker.domain.model.HabitType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class HabitEditorUiState(
    val form: HabitFormState = HabitFormState(),
    val loading: Boolean = false,
)

/** The editor is done once one of these events occurs. */
sealed interface HabitEditorEvent {
    data object Saved : HabitEditorEvent

    data object Archived : HabitEditorEvent

    data object Deleted : HabitEditorEvent

    /** The habit has been deleted elsewhere in the meantime. */
    data object NotFound : HabitEditorEvent
}

class HabitEditorViewModel(
    private val repository: HabitRepository,
    private val habitId: Long = NEW_HABIT_ID,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HabitEditorUiState(loading = habitId != NEW_HABIT_ID))
    val uiState: StateFlow<HabitEditorUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<HabitEditorEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        if (habitId != NEW_HABIT_ID) {
            viewModelScope.launch {
                val habit = repository.getHabit(habitId)
                if (habit == null) {
                    eventChannel.send(HabitEditorEvent.NotFound)
                } else {
                    _uiState.value = HabitEditorUiState(HabitFormState.from(habit), loading = false)
                }
            }
        }
    }

    fun onNameChange(value: String) = updateForm { it.withName(value) }

    fun onTypeChange(value: HabitType) = updateForm { it.withType(value) }

    fun onTargetChange(value: String) = updateForm { it.withTarget(value) }

    fun onUnitChange(value: String) = updateForm { it.withUnit(value) }

    fun onPointsChange(value: Int) = updateForm { it.withPoints(value) }

    fun onRequiredChange(value: Boolean) = updateForm { it.copy(required = value) }

    fun onIconChange(value: String) = updateForm { it.copy(icon = value) }

    fun onSave() {
        val form = _uiState.value.form
        if (!form.canSave) return
        viewModelScope.launch {
            repository.upsertHabit(form.toHabit())
            eventChannel.send(HabitEditorEvent.Saved)
        }
    }

    /** Archiving also saves pending edits, so that nothing is silently lost. */
    fun onToggleArchived() {
        val form = _uiState.value.form
        if (form.isNew || !form.canSave) return
        viewModelScope.launch {
            repository.upsertHabit(form.toHabit().copy(archived = !form.archived))
            eventChannel.send(HabitEditorEvent.Archived)
        }
    }

    fun onDelete() {
        val form = _uiState.value.form
        if (form.isNew) return
        viewModelScope.launch {
            repository.deleteHabit(form.id)
            eventChannel.send(HabitEditorEvent.Deleted)
        }
    }

    private inline fun updateForm(transform: (HabitFormState) -> HabitFormState) {
        _uiState.value = _uiState.value.let { it.copy(form = transform(it.form)) }
    }
}
