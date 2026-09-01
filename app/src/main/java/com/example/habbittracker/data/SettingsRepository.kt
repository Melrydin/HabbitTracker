package com.example.habbittracker.data

import com.example.habbittracker.domain.model.AppSettings
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * App settings (F7). Kept apart from [HabitRepository]: settings are a handful of
 * scalars with no relations, which is what DataStore is for.
 */
interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    /** The rule and threshold new days start with; existing days keep their own. */
    suspend fun setDefaultGoal(type: GoalType, threshold: Int)
}
