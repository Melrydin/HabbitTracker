package com.example.habbittracker.ui.habit

import androidx.annotation.StringRes
import com.example.habbittracker.R
import com.example.habbittracker.domain.model.HabitType

/** Beschriftung der Erfassungsart, an einer Stelle, damit Editor und Liste gleich sprechen. */
@StringRes
fun HabitType.labelRes(): Int = when (this) {
    HabitType.CHECK -> R.string.habit_type_check
    HabitType.COUNTER -> R.string.habit_type_counter
    HabitType.AMOUNT -> R.string.habit_type_amount
}
