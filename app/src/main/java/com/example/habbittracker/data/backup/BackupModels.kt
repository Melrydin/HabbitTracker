package com.example.habbittracker.data.backup

import kotlinx.serialization.Serializable

/**
 * The wire format of a backup (F6).
 *
 * These types are deliberately separate from the domain models and the Room
 * entities. A backup written today has to stay readable after the app has
 * refactored either of those, so the file format needs to be able to stand still
 * while the code moves.
 *
 * Everything is stored as text or numbers, dates in ISO-8601 and enums by name,
 * so a backup can be inspected and repaired in a text editor.
 */
@Serializable
data class BackupManifest(
    val appVersion: String,
    val schemaVersion: Int,
    val exportedAt: String,
)

@Serializable
data class BackupHabit(
    val id: Long,
    val name: String,
    val target: Int,
    val unit: String? = null,
    val points: Int = 1,
    val required: Boolean = false,
    val icon: String,
    val colorTag: Int? = null,
    val note: String? = null,
    val archived: Boolean = false,
    val kind: String = "SIMPLE",
    val parentId: Long? = null,
    val weekStart: String? = null,
    val weekSpan: String? = null,
    val recurrence: String? = null,
    val assignedDows: List<Int> = emptyList(),
    val givesTheme: Boolean = false,
    val isThemeGenerated: Boolean = false,
    val streakRule: String = "DAILY",
    val perWeekTarget: Int? = null,
    val polarity: String = "GOOD",
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val sortIndex: Int = 0,
)

@Serializable
data class BackupDay(
    val date: String,
    val themeHabitId: Long? = null,
    val dayNote: String? = null,
    val goalType: String,
    val goalThreshold: Int,
    val status: String,
)

@Serializable
data class BackupDayHabit(
    val date: String,
    val habitId: Long,
    val progress: Int,
)

@Serializable
data class BackupGoal(
    val id: Long,
    val habitId: Long,
    val targetCount: Int,
    val periodStart: String,
    val periodEnd: String,
    val reward: String? = null,
    val achieved: Boolean = false,
)

@Serializable
data class BackupSettings(
    val themeMode: String,
    val defaultGoalType: String,
    val defaultGoalThreshold: Int,
)

/** Names of the entries inside the ZIP, fixed by the feature list. */
object BackupEntries {
    const val MANIFEST = "manifest.json"
    const val HABITS = "habits.json"
    const val DAYS = "days.json"
    const val DAY_HABITS = "day_habits.json"
    const val GOALS = "goals.json"
    const val SETTINGS = "settings.json"
}
