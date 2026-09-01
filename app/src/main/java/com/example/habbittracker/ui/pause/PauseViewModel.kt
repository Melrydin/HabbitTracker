package com.example.habbittracker.ui.pause

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habbittracker.data.HabitRepository
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.Pause
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PauseListUiState(
    val pauses: List<Pause> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val loaded: Boolean = false,
) {
    fun habitName(habitId: Long?): String? = habits.firstOrNull { it.id == habitId }?.name
}

class PauseViewModel(private val repository: HabitRepository) : ViewModel() {
    val uiState: StateFlow<PauseListUiState> =
        combine(repository.observePauses(), repository.observeHabits()) { pauses, habits ->
            PauseListUiState(
                pauses = pauses.sortedBy { it.from },
                habits = habits.filterNot { it.archived },
                loaded = true,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = PauseListUiState(),
        )

    fun onSave(pause: Pause) {
        viewModelScope.launch { repository.upsertPause(pause) }
    }

    fun onDelete(id: Long) {
        viewModelScope.launch { repository.deletePause(id) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
