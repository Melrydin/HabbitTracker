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

    /** The rule today and the days ahead are judged by, plus the points bar. */
    suspend fun setDefaultGoal(type: GoalType, threshold: Int)

    /** Grace days per month; zero switches the streak protection off (F4). */
    suspend fun setFreezePerMonth(value: Int)

    /** Overwrites everything at once, used when a backup is restored (F6). */
    suspend fun replace(settings: AppSettings)
}
