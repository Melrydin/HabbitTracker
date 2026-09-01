package com.example.habbittracker.ui.habit

import androidx.annotation.StringRes
import com.example.habbittracker.R
import com.example.habbittracker.domain.model.HabitKind
import com.example.habbittracker.domain.model.Recurrence
import com.example.habbittracker.domain.model.WeekSpan

/** Labels for where a habit sits in the week (F8), kept in one place. */
@StringRes
fun HabitKind.labelRes(): Int =
    when (this) {
        HabitKind.SIMPLE -> R.string.habit_editor_kind_simple
        HabitKind.WEEKLY -> R.string.habit_editor_kind_weekly
        HabitKind.SUB -> R.string.habit_editor_kind_sub
    }

@StringRes
fun WeekSpan.labelRes(): Int =
    when (this) {
        WeekSpan.WORKWEEK -> R.string.habit_editor_span_workweek
        WeekSpan.FULL -> R.string.habit_editor_span_full
    }

@StringRes
fun Recurrence.labelRes(): Int =
    when (this) {
        Recurrence.EVERY_DAY -> R.string.habit_editor_recurrence_every_day
        Recurrence.BY_SUBS -> R.string.habit_editor_recurrence_by_subs
    }
