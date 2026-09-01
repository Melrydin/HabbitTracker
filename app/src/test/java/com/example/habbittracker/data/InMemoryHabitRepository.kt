package com.example.habbittracker.data

import com.example.habbittracker.data.HabitRepository.Companion.NEW_HABIT_ID
import com.example.habbittracker.domain.DayEvaluator
import com.example.habbittracker.domain.DayHabits
import com.example.habbittracker.domain.StreakCalculator
import com.example.habbittracker.domain.model.Day
import com.example.habbittracker.domain.model.DayStatus
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * An in-memory test double for [HabitRepository], seeded with sample habits.
 *
 * Production uses [com.example.habbittracker.data.RoomHabitRepository]; this fake
 * exists so repository behavior can be exercised without a database. Both share the
 * rules in [DayEvaluator], [DayHabits] and [StreakCalculator], so the two cannot
 * drift apart on the parts that matter.
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

    override fun observeDay(date: LocalDate): Flow<DaySnapshot> =
        store.map { current ->
            DaySnapshot(
                day = current.days[date] ?: defaultDay(date),
                entries = current.entriesFor(date),
                currentStreak =
                    StreakCalculator.currentStreak(
                        statuses = current.days.mapValues { (_, day) -> day.status },
                        today = date,
                    ),
            )
        }

    override suspend fun setProgress(date: LocalDate, habitId: Long, progress: Int) =
        writeLock.withLock {
            val current = store.value
            current.habits.firstOrNull { it.id == habitId } ?: return@withLock
            // Counts start at zero and are allowed to run past their target.
            val clamped = progress.coerceIn(0, PROGRESS_MAX)
            val updated = current.copy(progress = current.progress + ((date to habitId) to clamped))
            store.value = updated.withRecalculatedDays()
        }

    /** The fake only stores the link; creating a theme habit is the real repository's job. */
    override suspend fun setDayTheme(date: LocalDate, themeName: String?) =
        writeLock.withLock {
            val current = store.value
            val day = current.days[date] ?: defaultDay(date)
            val named = themeName?.trim()?.ifEmpty { null }
            val habitId = named?.let { name -> current.habits.firstOrNull { it.name == name }?.id }
            store.value = current.copy(days = current.days + (date to day.copy(themeHabitId = habitId)))
        }

    override suspend fun setDayNote(date: LocalDate, note: String?) =
        writeLock.withLock {
            val current = store.value
            val day = current.days[date] ?: defaultDay(date)
            val cleaned = note?.trim()?.take(Day.NOTE_MAX_LENGTH)?.ifEmpty { null }
            store.value = current.copy(days = current.days + (date to day.copy(dayNote = cleaned)))
        }

    override fun observeDayStatuses(): Flow<Map<LocalDate, DayStatus>> =
        store.map { current -> current.days.mapValues { (_, day) -> day.status } }

    override fun observeHabits(): Flow<List<Habit>> = store.map { it.habits }

    override suspend fun getHabit(id: Long): Habit? = store.value.habits.firstOrNull { it.id == id }

    override suspend fun upsertHabit(habit: Habit): Long =
        writeLock.withLock {
            val current = store.value
            if (habit.id == NEW_HABIT_ID) {
                val created = habit.copy(id = current.nextId)
                store.value =
                    current
                        .copy(habits = current.habits + created, nextId = current.nextId + 1)
                        .withRecalculatedDays()
                created.id
            } else {
                store.value =
                    current
                        .copy(habits = current.habits.map { if (it.id == habit.id) habit else it })
                        .withRecalculatedDays()
                habit.id
            }
        }

    override suspend fun setArchived(id: Long, archived: Boolean) =
        writeLock.withLock {
            val current = store.value
            store.value =
                current
                    .copy(habits = current.habits.map { if (it.id == id) it.copy(archived = archived) else it })
                    .withRecalculatedDays()
        }

    override suspend fun deleteHabit(id: Long) =
        writeLock.withLock {
            val current = store.value
            store.value =
                current
                    .copy(
                        habits = current.habits.filterNot { it.id == id },
                        progress = current.progress.filterKeys { (_, habitId) -> habitId != id },
                    ).withRecalculatedDays()
        }

    /**
     * The habits of a day: every active one, plus archived ones that already have a
     * value recorded on that day. An archived habit therefore disappears from new
     * days while older entries stay visible (F1).
     *
     */
    private fun Store.entriesFor(date: LocalDate): List<HabitEntry> =
        DayHabits.entriesFor(
            habits = habits,
            // Only the rows of this day, so a missing row stays distinguishable
            // from a recorded zero.
            progressByHabitId = progress.filterKeys { it.first == date }.mapKeys { it.key.second },
        )

    /**
     * Carries `Day.status` forward. Recorded values are not the only thing that
     * changes the outcome: edited points, a new goal or an archived habit do too.
     */
    private fun Store.withRecalculatedDays(): Store =
        copy(
            days =
                days.mapValues { (date, day) ->
                    day.copy(status = DayEvaluator.evaluate(day, entriesFor(date)).status)
                },
        )

    private fun defaultDay(date: LocalDate) =
        Day(
            date = date,
            goalType = DEFAULT_GOAL_TYPE,
            goalThreshold = DEFAULT_GOAL_THRESHOLD,
        )

    private fun seed(today: LocalDate): Store {
        val habits =
            listOf(
                Habit(
                    id = 1,
                    name = "Drink water",
                    target = 8,
                    unit = "glasses",
                    points = 2,
                    icon = "water_drop",
                ),
                Habit(2, "Exercise", target = 1, points = 3, required = true, icon = "directions_run"),
                Habit(3, "Read", target = 30, unit = "min", points = 2, icon = "menu_book"),
                Habit(4, "Meditation", target = 1, points = 1, icon = "self_improvement"),
                Habit(5, "Journal", target = 1, points = 1, icon = "edit_note"),
            )
        // Four passed days before today so the streak and history show something.
        val pastDays =
            (1..4).associate { back ->
                val date = today.minusDays(back.toLong())
                date to
                    Day(
                        date = date,
                        goalType = DEFAULT_GOAL_TYPE,
                        goalThreshold = DEFAULT_GOAL_THRESHOLD,
                        status = DayStatus.PASSED,
                    )
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
