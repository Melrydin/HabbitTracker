package com.example.habbittracker

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.habbittracker.data.HabitRepository
import com.example.habbittracker.data.RoomHabitRepository
import com.example.habbittracker.data.local.HabitDatabase

/**
 * A simple service locator. Everything is built lazily, so a screen that never
 * touches the database does not open it either.
 */
class AppContainer(private val context: Context) {
    private val database: HabitDatabase by lazy {
        Room
            .databaseBuilder(context, HabitDatabase::class.java, HabitDatabase.NAME)
            .build()
    }

    val habitRepository: HabitRepository by lazy {
        RoomHabitRepository(
            database = database,
            habitDao = database.habitDao(),
            dayDao = database.dayDao(),
            dayHabitDao = database.dayHabitDao(),
        )
    }
}

class HabbitTrackerApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
