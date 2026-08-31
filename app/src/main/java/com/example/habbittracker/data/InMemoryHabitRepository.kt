package com.example.habbittracker.data

import com.example.habbittracker.data.HabitRepository.Companion.NEW_HABIT_ID
import com.example.habbittracker.domain.DayEvaluator
import com.example.habbittracker.domain.StreakCalculator
import com.example.habbittracker.domain.model.Day
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitEntry
import com.example.habbittracker.domain.model.HabitType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * Platzhalter-Repository mit Beispieldaten, damit die Screens ohne Datenbank
 * lauffaehig und im Emulator bedienbar sind.
 *
 * TODO(Room): durch eine Room-Implementierung ersetzen. Die Regeln fuer `passed`
 *  und Streak liegen bereits in [DayEvaluator] und [StreakCalculator] und werden
 *  dabei unveraendert uebernommen.
 */
class InMemoryHabitRepository(
    today: LocalDate = LocalDate.now(),
) : HabitRepository {

    private data class Store(
        val habits: List<Habit>,
        val days: Map<LocalDate, Day>,
        val progress: Map<Pair<LocalDate, Long>, Int>,
        val nextId: Long,
    )

    private val writeLock = Mutex()
    private val store = MutableStateFlow(seed(today))

    override fun observeDay(date: LocalDate): Flow<DaySnapshot> = store.map { current ->
        DaySnapshot(
            day = current.days[date] ?: defaultDay(date),
            entries = current.entriesFor(date),
            currentStreak = StreakCalculator.currentStreak(
                passedDates = current.days.filterValues { it.passed }.keys,
                today = date,
            ),
        )
    }

    override suspend fun setProgress(date: LocalDate, habitId: Long, progress: Int) = writeLock.withLock {
        val current = store.value
        val habit = current.habits.firstOrNull { it.id == habitId } ?: return@withLock
        val clamped = when (habit.type) {
            // CHECK kennt nur 0 oder 1, Zaehler duerfen ihr Ziel ueberschreiten.
            HabitType.CHECK -> progress.coerceIn(0, 1)
            HabitType.COUNTER, HabitType.AMOUNT -> progress.coerceIn(0, PROGRESS_MAX)
        }
        val updated = current.copy(progress = current.progress + ((date to habitId) to clamped))
        store.value = updated.withRecalculatedDays()
    }

    override suspend fun setDayTheme(date: LocalDate, theme: String?) = writeLock.withLock {
        val current = store.value
        val day = current.days[date] ?: defaultDay(date)
        val cleaned = theme?.trim()?.take(Day.THEME_MAX_LENGTH)?.ifEmpty { null }
        store.value = current.copy(days = current.days + (date to day.copy(theme = cleaned)))
    }

    override fun observeHabits(): Flow<List<Habit>> = store.map { it.habits }

    override suspend fun getHabit(id: Long): Habit? = store.value.habits.firstOrNull { it.id == id }

    override suspend fun upsertHabit(habit: Habit): Long = writeLock.withLock {
        val current = store.value
        if (habit.id == NEW_HABIT_ID) {
            val created = habit.copy(id = current.nextId)
            store.value = current
                .copy(habits = current.habits + created, nextId = current.nextId + 1)
                .withRecalculatedDays()
            created.id
        } else {
            store.value = current
                .copy(habits = current.habits.map { if (it.id == habit.id) habit else it })
                .withRecalculatedDays()
            habit.id
        }
    }

    override suspend fun setArchived(id: Long, archived: Boolean) = writeLock.withLock {
        val current = store.value
        store.value = current
            .copy(habits = current.habits.map { if (it.id == id) it.copy(archived = archived) else it })
            .withRecalculatedDays()
    }

    override suspend fun deleteHabit(id: Long) = writeLock.withLock {
        val current = store.value
        store.value = current
            .copy(
                habits = current.habits.filterNot { it.id == id },
                progress = current.progress.filterKeys { (_, habitId) -> habitId != id },
            )
            .withRecalculatedDays()
    }

    /**
     * Habits eines Tages: alle aktiven, dazu archivierte, fuer die an diesem Tag
     * schon etwas erfasst wurde. So verschwindet ein archivierter Habit aus neuen
     * Tagen, alte Eintraege bleiben aber sichtbar (F1).
     *
     * TODO(Room): dort ergibt sich die Zugehoerigkeit direkt aus den `DayHabit`-Zeilen.
     */
    private fun Store.entriesFor(date: LocalDate): List<HabitEntry> = habits
        .filter { !it.archived || (progress[date to it.id] ?: 0) > 0 }
        .map { habit -> HabitEntry(habit, progress[date to habit.id] ?: 0) }

    /**
     * Schreibt `Day.passed` fort. Nicht nur Erfassungen aendern das Ergebnis:
     * auch geaenderte Punkte, ein neues Ziel oder ein archivierter Habit tun es.
     */
    private fun Store.withRecalculatedDays(): Store = copy(
        days = days.mapValues { (date, day) ->
            day.copy(passed = DayEvaluator.evaluate(day, entriesFor(date)).passed)
        },
    )

    private fun defaultDay(date: LocalDate) = Day(
        date = date,
        goalType = DEFAULT_GOAL_TYPE,
        goalThreshold = DEFAULT_GOAL_THRESHOLD,
    )

    private fun seed(today: LocalDate): Store {
        val habits = listOf(
            Habit(
                id = 1,
                name = "Wasser trinken",
                type = HabitType.COUNTER,
                target = 8,
                unit = "Glaeser",
                points = 2,
                icon = "water_drop",
            ),
            Habit(2, "Sport", HabitType.CHECK, target = 1, points = 3, required = true, icon = "directions_run"),
            Habit(3, "Lesen", HabitType.AMOUNT, target = 30, unit = "min", points = 2, icon = "menu_book"),
            Habit(4, "Meditation", HabitType.CHECK, target = 1, points = 1, icon = "self_improvement"),
            Habit(5, "Tagebuch", HabitType.CHECK, target = 1, points = 1, icon = "edit_note"),
        )
        // Vier bestandene Tage vor heute, damit Streak und Verlauf etwas anzeigen.
        val pastDays = (1..4).associate { back ->
            val date = today.minusDays(back.toLong())
            date to Day(date, goalType = DEFAULT_GOAL_TYPE, goalThreshold = DEFAULT_GOAL_THRESHOLD, passed = true)
        }
        return Store(
            habits = habits,
            days = pastDays + (today to defaultDay(today)),
            progress = mapOf((today to 1L) to 3),
            nextId = habits.size + 1L,
        )
    }

    private companion object {
        val DEFAULT_GOAL_TYPE = GoalType.POINTS
        const val DEFAULT_GOAL_THRESHOLD = 6
        const val PROGRESS_MAX = 9_999
    }
}
