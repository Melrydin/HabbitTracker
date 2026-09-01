package com.example.habbittracker.data

import com.example.habbittracker.domain.model.Day
import com.example.habbittracker.domain.model.DayStatus
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitEntry
import com.example.habbittracker.domain.model.Pause
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Everything the today screen needs to know about a single day. */
data class DaySnapshot(
    val day: Day,
    val entries: List<HabitEntry>,
    val currentStreak: Int,
) {
    /** Display name of the day theme, which is the name of its theme habit (F2). */
    val themeName: String?
        get() = entries.firstOrNull { it.habit.id == day.themeHabitId }?.habit?.name
}

/**
 * Access to habits and daily tracking. The implementation later moves from
 * [InMemoryHabitRepository] to Room without any change to view models or UI.
 */
interface HabitRepository {
    // --- Daily tracking (F2, F3) ---

    fun observeDay(date: LocalDate): Flow<DaySnapshot>

    /** Stores the recorded value and recomputes `Day.status` and the streak (F3). */
    suspend fun setProgress(date: LocalDate, habitId: Long, progress: Int)

    /**
     * Marks a habit as done for the day, whatever its target (F9).
     *
     * The one-tap check-in of the widget, the tile and the notification action all
     * mean this, so they share it rather than each looking the target up.
     */
    suspend fun completeHabit(date: LocalDate, habitId: Long)

    /**
     * Adds one step to a habit (F9). A check is ticked, a counter goes up by one.
     *
     * What the widget does on a tap: a counter of five glasses is filled glass by
     * glass there just as it is in the app, rather than jumping to done.
     */
    suspend fun incrementHabit(date: LocalDate, habitId: Long)

    /**
     * Sets the day theme (F2). The theme is always backed by a habit: a new name
     * creates one, an empty name clears the link again.
     */
    suspend fun setDayTheme(date: LocalDate, themeName: String?)

    /** Gives this one day a goal of its own, overriding the default (F2). */
    suspend fun setDayGoal(date: LocalDate, goalType: GoalType, threshold: Int)

    /** Drops that override again, so the day follows the setting once more. */
    suspend fun clearDayGoal(date: LocalDate)

    /** Empty text clears the note, hence `null` rather than an empty string (F2). */
    suspend fun setDayNote(date: LocalDate, note: String?)

    /** Status of every stored day, for streaks, heatmap and completion rate (F4). */
    fun observeDayStatuses(): Flow<Map<LocalDate, DayStatus>>

    /** The days that spent a grace day: marked in the history, skipped by streaks (F4). */
    fun observeFrozenDays(): Flow<Set<LocalDate>>

    /**
     * Judges every stored day again. Needed for a setting that changes how days
     * already behind are judged rather than only the ones still to come (F4).
     */
    suspend fun refreshDays()

    /**
     * For one habit, every stored day it applied to and whether it was fulfilled
     * there (F4). Days the habit did not belong to are absent rather than false, so
     * a streak can skip them instead of counting them against the run.
     */
    fun observeHabitHistory(habitId: Long): Flow<Map<LocalDate, Boolean>>

    // --- Habit management (F1) ---

    // --- Pauses (F4) ---

    fun observePauses(): Flow<List<Pause>>

    /** A pause without a habit stops the whole app for those days. */
    suspend fun upsertPause(pause: Pause): Long

    suspend fun deletePause(id: Long)

    /** Every habit including archived ones. Callers that want only active ones filter themselves. */
    fun observeHabits(): Flow<List<Habit>>

    /**
     * A one-off load for the editor. Deliberately not a flow: the form must not
     * change underneath the user while they are editing it.
     */
    suspend fun getHabit(id: Long): Habit?

    /** Inserts when `habit.id == NEW_HABIT_ID`, updates otherwise. Returns the id. */
    suspend fun upsertHabit(habit: Habit): Long

    /** Archiving instead of deleting keeps the history intact (F1). */
    suspend fun setArchived(id: Long, archived: Boolean)

    /** Removes the habit along with every recorded value. */
    suspend fun deleteHabit(id: Long)

    companion object {
        /** Id of a habit that has not been stored yet, matching Room's `autoGenerate`. */
        const val NEW_HABIT_ID = 0L
    }
}
