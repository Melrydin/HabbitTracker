package com.example.habbittracker.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habbittracker.data.HabitRepository
import com.example.habbittracker.data.SettingsRepository
import com.example.habbittracker.data.backup.BackupManager
import com.example.habbittracker.data.backup.BackupOutcome
import com.example.habbittracker.domain.model.AppSettings
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.ThemeMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val habits: HabitRepository,
    private val backupManager: BackupManager,
) : ViewModel() {
    val settings: StateFlow<AppSettings> =
        repository.settings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppSettings(),
        )

    private val outcomes = Channel<BackupOutcome>(Channel.BUFFERED)
    val backupOutcomes = outcomes.receiveAsFlow()

    fun suggestedFileName(): String = backupManager.suggestedFileName()

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun onGoalTypeChange(type: GoalType) {
        viewModelScope.launch { repository.setDefaultGoal(type, settings.value.defaultGoalThreshold) }
    }

    fun onThresholdChange(threshold: Int) {
        viewModelScope.launch { repository.setDefaultGoal(settings.value.defaultGoalType, threshold) }
    }

    /** The budget also decides how days already behind are judged, so they are judged again. */
    fun onFreezeChange(value: Int) {
        viewModelScope.launch {
            repository.setFreezePerMonth(value)
            habits.refreshDays()
        }
    }

    fun onExport(target: Uri) {
        viewModelScope.launch { outcomes.send(backupManager.exportTo(target)) }
    }

    fun onImport(source: Uri) {
        viewModelScope.launch { outcomes.send(backupManager.importFrom(source)) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
