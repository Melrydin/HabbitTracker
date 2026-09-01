package com.example.habbittracker.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.habbittracker.domain.model.Day
import com.example.habbittracker.domain.model.DayStatus
import com.example.habbittracker.domain.model.Goal
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitKind
import com.example.habbittracker.domain.model.HabitType
import com.example.habbittracker.domain.model.Pause
import com.example.habbittracker.domain.model.Polarity
import com.example.habbittracker.domain.model.Recurrence
import com.example.habbittracker.domain.model.Reminder
import com.example.habbittracker.domain.model.StreakRule
import com.example.habbittracker.domain.model.WeekSpan
import java.time.LocalDate
import java.time.LocalTime

/**
 * Stored form of [Habit] (F1).
 *
 * The columns for F4, F8, F11 and F12 exist from the MVP on although their UI
 * only arrives in V2, so those features need no migration later.
 *
 * `parent_id` points at the weekly habit a sub habit belongs to; deleting the
 * parent takes its sub habits with it (F8).
 */
@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("parent_id"), Index("sort_index")],
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: HabitType,
    val target: Int,
    val unit: String?,
    val points: Int,
    val required: Boolean,
    val icon: String,
    @ColumnInfo(name = "color_tag") val colorTag: Int?,
    val note: String?,
    val archived: Boolean,
    val kind: HabitKind,
    @ColumnInfo(name = "parent_id") val parentId: Long?,
    @ColumnInfo(name = "week_start") val weekStart: LocalDate?,
    @ColumnInfo(name = "week_span") val weekSpan: WeekSpan?,
    val recurrence: Recurrence?,
    @ColumnInfo(name = "assigned_dows") val assignedDows: Set<Int>,
    @ColumnInfo(name = "gives_theme") val givesTheme: Boolean,
    @ColumnInfo(name = "is_theme_generated") val isThemeGenerated: Boolean,
    @ColumnInfo(name = "streak_rule") val streakRule: StreakRule,
    @ColumnInfo(name = "per_week_target") val perWeekTarget: Int?,
    val polarity: Polarity,
    val category: String?,
    val tags: Set<String>,
    @ColumnInfo(name = "sort_index") val sortIndex: Int,
)

/**
 * Stored form of [Day] (F2).
 *
 * `theme_habit_id` has no foreign key on purpose: the theme is a display detail,
 * and a deleted habit should clear it rather than take the day down with it.
 */
@Entity(tableName = "days")
data class DayEntity(
    @PrimaryKey val date: LocalDate,
    @ColumnInfo(name = "theme_habit_id") val themeHabitId: Long?,
    @ColumnInfo(name = "day_note") val dayNote: String?,
    @ColumnInfo(name = "goal_type") val goalType: GoalType,
    @ColumnInfo(name = "goal_threshold") val goalThreshold: Int,
    @ColumnInfo(name = "goal_overridden", defaultValue = "0") val goalOverridden: Boolean,
    @ColumnInfo(name = "freeze_used", defaultValue = "0") val freezeUsed: Boolean,
    val status: DayStatus,
)

/**
 * A habit's recorded value on one day (F3).
 *
 * Deleting a habit cascades to its recorded values, so no orphan rows survive a
 * deletion. Archiving does not touch these rows, which is what keeps old entries
 * visible on days that already had a value.
 */
@Entity(
    tableName = "day_habits",
    primaryKeys = ["date", "habit_id"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habit_id")],
)
data class DayHabitEntity(
    val date: LocalDate,
    @ColumnInfo(name = "habit_id") val habitId: Long,
    val progress: Int,
)

/**
 * Stored form of [Reminder] (F5).
 *
 * A reminder for a habit goes when the habit does; a general one has no habit to
 * hang on and keeps a null `habit_id`.
 */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habit_id")],
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: LocalTime,
    @ColumnInfo(name = "days_of_week") val daysOfWeek: Set<Int>,
    @ColumnInfo(name = "habit_id") val habitId: Long?,
    val enabled: Boolean,
)

fun PauseEntity.toDomain() = Pause(id = id, from = from, to = to, habitId = habitId)

fun Pause.toEntity() = PauseEntity(id = id, from = from, to = to, habitId = habitId)

fun ReminderEntity.toDomain() =
    Reminder(id = id, time = time, daysOfWeek = daysOfWeek, habitId = habitId, enabled = enabled)

fun Reminder.toEntity() =
    ReminderEntity(id = id, time = time, daysOfWeek = daysOfWeek, habitId = habitId, enabled = enabled)

/** Stored form of [Goal] (F10, V3). Empty until that feature lands. */
@Entity(
    tableName = "goals",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habit_id")],
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "habit_id") val habitId: Long,
    @ColumnInfo(name = "target_count") val targetCount: Int,
    @ColumnInfo(name = "period_start") val periodStart: LocalDate,
    @ColumnInfo(name = "period_end") val periodEnd: LocalDate,
    val reward: String?,
    val achieved: Boolean,
)

/** Stored form of [Pause] (F4). */
@Entity(
    tableName = "pauses",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habit_id")],
)
data class PauseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "from_date") val from: LocalDate,
    @ColumnInfo(name = "to_date") val to: LocalDate,
    @ColumnInfo(name = "habit_id") val habitId: Long?,
)

// --- Mapping between storage and domain ---

fun HabitEntity.toDomain() =
    Habit(
        id = id,
        name = name,
        type = type,
        target = target,
        unit = unit,
        points = points,
        required = required,
        icon = icon,
        colorTag = colorTag,
        note = note,
        archived = archived,
        kind = kind,
        parentId = parentId,
        weekStart = weekStart,
        weekSpan = weekSpan,
        recurrence = recurrence,
        assignedDows = assignedDows,
        givesTheme = givesTheme,
        isThemeGenerated = isThemeGenerated,
        streakRule = streakRule,
        perWeekTarget = perWeekTarget,
        polarity = polarity,
        category = category,
        tags = tags,
        sortIndex = sortIndex,
    )

fun Habit.toEntity() =
    HabitEntity(
        id = id,
        name = name,
        type = type,
        target = target,
        unit = unit,
        points = points,
        required = required,
        icon = icon,
        colorTag = colorTag,
        note = note,
        archived = archived,
        kind = kind,
        parentId = parentId,
        weekStart = weekStart,
        weekSpan = weekSpan,
        recurrence = recurrence,
        assignedDows = assignedDows,
        givesTheme = givesTheme,
        isThemeGenerated = isThemeGenerated,
        streakRule = streakRule,
        perWeekTarget = perWeekTarget,
        polarity = polarity,
        category = category,
        tags = tags,
        sortIndex = sortIndex,
    )

fun DayEntity.toDomain() =
    Day(
        date = date,
        themeHabitId = themeHabitId,
        dayNote = dayNote,
        goalType = goalType,
        goalThreshold = goalThreshold,
        goalOverridden = goalOverridden,
        freezeUsed = freezeUsed,
        status = status,
    )

fun Day.toEntity() =
    DayEntity(
        date = date,
        themeHabitId = themeHabitId,
        dayNote = dayNote,
        goalType = goalType,
        goalThreshold = goalThreshold,
        goalOverridden = goalOverridden,
        freezeUsed = freezeUsed,
        status = status,
    )
