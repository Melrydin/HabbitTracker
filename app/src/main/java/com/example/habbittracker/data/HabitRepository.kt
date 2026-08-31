package com.example.habbittracker.data

import com.example.habbittracker.domain.model.Day
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Alles, was der Heute-Screen ueber einen Tag wissen muss. */
data class DaySnapshot(
    val day: Day,
    val entries: List<HabitEntry>,
    val currentStreak: Int,
)

/**
 * Zugriff auf Habits und Tageserfassung. Die Implementierung wechselt spaeter
 * von [InMemoryHabitRepository] auf Room, ohne dass ViewModel oder UI sich aendern.
 */
interface HabitRepository {

    // --- Tageserfassung (F2, F3) ---

    fun observeDay(date: LocalDate): Flow<DaySnapshot>

    /** Setzt den Ist-Wert und rechnet `Day.passed` sowie die Streak neu (F3). */
    suspend fun setProgress(date: LocalDate, habitId: Long, progress: Int)

    /** Leerer Text loescht das Thema, deshalb `null` statt Leerstring (F2). */
    suspend fun setDayTheme(date: LocalDate, theme: String?)

    // --- Habit-Verwaltung (F1) ---

    /** Alle Habits inklusive archivierter. Wer nur aktive braucht, filtert selbst. */
    fun observeHabits(): Flow<List<Habit>>

    /**
     * Einmaliges Laden fuer den Editor. Bewusst kein Flow: das Formular soll
     * waehrend der Bearbeitung nicht unter der Hand aktualisiert werden.
     */
    suspend fun getHabit(id: Long): Habit?

    /** Legt an, wenn `habit.id == NEW_HABIT_ID`, sonst aktualisiert. Gibt die Id zurueck. */
    suspend fun upsertHabit(habit: Habit): Long

    /** Archivieren statt loeschen: der Verlauf bleibt erhalten (F1). */
    suspend fun setArchived(id: Long, archived: Boolean)

    /** Entfernt den Habit und alle erfassten Werte. */
    suspend fun deleteHabit(id: Long)

    companion object {
        /** Id fuer einen noch nicht gespeicherten Habit, passend zu Rooms `autoGenerate`. */
        const val NEW_HABIT_ID = 0L
    }
}
