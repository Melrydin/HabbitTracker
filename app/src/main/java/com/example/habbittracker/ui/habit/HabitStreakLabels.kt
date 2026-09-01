package com.example.habbittracker.ui.habit

import androidx.annotation.StringRes
import com.example.habbittracker.R
import com.example.habbittracker.domain.model.StreakRule

/** Label for the streak rule, kept in one place. */
@StringRes
fun StreakRule.labelRes(): Int =
    when (this) {
        StreakRule.DAILY -> R.string.habit_editor_streak_daily
        StreakRule.WEEKLY_COUNT -> R.string.habit_editor_streak_weekly
    }
