package com.example.habbittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habbittracker.domain.model.AppSettings
import com.example.habbittracker.domain.model.ThemeMode
import com.example.habbittracker.ui.navigation.HabitNavHost
import com.example.habbittracker.ui.theme.HabbitTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as HabbitTrackerApp).container

        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(AppSettings())
            val darkTheme =
                when (settings.themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }

            HabbitTrackerTheme(darkTheme = darkTheme) {
                HabitNavHost(
                    container = container,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
