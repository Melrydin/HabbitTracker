package com.example.habbittracker

import android.app.Application
import com.example.habbittracker.data.HabitRepository
import com.example.habbittracker.data.InMemoryHabitRepository

/**
 * Einfacher Service-Locator. Hier wird spaeter die Room-Datenbank aufgebaut,
 * ohne dass Screens oder ViewModels sich aendern.
 */
class AppContainer {
    val habitRepository: HabitRepository by lazy { InMemoryHabitRepository() }
}

class HabbitTrackerApp : Application() {
    val container: AppContainer by lazy { AppContainer() }
}
