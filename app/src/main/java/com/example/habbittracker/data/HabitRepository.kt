package com.example.habbittracker.data

import com.example.habbittracker.domain.model.Day
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Everything the today screen needs to know about a single day. */
data class DaySnapshot(
    val day: Day,
    val entries: List<HabitEntry>,
    val currentStreak: Int,
)

/**
 * Access to habits and daily tracking. The implementation later moves from
 * [InMemoryHabitRepository] to Room without any change to view models or UI.
 */
interface HabitRepository {
    // --- Daily tracking (F2, F3) ---

    fun observeDay(date: LocalDate): Flow<DaySnapshot>

    /** Stores the recorded value and recomputes `Day.passed` and the streak (F3). */
    suspend fun setProgress(date: LocalDate, habitId: Long, progress: Int)

    /** Empty text clears the theme, hence `null` rather than an empty string (F2). */
    suspend fun setDayTheme(date: LocalDate, theme: String?)

    // --- Habit management (F1) ---

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
