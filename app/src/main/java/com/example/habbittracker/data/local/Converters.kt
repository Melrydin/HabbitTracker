package com.example.habbittracker.data.local

import androidx.room.TypeConverter
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.HabitType
import java.time.LocalDate

/**
 * Dates are stored as ISO-8601 text and enums by name. Both are readable when
 * inspecting the database by hand, and ISO dates still sort correctly as text.
 */
class Converters {
    @TypeConverter
    fun dateToText(value: LocalDate): String = value.toString()

    @TypeConverter
    fun textToDate(value: String): LocalDate = LocalDate.parse(value)

    @TypeConverter
    fun habitTypeToText(value: HabitType): String = value.name

    @TypeConverter
    fun textToHabitType(value: String): HabitType = HabitType.valueOf(value)

    @TypeConverter
    fun goalTypeToText(value: GoalType): String = value.name

    @TypeConverter
    fun textToGoalType(value: String): GoalType = GoalType.valueOf(value)
}
