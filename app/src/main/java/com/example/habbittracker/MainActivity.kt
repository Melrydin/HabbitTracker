package com.example.habbittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.habbittracker.ui.navigation.HabitNavHost
import com.example.habbittracker.ui.theme.HabbitTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as HabbitTrackerApp).container

        setContent {
            HabbitTrackerTheme {
                HabitNavHost(
                    container = container,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
