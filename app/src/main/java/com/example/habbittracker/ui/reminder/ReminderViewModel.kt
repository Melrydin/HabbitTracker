package com.example.habbittracker.ui.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habbittracker.data.HabitRepository
import com.example.habbittracker.data.ReminderRepository
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.Reminder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReminderListUiState(
    val reminders: List<Reminder> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val loaded: Boolean = false,
) {
    fun habitName(habitId: Long?): String? = habits.firstOrNull { it.id == habitId }?.name
}

class ReminderViewModel(
    private val repository: ReminderRepository,
    habitRepository: HabitRepository,
) : ViewModel() {
    val uiState: StateFlow<ReminderListUiState> =
        combine(repository.observeAll(), habitRepository.observeHabits()) { reminders, habits ->
            ReminderListUiState(
                reminders = reminders,
                habits = habits.filterNot { it.archived },
                loaded = true,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ReminderListUiState(),
        )

    fun onSave(reminder: Reminder) {
        viewModelScope.launch { repository.upsert(reminder) }
    }

    fun onDelete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun onToggle(reminder: Reminder, enabled: Boolean) {
        viewModelScope.launch { repository.upsert(reminder.copy(enabled = enabled)) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
