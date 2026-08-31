package com.example.habbittracker

import android.app.Application
import com.example.habbittracker.data.HabitRepository
import com.example.habbittracker.data.InMemoryHabitRepository

/**
 * A simple service locator. The Room database will later be built here without
 * screens or view models having to change.
 */
class AppContainer {
    val habitRepository: HabitRepository by lazy { InMemoryHabitRepository() }
}

class HabbitTrackerApp : Application() {
    val container: AppContainer by lazy { AppContainer() }
}
