package com.example.habbittracker

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.habbittracker.data.HabitRepository
import com.example.habbittracker.data.RoomHabitRepository
import com.example.habbittracker.data.SettingsRepository
import com.example.habbittracker.data.backup.BackupManager
import com.example.habbittracker.data.backup.ZipBackupRepository
import com.example.habbittracker.data.local.DataStoreSettingsRepository
import com.example.habbittracker.data.local.HabitDatabase
import com.example.habbittracker.data.local.MIGRATION_1_2

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * A simple service locator. Everything is built lazily, so a screen that never
 * touches the database does not open it either.
 */
class AppContainer(private val context: Context) {
    private val database: HabitDatabase by lazy {
        Room
            .databaseBuilder(context, HabitDatabase::class.java, HabitDatabase.NAME)
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(context.settingsDataStore)
    }

    val backupManager: BackupManager by lazy {
        BackupManager(
            context = context,
            repository =
                ZipBackupRepository(
                    database = database,
                    habitDao = database.habitDao(),
                    dayDao = database.dayDao(),
                    dayHabitDao = database.dayHabitDao(),
                    goalDao = database.goalDao(),
                    pauseDao = database.pauseDao(),
                    settingsRepository = settingsRepository,
                    appVersion = BuildConfig.VERSION_NAME,
                ),
        )
    }

    val habitRepository: HabitRepository by lazy {
        RoomHabitRepository(
            database = database,
            habitDao = database.habitDao(),
            dayDao = database.dayDao(),
            dayHabitDao = database.dayHabitDao(),
            settings = settingsRepository.settings,
        )
    }
}

class HabbitTrackerApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
