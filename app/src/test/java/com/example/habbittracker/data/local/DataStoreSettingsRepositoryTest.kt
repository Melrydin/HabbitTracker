package com.example.habbittracker.data.local

import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.habbittracker.domain.model.AppSettings
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class DataStoreSettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun newStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { folder.newFile("settings.preferences_pb") }

    @Test
    fun `an untouched store yields the documented defaults`() =
        runBlocking {
            val settings = DataStoreSettingsRepository(newStore()).settings.first()

            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
            assertEquals(GoalType.POINTS, settings.defaultGoalType)
            assertEquals(AppSettings.DEFAULT_GOAL_THRESHOLD, settings.defaultGoalThreshold)
        }

    @Test
    fun `the theme mode survives a write`() =
        runBlocking {
            val repository = DataStoreSettingsRepository(newStore())

            repository.setThemeMode(ThemeMode.DARK)

            assertEquals(ThemeMode.DARK, repository.settings.first().themeMode)
        }

    @Test
    fun `the default goal survives a write`() =
        runBlocking {
            val repository = DataStoreSettingsRepository(newStore())

            repository.setDefaultGoal(GoalType.MIN_COUNT, threshold = 4)

            val settings = repository.settings.first()
            assertEquals(GoalType.MIN_COUNT, settings.defaultGoalType)
            assertEquals(4, settings.defaultGoalThreshold)
        }

    @Test
    fun `the points threshold stays within its range`() =
        runBlocking {
            val repository = DataStoreSettingsRepository(newStore())

            repository.setDefaultGoal(GoalType.POINTS, threshold = 0)
            assertEquals(
                AppSettings.GOAL_THRESHOLD_MIN,
                repository.settings.first().defaultGoalThreshold,
            )

            repository.setDefaultGoal(GoalType.POINTS, threshold = 500)
            assertEquals(
                AppSettings.GOAL_THRESHOLD_MAX,
                repository.settings.first().defaultGoalThreshold,
            )
        }

    @Test
    fun `an unreadable value falls back instead of throwing`() =
        runBlocking {
            val store = newStore()
            val repository = DataStoreSettingsRepository(store)

            // A value written by a future version, or a corrupted file.
            store.edit { it[stringPreferencesKey("theme_mode")] = "MIDNIGHT" }

            assertEquals(ThemeMode.SYSTEM, repository.settings.first().themeMode)
        }
}
