package com.example.habbittracker.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SmokeFree
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * `Habit.icon` haelt den Namen aus dem Material-Symbols-Set (F1). Hier wird er
 * auf genau ein Icon-Set abgebildet: outlined, keine Mischung.
 *
 * Dieselbe Liste dient spaeter als Auswahl im Habit-Editor.
 */
object HabitIcons {
    const val FALLBACK = "task_alt"

    val catalog: Map<String, ImageVector> =
        linkedMapOf(
            FALLBACK to Icons.Outlined.TaskAlt,
            "water_drop" to Icons.Outlined.WaterDrop,
            "directions_run" to Icons.AutoMirrored.Outlined.DirectionsRun,
            "directions_bike" to Icons.AutoMirrored.Outlined.DirectionsBike,
            "fitness_center" to Icons.Outlined.FitnessCenter,
            "self_improvement" to Icons.Outlined.SelfImprovement,
            "menu_book" to Icons.AutoMirrored.Outlined.MenuBook,
            "edit_note" to Icons.Outlined.EditNote,
            "bedtime" to Icons.Outlined.Bedtime,
            "restaurant" to Icons.Outlined.Restaurant,
            "medication" to Icons.Outlined.Medication,
            "mood" to Icons.Outlined.Mood,
            "local_florist" to Icons.Outlined.LocalFlorist,
            "smoke_free" to Icons.Outlined.SmokeFree,
            "music_note" to Icons.Outlined.MusicNote,
            "code" to Icons.Outlined.Code,
            "call" to Icons.Outlined.Call,
            "cleaning_services" to Icons.Outlined.CleaningServices,
            "savings" to Icons.Outlined.Savings,
        )

    /** Unbekannte Namen (z. B. aus einem aelteren Backup) fallen auf ein neutrales Icon zurueck. */
    operator fun get(name: String): ImageVector = catalog[name] ?: catalog.getValue(FALLBACK)
}
