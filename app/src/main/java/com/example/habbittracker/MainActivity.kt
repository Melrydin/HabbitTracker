package com.example.habbittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.habbittracker.domain.model.AppSettings
import com.example.habbittracker.domain.model.ThemeMode
import com.example.habbittracker.ui.navigation.HabitNavHost
import com.example.habbittracker.ui.theme.HabbitTrackerTheme
import com.example.habbittracker.ui.widget.TodayWidget
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /**
     * Leaving the app pushes the widget once (F9).
     *
     * Glance keeps its own content up to date only while its session is running.
     * Once the host has torn that down, the home screen would keep whatever it
     * drew last, which is exactly the moment the user goes to look at it.
     */
    override fun onStop() {
        super.onStop()
        lifecycleScope.launch { TodayWidget().updateAll(this@MainActivity) }
    }

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
