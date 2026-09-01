package com.example.habbittracker.data

import com.example.habbittracker.data.HabitRepository.Companion.NEW_HABIT_ID
import com.example.habbittracker.domain.DayEvaluator
import com.example.habbittracker.domain.DayHabits
import com.example.habbittracker.domain.Pauses
import com.example.habbittracker.domain.StreakCalculator
import com.example.habbittracker.domain.StreakProtection
import com.example.habbittracker.domain.model.AppSettings
import com.example.habbittracker.domain.model.Day
import com.example.habbittracker.domain.model.DayStatus
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitEntry
import com.example.habbittracker.domain.model.HabitType
import com.example.habbittracker.domain.model.Pause
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
    private val freezePerMonth: Int = AppSettings.DEFAULT_FREEZE_PER_MONTH,
) : HabitRepository {
    private data class Store(
        val habits: List<Habit>,
        val days: Map<LocalDate, Day>,
        val progress: Map<Pair<LocalDate, Long>, Int>,
        val nextId: Long,
        val pauses: List<Pause> = emptyList(),
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
                        frozen = current.frozenDays(),
                    ),
            )
        }

    override suspend fun setProgress(date: LocalDate, habitId: Long, progress: Int) =
        writeLock.withLock {
            val current = store.value
            val habit = current.habits.firstOrNull { it.id == habitId } ?: return@withLock
            val clamped =
                when (habit.type) {
                    // CHECK only knows 0 or 1, counters are allowed to exceed their target.
                    HabitType.CHECK -> progress.coerceIn(0, 1)

                    HabitType.COUNTER -> progress.coerceIn(0, PROGRESS_MAX)
                }
            val updated = current.copy(progress = current.progress + ((date to habitId) to clamped))
            store.value = updated.withRecalculatedDays()
        }

    override suspend fun completeHabit(date: LocalDate, habitId: Long) {
        val habit = store.value.habits.firstOrNull { it.id == habitId } ?: return
        setProgress(date, habitId, habit.target)
    }

    /** The fake only stores the link; creating a theme habit is the real repository's job. */
    override suspend fun incrementHabit(date: LocalDate, habitId: Long) {
        setProgress(date, habitId, (store.value.progress[date to habitId] ?: 0) + 1)
    }

    override suspend fun setDayTheme(date: LocalDate, themeName: String?) =
        writeLock.withLock {
            val current = store.value
            val day = current.days[date] ?: defaultDay(date)
            val named = themeName?.trim()?.ifEmpty { null }
            val habitId = named?.let { name -> current.habits.firstOrNull { it.name == name }?.id }
            store.value = current.copy(days = current.days + (date to day.copy(themeHabitId = habitId)))
        }

    override suspend fun chooseDayTheme(date: LocalDate, habitId: Long) =
        writeLock.withLock {
            val current = store.value
            val day = current.days[date] ?: defaultDay(date)
            store.value = current.copy(days = current.days + (date to day.copy(themeHabitId = habitId)))
        }

    override suspend fun setDayGoal(date: LocalDate, goalType: GoalType, threshold: Int) =
        writeLock.withLock {
            val current = store.value
            val day = current.days[date] ?: defaultDay(date)
            val own = day.copy(goalType = goalType, goalThreshold = threshold, goalOverridden = true)
            store.value = current.copy(days = current.days + (date to own)).withRecalculatedDays()
        }

    override suspend fun clearDayGoal(date: LocalDate) =
        writeLock.withLock {
            val current = store.value
            val day = current.days[date] ?: return@withLock
            val followed =
                day.copy(
                    goalType = DEFAULT_GOAL_TYPE,
                    goalThreshold = DEFAULT_GOAL_THRESHOLD,
                    goalOverridden = false,
                )
            store.value = current.copy(days = current.days + (date to followed)).withRecalculatedDays()
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

    override fun observeFrozenDays(): Flow<Set<LocalDate>> = store.map { it.frozenDays() }

    override suspend fun refreshDays() =
        writeLock.withLock {
            store.value = store.value.withRecalculatedDays()
        }

    override fun observeHabitHistory(habitId: Long): Flow<Map<LocalDate, Boolean>> =
        store.map { current ->
            val habit = current.habits.firstOrNull { it.id == habitId }
            if (habit == null) {
                emptyMap()
            } else {
                current.days.keys
                    .filter { date -> current.entriesFor(date).any { it.habit.id == habitId } }
                    .associateWith { date -> (current.progress[date to habitId] ?: 0) >= habit.target }
            }
        }

    override fun observePauses(): Flow<List<Pause>> = store.map { it.pauses }

    override suspend fun upsertPause(pause: Pause): Long =
        writeLock.withLock {
            val current = store.value
            val id = if (pause.id == 0L) current.nextId else pause.id
            val stored = pause.copy(id = id)
            store.value =
                current
                    .copy(
                        pauses = current.pauses.filterNot { it.id == id } + stored,
                        nextId = maxOf(current.nextId, id + 1),
                    ).withRecalculatedDays()
            id
        }

    override suspend fun deletePause(id: Long) =
        writeLock.withLock {
            val current = store.value
            store.value =
                current.copy(pauses = current.pauses.filterNot { it.id == id }).withRecalculatedDays()
        }

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
            date = date,
            pausedHabits = Pauses.pausedHabits(pauses, date),
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
                    day.copy(
                        status =
                            DayEvaluator
                                .evaluate(day, entriesFor(date), Pauses.isPaused(pauses, date))
                                .status,
                    )
                },
        ).withGraceDays()

    /** Hands the monthly budget to the earliest missed days, as the Room repository does (F4). */
    private fun Store.withGraceDays(): Store {
        val statuses = days.mapValues { (_, day) -> day.status }
        val frozen = mutableSetOf<LocalDate>()
        val judged =
            days.keys.sorted().associateWith { date ->
                val day = days.getValue(date)
                val spends = StreakProtection.shouldFreeze(date, day.status, statuses, frozen, freezePerMonth)
                if (spends) frozen += date
                day.copy(freezeUsed = spends)
            }
        return copy(days = judged)
    }

    private fun Store.frozenDays(): Set<LocalDate> = days.filterValues { it.freezeUsed }.keys

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
                    type = HabitType.COUNTER,
                    target = 8,
                    unit = "glasses",
                    points = 2,
                    icon = "water_drop",
                ),
                Habit(2, "Exercise", HabitType.CHECK, target = 1, points = 3, required = true, icon = "directions_run"),
                Habit(3, "Read", HabitType.COUNTER, target = 30, unit = "min", points = 2, icon = "menu_book"),
                Habit(4, "Meditation", HabitType.CHECK, target = 1, points = 1, icon = "self_improvement"),
                Habit(5, "Journal", HabitType.CHECK, target = 1, points = 1, icon = "edit_note"),
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
