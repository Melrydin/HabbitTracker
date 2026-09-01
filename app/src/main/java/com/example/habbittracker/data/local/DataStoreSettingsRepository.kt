package com.example.habbittracker.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.habbittracker.data.SettingsRepository
import com.example.habbittracker.data.toEnumOr
import com.example.habbittracker.domain.model.AppSettings
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Stores the settings in a preferences DataStore.
 *
 * Unknown or damaged values fall back to the default instead of throwing: a
 * settings file the app cannot read should not stop it from starting.
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val settings: Flow<AppSettings> =
        dataStore.data.map { preferences ->
            AppSettings(
                themeMode = preferences[THEME_MODE].toEnumOr(AppSettings().themeMode),
                defaultGoalType = preferences[GOAL_TYPE].toEnumOr(AppSettings().defaultGoalType),
                defaultGoalThreshold = preferences[GOAL_THRESHOLD] ?: AppSettings.DEFAULT_GOAL_THRESHOLD,
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    override suspend fun setDefaultGoal(type: GoalType, threshold: Int) {
        dataStore.edit {
            it[GOAL_TYPE] = type.name
            it[GOAL_THRESHOLD] =
                threshold.coerceIn(
                    AppSettings.GOAL_THRESHOLD_MIN,
                    AppSettings.GOAL_THRESHOLD_MAX,
                )
        }
    }

    override suspend fun replace(settings: AppSettings) {
        dataStore.edit {
            it[THEME_MODE] = settings.themeMode.name
            it[GOAL_TYPE] = settings.defaultGoalType.name
            it[GOAL_THRESHOLD] =
                settings.defaultGoalThreshold.coerceIn(
                    AppSettings.GOAL_THRESHOLD_MIN,
                    AppSettings.GOAL_THRESHOLD_MAX,
                )
        }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val GOAL_TYPE = stringPreferencesKey("default_goal_type")
        val GOAL_THRESHOLD = intPreferencesKey("default_goal_threshold")
    }
}
