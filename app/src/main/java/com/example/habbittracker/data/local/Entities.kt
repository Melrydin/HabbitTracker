package com.example.habbittracker.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.habbittracker.domain.model.Day
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitType
import java.time.LocalDate

/** Stored form of [Habit] (F1). */
@Entity(tableName = "habits")
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
    val archived: Boolean,
)

/** Stored form of [Day] (F2). */
@Entity(tableName = "days")
data class DayEntity(
    @PrimaryKey val date: LocalDate,
    val theme: String?,
    @ColumnInfo(name = "goal_type") val goalType: GoalType,
    @ColumnInfo(name = "goal_threshold") val goalThreshold: Int,
    val passed: Boolean,
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
        archived = archived,
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
        archived = archived,
    )

fun DayEntity.toDomain() =
    Day(
        date = date,
        theme = theme,
        goalType = goalType,
        goalThreshold = goalThreshold,
        passed = passed,
    )

fun Day.toEntity() =
    DayEntity(
        date = date,
        theme = theme,
        goalType = goalType,
        goalThreshold = goalThreshold,
        passed = passed,
    )
