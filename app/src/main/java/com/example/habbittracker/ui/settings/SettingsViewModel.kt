package com.example.habbittracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habbittracker.data.SettingsRepository
import com.example.habbittracker.domain.model.AppSettings
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<AppSettings> =
        repository.settings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppSettings(),
        )

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun onGoalTypeChange(type: GoalType) {
        viewModelScope.launch { repository.setDefaultGoal(type, settings.value.defaultGoalThreshold) }
    }

    fun onThresholdChange(threshold: Int) {
        viewModelScope.launch { repository.setDefaultGoal(settings.value.defaultGoalType, threshold) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
