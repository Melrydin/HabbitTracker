package com.example.habbittracker.ui.settings

import androidx.annotation.StringRes
import com.example.habbittracker.R
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.ThemeMode

/** Labels for the settings enums, kept next to each other so they stay consistent. */
@StringRes
fun ThemeMode.labelRes(): Int =
    when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }

@StringRes
fun GoalType.labelRes(): Int =
    when (this) {
        GoalType.ALL_REQUIRED -> R.string.settings_goal_rule_all_required
        GoalType.MIN_COUNT -> R.string.settings_goal_rule_min_count
        GoalType.POINTS -> R.string.settings_goal_rule_points
    }
