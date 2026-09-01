package com.example.habbittracker.data

import androidx.room.withTransaction
import com.example.habbittracker.data.local.DayDao
import com.example.habbittracker.data.local.DayHabitDao
import com.example.habbittracker.data.local.DayHabitEntity
import com.example.habbittracker.data.local.HabitDao
import com.example.habbittracker.data.local.HabitDatabase
import com.example.habbittracker.data.local.toDomain
import com.example.habbittracker.data.local.toEntity
import com.example.habbittracker.domain.DayEvaluator
import com.example.habbittracker.domain.DayHabits
import com.example.habbittracker.domain.StreakCalculator
import com.example.habbittracker.domain.model.AppSettings
import com.example.habbittracker.domain.model.Day
import com.example.habbittracker.domain.model.DayStatus
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * The persistent implementation of [HabitRepository] (F6 exports from the same tables).
 *
 * Every rule lives in `domain/`: this class only reads, writes and keeps
 * `Day.status` in step with the data.
 */
class RoomHabitRepository(
    private val database: HabitDatabase,
    private val habitDao: HabitDao,
    private val dayDao: DayDao,
    private val dayHabitDao: DayHabitDao,
    private val settings: Flow<AppSettings> = flowOf(AppSettings()),
) : HabitRepository {
    override fun observeDay(date: LocalDate): Flow<DaySnapshot> =
        combine(
            dayDao.observe(date),
            habitDao.observeAll(),
            dayHabitDao.observeForDate(date),
            dayDao.observeStatuses(),
            settings,
        ) { day, habits, recorded, statuses, current ->
            DaySnapshot(
                day = day?.toDomain() ?: defaultDay(date, current),
                entries =
                    DayHabits.entriesFor(
                        habits = habits.map { it.toDomain() },
                        progressByHabitId = recorded.associate { it.habitId to it.progress },
                    ),
                currentStreak =
                    StreakCalculator.currentStreak(
                        statuses = statuses.associate { it.date to it.status },
                        today = date,
                    ),
            )
        }

    override fun observeDayStatuses(): Flow<Map<LocalDate, DayStatus>> =
        dayDao.observeStatuses().map { rows -> rows.associate { it.date to it.status } }

    override suspend fun setProgress(date: LocalDate, habitId: Long, progress: Int) {
        database.withTransaction {
            val habit = habitDao.getById(habitId)?.toDomain() ?: return@withTransaction
            dayHabitDao.upsert(DayHabitEntity(date, habitId, habit.clamp(progress)))
            recalculate(date)
        }
    }

    /**
     * A manually set theme creates a habit and links it to the day (F2). The habit
     * counts towards the daily goal like any other, which is the whole point of
     * tying theme and habit together.
     */
    override suspend fun setDayTheme(date: LocalDate, themeName: String?) {
        database.withTransaction {
            val day = dayDao.get(date)?.toDomain() ?: defaultDay(date)
            val cleaned = themeName?.trim()?.take(Habit.NAME_MAX_LENGTH)?.ifEmpty { null }
            val themeHabitId =
                when {
                    cleaned == null -> null.also { discardGeneratedTheme(day, date) }
                    else -> renameOrCreateThemeHabit(day, date, cleaned)
                }
            dayDao.upsert(day.copy(themeHabitId = themeHabitId).toEntity())
            recalculate(date)
        }
    }

    override suspend fun setDayNote(date: LocalDate, note: String?) {
        database.withTransaction {
            val day = dayDao.get(date)?.toDomain() ?: defaultDay(date)
            val cleaned = note?.trim()?.take(Day.NOTE_MAX_LENGTH)?.ifEmpty { null }
            dayDao.upsert(day.copy(dayNote = cleaned).toEntity())
        }
    }

    override fun observeHabits(): Flow<List<Habit>> =
        habitDao.observeAll().map { habits ->
            habits.map { it.toDomain() }
        }

    override suspend fun getHabit(id: Long): Habit? = habitDao.getById(id)?.toDomain()

    override suspend fun upsertHabit(habit: Habit): Long =
        database.withTransaction {
            val id = habitDao.upsert(habit.toEntity())
            // Edited points or a changed target can flip days that were already recorded.
            recalculateAll()
            if (habit.id == HabitRepository.NEW_HABIT_ID) id else habit.id
        }

    override suspend fun setArchived(id: Long, archived: Boolean) {
        database.withTransaction {
            habitDao.setArchived(id, archived)
            recalculateAll()
        }
    }

    override suspend fun deleteHabit(id: Long) {
        database.withTransaction {
            // The foreign key cascades, so the recorded values go with the habit.
            habitDao.getById(id)?.let { habitDao.delete(it) }
            dayDao.clearThemeHabit(id)
            recalculateAll()
        }
    }

    /** Renames the day's generated theme habit, or creates one if there is none. */
    private suspend fun renameOrCreateThemeHabit(day: Day, date: LocalDate, name: String): Long {
        val current = day.themeHabitId?.let { habitDao.getById(it)?.toDomain() }
        if (current != null && current.isThemeGenerated) {
            habitDao.upsert(current.copy(name = name).toEntity())
            return current.id
        }
        val created = habitDao.upsert(themeHabit(name).toEntity())
        dayHabitDao.upsert(DayHabitEntity(date, created, 0))
        return created
    }

    /**
     * Clearing the theme also removes the habit it generated, but only while nothing
     * was recorded on it. Once it carries progress it is a habit like any other.
     */
    private suspend fun discardGeneratedTheme(day: Day, date: LocalDate) {
        val current = day.themeHabitId?.let { habitDao.getById(it) } ?: return
        val untouched = (dayHabitDao.get(date, current.id)?.progress ?: 0) == 0
        if (current.isThemeGenerated && untouched) habitDao.delete(current)
    }

    private fun themeHabit(name: String) =
        Habit(
            id = HabitRepository.NEW_HABIT_ID,
            name = name,
            type = HabitType.CHECK,
            target = 1,
            icon = Habit.DEFAULT_ICON,
            givesTheme = true,
            isThemeGenerated = true,
        )

    /** CHECK only knows 0 or 1, counters are allowed to exceed their target. */
    private fun Habit.clamp(progress: Int): Int =
        when (type) {
            HabitType.CHECK -> progress.coerceIn(0, 1)
            HabitType.COUNTER, HabitType.AMOUNT -> progress.coerceIn(0, PROGRESS_MAX)
        }

    private suspend fun recalculate(date: LocalDate) {
        val day = dayDao.get(date)?.toDomain() ?: defaultDay(date)
        val entries =
            DayHabits.entriesFor(
                habits = habitDao.getAll().map { it.toDomain() },
                progressByHabitId = dayHabitDao.getForDate(date).associate { it.habitId to it.progress },
            )
        dayDao.upsert(day.copy(status = DayEvaluator.evaluate(day, entries).status).toEntity())
    }

    /**
     * Recomputes every stored day at once. Used when a change to a habit can affect
     * days other than today, which a single-date pass would silently leave stale.
     */
    private suspend fun recalculateAll() {
        val habits = habitDao.getAll().map { it.toDomain() }
        val recordedByDate = dayHabitDao.getAll().groupBy { it.date }
        val updated =
            dayDao.getAll().map { entity ->
                val day = entity.toDomain()
                val entries =
                    DayHabits.entriesFor(
                        habits = habits,
                        progressByHabitId = recordedByDate[day.date].orEmpty().associate { it.habitId to it.progress },
                    )
                day.copy(status = DayEvaluator.evaluate(day, entries).status).toEntity()
            }
        dayDao.upsertAll(updated)
    }

    /** New days start from the settings; days that already exist keep their own goal. */
    private suspend fun defaultDay(date: LocalDate) = defaultDay(date, settings.first())

    private fun defaultDay(date: LocalDate, current: AppSettings) =
        Day(
            date = date,
            goalType = current.defaultGoalType,
            goalThreshold = current.defaultGoalThreshold,
        )

    private companion object {
        const val PROGRESS_MAX = 9_999
    }
}
