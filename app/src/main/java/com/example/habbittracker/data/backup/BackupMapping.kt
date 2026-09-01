package com.example.habbittracker.data.backup

import com.example.habbittracker.data.local.DayEntity
import com.example.habbittracker.data.local.DayHabitEntity
import com.example.habbittracker.data.local.GoalEntity
import com.example.habbittracker.data.local.HabitEntity
import com.example.habbittracker.domain.model.AppSettings
import com.example.habbittracker.domain.model.DayStatus
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.HabitKind
import com.example.habbittracker.domain.model.HabitType
import com.example.habbittracker.domain.model.Polarity
import com.example.habbittracker.domain.model.Recurrence
import com.example.habbittracker.domain.model.StreakRule
import com.example.habbittracker.domain.model.ThemeMode
import com.example.habbittracker.domain.model.WeekSpan
import java.time.LocalDate

/**
 * Translation between the stored entities and the backup format (F6).
 *
 * Enums are written by name and read back leniently: a value this build does not
 * know falls back to the default rather than failing the whole restore, so one
 * unexpected word cannot cost a user their history.
 */

private inline fun <reified T : Enum<T>> String?.toEnum(fallback: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: fallback

/** Amount habits were merged into counters; older backups still name them. */
private fun String?.toHabitType(): HabitType =
    if (this == LEGACY_AMOUNT) HabitType.COUNTER else toEnum(HabitType.CHECK)

private const val LEGACY_AMOUNT = "AMOUNT"

private fun String?.toDate(): LocalDate? = this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

fun HabitEntity.toBackup() =
    BackupHabit(
        id = id,
        name = name,
        type = type.name,
        target = target,
        unit = unit,
        points = points,
        required = required,
        icon = icon,
        colorTag = colorTag,
        note = note,
        archived = archived,
        kind = kind.name,
        parentId = parentId,
        weekStart = weekStart?.toString(),
        weekSpan = weekSpan?.name,
        recurrence = recurrence?.name,
        assignedDows = assignedDows.sorted(),
        givesTheme = givesTheme,
        isThemeGenerated = isThemeGenerated,
        streakRule = streakRule.name,
        perWeekTarget = perWeekTarget,
        polarity = polarity.name,
        category = category,
        tags = tags.toList(),
        sortIndex = sortIndex,
    )

fun BackupHabit.toEntity() =
    HabitEntity(
        id = id,
        name = name,
        type = type.toHabitType(),
        target = target,
        unit = unit,
        points = points,
        required = required,
        icon = icon,
        colorTag = colorTag,
        note = note,
        archived = archived,
        kind = kind.toEnum(HabitKind.SIMPLE),
        parentId = parentId,
        weekStart = weekStart.toDate(),
        weekSpan = weekSpan?.let { it.toEnum(WeekSpan.FULL) },
        recurrence = recurrence?.let { it.toEnum(Recurrence.EVERY_DAY) },
        assignedDows = assignedDows.filter { it in 1..7 }.toSet(),
        givesTheme = givesTheme,
        isThemeGenerated = isThemeGenerated,
        streakRule = streakRule.toEnum(StreakRule.DAILY),
        perWeekTarget = perWeekTarget,
        polarity = polarity.toEnum(Polarity.GOOD),
        category = category,
        tags = tags.toSet(),
        sortIndex = sortIndex,
    )

fun DayEntity.toBackup() =
    BackupDay(
        date = date.toString(),
        themeHabitId = themeHabitId,
        dayNote = dayNote,
        goalType = goalType.name,
        goalThreshold = goalThreshold,
        status = status.name,
    )

fun BackupDay.toEntity() =
    DayEntity(
        date = LocalDate.parse(date),
        themeHabitId = themeHabitId,
        dayNote = dayNote,
        goalType = goalType.toEnum(GoalType.POINTS),
        goalThreshold = goalThreshold,
        status = status.toEnum(DayStatus.NEUTRAL),
    )

fun DayHabitEntity.toBackup() =
    BackupDayHabit(
        date = date.toString(),
        habitId = habitId,
        progress = progress,
    )

fun BackupDayHabit.toEntity() =
    DayHabitEntity(
        date = LocalDate.parse(date),
        habitId = habitId,
        progress = progress,
    )

fun GoalEntity.toBackup() =
    BackupGoal(
        id = id,
        habitId = habitId,
        targetCount = targetCount,
        periodStart = periodStart.toString(),
        periodEnd = periodEnd.toString(),
        reward = reward,
        achieved = achieved,
    )

fun BackupGoal.toEntity() =
    GoalEntity(
        id = id,
        habitId = habitId,
        targetCount = targetCount,
        periodStart = LocalDate.parse(periodStart),
        periodEnd = LocalDate.parse(periodEnd),
        reward = reward,
        achieved = achieved,
    )

fun AppSettings.toBackup() =
    BackupSettings(
        themeMode = themeMode.name,
        defaultGoalType = defaultGoalType.name,
        defaultGoalThreshold = defaultGoalThreshold,
    )

fun BackupSettings.toDomain() =
    AppSettings(
        themeMode = themeMode.toEnum(ThemeMode.SYSTEM),
        defaultGoalType = defaultGoalType.toEnum(GoalType.POINTS),
        defaultGoalThreshold = defaultGoalThreshold,
    )
