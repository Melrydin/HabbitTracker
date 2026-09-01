package com.example.habbittracker.data.local

import androidx.room.TypeConverter
import com.example.habbittracker.domain.model.DayStatus
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.HabitKind
import com.example.habbittracker.domain.model.Polarity
import com.example.habbittracker.domain.model.Recurrence
import com.example.habbittracker.domain.model.StreakRule
import com.example.habbittracker.domain.model.WeekSpan
import java.time.LocalDate

/**
 * Dates are stored as ISO-8601 text and enums by name. Both are readable when
 * inspecting the database by hand, and ISO dates still sort correctly as text.
 *
 * Sets are stored as a comma separated list. Neither weekdays nor tags contain a
 * comma, and the alternative, a join table, would be a lot of machinery for a
 * handful of values.
 */
class Converters {
    @TypeConverter
    fun dateToText(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun textToDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun goalTypeToText(value: GoalType): String = value.name

    @TypeConverter
    fun textToGoalType(value: String): GoalType = GoalType.valueOf(value)

    @TypeConverter
    fun dayStatusToText(value: DayStatus): String = value.name

    @TypeConverter
    fun textToDayStatus(value: String): DayStatus = DayStatus.valueOf(value)

    @TypeConverter
    fun habitKindToText(value: HabitKind): String = value.name

    @TypeConverter
    fun textToHabitKind(value: String): HabitKind = HabitKind.valueOf(value)

    @TypeConverter
    fun weekSpanToText(value: WeekSpan?): String? = value?.name

    @TypeConverter
    fun textToWeekSpan(value: String?): WeekSpan? = value?.let(WeekSpan::valueOf)

    @TypeConverter
    fun recurrenceToText(value: Recurrence?): String? = value?.name

    @TypeConverter
    fun textToRecurrence(value: String?): Recurrence? = value?.let(Recurrence::valueOf)

    @TypeConverter
    fun streakRuleToText(value: StreakRule): String = value.name

    @TypeConverter
    fun textToStreakRule(value: String): StreakRule = StreakRule.valueOf(value)

    @TypeConverter
    fun polarityToText(value: Polarity): String = value.name

    @TypeConverter
    fun textToPolarity(value: String): Polarity = Polarity.valueOf(value)

    @TypeConverter
    fun intSetToText(value: Set<Int>): String = value.sorted().joinToString(SEPARATOR)

    @TypeConverter
    fun textToIntSet(value: String): Set<Int> =
        value
            .split(SEPARATOR)
            .filter(String::isNotBlank)
            .map(String::toInt)
            .toSet()

    @TypeConverter
    fun stringSetToText(value: Set<String>): String = value.joinToString(SEPARATOR)

    @TypeConverter
    fun textToStringSet(value: String): Set<String> =
        value.split(SEPARATOR).filter(String::isNotBlank).toSet()

    private companion object {
        const val SEPARATOR = ","
    }
}
