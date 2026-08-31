package com.example.habbittracker.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habbittracker.data.HabitRepository
import com.example.habbittracker.domain.model.Habit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HabitListUiState(
    val active: List<Habit> = emptyList(),
    val archived: List<Habit> = emptyList(),
    val loaded: Boolean = false,
) {
    val isEmpty: Boolean get() = active.isEmpty() && archived.isEmpty()
}

class HabitListViewModel(repository: HabitRepository) : ViewModel() {
    val uiState: StateFlow<HabitListUiState> =
        repository
            .observeHabits()
            .map { habits ->
                val (archived, active) = habits.partition { it.archived }
                HabitListUiState(active = active, archived = archived, loaded = true)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = HabitListUiState(),
            )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
