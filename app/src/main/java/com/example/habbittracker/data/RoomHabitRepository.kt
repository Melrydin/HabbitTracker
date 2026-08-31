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
import com.example.habbittracker.domain.model.Day
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * The persistent implementation of [HabitRepository] (F6 exports from the same tables).
 *
 * Every rule lives in `domain/`: this class only reads, writes and keeps
 * `Day.passed` in step with the data.
 */
class RoomHabitRepository(
    private val database: HabitDatabase,
    private val habitDao: HabitDao,
    private val dayDao: DayDao,
    private val dayHabitDao: DayHabitDao,
    // TODO(F7): read the default goal from the settings instead of hard-coding it.
    private val defaultGoalType: GoalType = GoalType.POINTS,
    private val defaultGoalThreshold: Int = DEFAULT_GOAL_THRESHOLD,
) : HabitRepository {
    override fun observeDay(date: LocalDate): Flow<DaySnapshot> =
        combine(
            dayDao.observe(date),
            habitDao.observeAll(),
            dayHabitDao.observeForDate(date),
            dayDao.observePassedDates(),
        ) { day, habits, recorded, passedDates ->
            DaySnapshot(
                day = day?.toDomain() ?: defaultDay(date),
                entries =
                    DayHabits.entriesFor(
                        habits = habits.map { it.toDomain() },
                        progressByHabitId = recorded.associate { it.habitId to it.progress },
                    ),
                currentStreak = StreakCalculator.currentStreak(passedDates.toSet(), date),
            )
        }

    override suspend fun setProgress(date: LocalDate, habitId: Long, progress: Int) {
        database.withTransaction {
            val habit = habitDao.getById(habitId)?.toDomain() ?: return@withTransaction
            dayHabitDao.upsert(DayHabitEntity(date, habitId, habit.clamp(progress)))
            recalculate(date)
        }
    }

    override suspend fun setDayTheme(date: LocalDate, theme: String?) {
        database.withTransaction {
            val day = dayDao.get(date)?.toDomain() ?: defaultDay(date)
            val cleaned = theme?.trim()?.take(Day.THEME_MAX_LENGTH)?.ifEmpty { null }
            dayDao.upsert(day.copy(theme = cleaned).toEntity())
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
            recalculateAll()
        }
    }

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
        dayDao.upsert(day.copy(passed = DayEvaluator.evaluate(day, entries).passed).toEntity())
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
                day.copy(passed = DayEvaluator.evaluate(day, entries).passed).toEntity()
            }
        dayDao.upsertAll(updated)
    }

    private fun defaultDay(date: LocalDate) =
        Day(
            date = date,
            goalType = defaultGoalType,
            goalThreshold = defaultGoalThreshold,
        )

    private companion object {
        const val DEFAULT_GOAL_THRESHOLD = 6
        const val PROGRESS_MAX = 9_999
    }
}
