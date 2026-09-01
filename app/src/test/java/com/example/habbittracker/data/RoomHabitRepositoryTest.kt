package com.example.habbittracker.data

import android.os.Build
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.habbittracker.data.HabitRepository.Companion.NEW_HABIT_ID
import com.example.habbittracker.data.local.DataStoreSettingsRepository
import com.example.habbittracker.data.local.HabitDatabase
import com.example.habbittracker.domain.model.DayStatus
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitKind
import com.example.habbittracker.domain.model.HabitType
import com.example.habbittracker.domain.model.Pause
import com.example.habbittracker.domain.model.Polarity
import com.example.habbittracker.domain.model.Recurrence
import com.example.habbittracker.domain.model.WeekSpan
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

/**
 * Exercises the real Room implementation on the JVM, so the DAOs, type converters
 * and the foreign key cascade are covered by the normal unit test run rather than
 * only on a device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class RoomHabitRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val date = LocalDate.of(2026, 8, 31)

    private lateinit var database: HabitDatabase
    private lateinit var settings: DataStoreSettingsRepository
    private lateinit var repository: RoomHabitRepository

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HabitDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        settings =
            DataStoreSettingsRepository(
                PreferenceDataStoreFactory.create { folder.newFile("settings.preferences_pb") },
            )
        repository =
            RoomHabitRepository(
                database = database,
                habitDao = database.habitDao(),
                dayDao = database.dayDao(),
                dayHabitDao = database.dayHabitDao(),
                pauseDao = database.pauseDao(),
                settings = settings.settings,
                // Pin "today" so the past and the running day can both be exercised.
                clock = Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC),
            )
    }

    @After
    fun tearDown() = database.close()

    private suspend fun addHabit(
        name: String,
        type: HabitType = HabitType.CHECK,
        target: Int = 1,
        points: Int = 1,
        required: Boolean = false,
    ): Long =
        repository.upsertHabit(
            Habit(NEW_HABIT_ID, name, type, target, points = points, required = required, icon = "task_alt"),
        )

    private fun avoidedHabit(name: String) =
        Habit(NEW_HABIT_ID, name, HabitType.CHECK, target = 1, icon = "task_alt", polarity = Polarity.BAD)

    private fun weekHabit(name: String, monday: LocalDate) =
        Habit(
            NEW_HABIT_ID,
            name,
            HabitType.CHECK,
            target = 1,
            icon = "task_alt",
            kind = HabitKind.WEEKLY,
            weekStart = monday,
            weekSpan = WeekSpan.FULL,
            recurrence = Recurrence.EVERY_DAY,
        )

    private fun themeGiver(name: String) =
        Habit(NEW_HABIT_ID, name, HabitType.CHECK, target = 1, icon = "task_alt", givesTheme = true)

    private suspend fun habitIdsOn(day: LocalDate) =
        repository
            .observeDay(day)
            .first()
            .entries
            .map { it.habit.id }

    @Test
    fun `a stored habit survives a new repository instance`() =
        runBlocking {
            val id = addHabit("Exercise")

            val reopened =
                RoomHabitRepository(
                    database = database,
                    habitDao = database.habitDao(),
                    dayDao = database.dayDao(),
                    dayHabitDao = database.dayHabitDao(),
                    pauseDao = database.pauseDao(),
                )

            assertEquals("Exercise", reopened.getHabit(id)?.name)
        }

    @Test
    fun `a new habit receives a generated id`() =
        runBlocking {
            val first = addHabit("Exercise")
            val second = addHabit("Read")

            assertTrue(first != NEW_HABIT_ID)
            assertTrue(second != first)
            assertEquals(2, repository.observeHabits().first().size)
        }

    @Test
    fun `editing replaces the habit instead of creating a second one`() =
        runBlocking {
            val id = addHabit("Exercise")

            repository.upsertHabit(repository.getHabit(id)!!.copy(name = "Jogging", points = 5))

            val habits = repository.observeHabits().first()
            assertEquals(1, habits.size)
            assertEquals("Jogging", habits.single().name)
        }

    @Test
    fun `enum and date columns round trip`() =
        runBlocking {
            val id =
                repository.upsertHabit(
                    Habit(NEW_HABIT_ID, "Read", HabitType.COUNTER, target = 30, unit = "min", icon = "menu_book"),
                )
            repository.setProgress(date, id, 30)

            val stored = repository.getHabit(id)!!
            assertEquals(HabitType.COUNTER, stored.type)
            assertEquals("min", stored.unit)
            assertEquals(
                date,
                repository
                    .observeDay(date)
                    .first()
                    .day.date,
            )
        }

    @Test
    fun `a check habit cannot be pushed beyond one`() =
        runBlocking {
            val id = addHabit("Exercise")

            repository.setProgress(date, id, 7)

            assertEquals(
                1,
                repository
                    .observeDay(date)
                    .first()
                    .entries
                    .single()
                    .progress,
            )
        }

    @Test
    fun `a counter may exceed its target`() =
        runBlocking {
            val id =
                repository.upsertHabit(
                    Habit(
                        NEW_HABIT_ID,
                        "Drink water",
                        HabitType.COUNTER,
                        target = 8,
                        unit = "glasses",
                        icon = "water_drop",
                    ),
                )

            repository.setProgress(date, id, 12)

            assertEquals(
                12,
                repository
                    .observeDay(date)
                    .first()
                    .entries
                    .single()
                    .progress,
            )
        }

    @Test
    fun `the day passes once the points reach the threshold`() =
        runBlocking {
            val id = addHabit("Exercise", points = 6)
            assertFalse(
                repository
                    .observeDay(date)
                    .first()
                    .day.status == DayStatus.PASSED,
            )

            repository.setProgress(date, id, 1)

            assertTrue(
                repository
                    .observeDay(date)
                    .first()
                    .day.status == DayStatus.PASSED,
            )
        }

    @Test
    fun `a change to the habits recomputes the day status`() =
        runBlocking {
            val exercise = addHabit("Exercise", points = 3)
            val read = addHabit("Read", points = 5)
            repository.setProgress(date, exercise, 1)
            // Three of eight points, so the day is still open.
            assertFalse(
                repository
                    .observeDay(date)
                    .first()
                    .day.status == DayStatus.PASSED,
            )

            // Archiving the untouched habit takes its points out of the goal.
            repository.setArchived(read, archived = true)

            assertTrue(
                repository
                    .observeDay(date)
                    .first()
                    .day.status == DayStatus.PASSED,
            )
        }

    @Test
    fun `a single small habit can still finish the day`() =
        runBlocking {
            // The default threshold is six points; one habit worth one point must
            // not make the day impossible to pass.
            val id = addHabit("Exercise", points = 1)

            repository.setProgress(date, id, 1)

            assertTrue(
                repository
                    .observeDay(date)
                    .first()
                    .day.status == DayStatus.PASSED,
            )
        }

    @Test
    fun `an archived habit without a record disappears from the day`() =
        runBlocking {
            val id = addHabit("Exercise")
            assertTrue(habitIdsOn(date).contains(id))

            repository.setArchived(id, archived = true)

            assertFalse(habitIdsOn(date).contains(id))
        }

    @Test
    fun `an archived habit with a record stays visible on the day`() =
        runBlocking {
            val id = addHabit("Exercise")
            repository.setProgress(date, id, 1)

            repository.setArchived(id, archived = true)

            assertTrue(habitIdsOn(date).contains(id))
        }

    @Test
    fun `deleting a habit cascades to its recorded values`() =
        runBlocking {
            val id = addHabit("Exercise")
            repository.setProgress(date, id, 1)

            repository.deleteHabit(id)

            assertNull(repository.getHabit(id))
            assertTrue(database.dayHabitDao().getAll().isEmpty())
        }

    @Test
    fun `a theme habit belongs to its own day only`() =
        runBlocking {
            repository.setDayTheme(date, "Deep work")

            assertEquals(1, habitIdsOn(date).size)
            // The generated habit is day-local: another day must not inherit it.
            assertTrue(habitIdsOn(date.minusDays(1)).isEmpty())
            assertTrue(habitIdsOn(date.plusDays(1)).isEmpty())
        }

    @Test
    fun `a theme habit counts towards its own day`() =
        runBlocking {
            repository.setDayTheme(date, "Deep work")
            val themeHabit = habitIdsOn(date).single()

            repository.setProgress(date, themeHabit, 1)

            val snapshot = repository.observeDay(date).first()
            assertEquals("Deep work", snapshot.themeName)
            assertEquals(DayStatus.PASSED, snapshot.day.status)
        }

    @Test
    fun `a normal habit still shows on every day`() =
        runBlocking {
            val id = addHabit("Exercise")

            assertTrue(habitIdsOn(date.minusDays(3)).contains(id))
            assertTrue(habitIdsOn(date).contains(id))
        }

    @Test
    fun `switching the default goal takes effect on today`() =
        runBlocking {
            val id = addHabit("Exercise", points = 1)
            // The day gets a stored row the moment anything is tracked on it.
            repository.setProgress(date, id, 1)
            settings.setDefaultGoal(GoalType.MIN_COUNT, threshold = 1)

            val day = repository.observeDay(date).first().day

            assertEquals(GoalType.MIN_COUNT, day.goalType)
        }

    @Test
    fun `switching the default goal leaves finished days alone`() =
        runBlocking {
            val yesterday = date.minusDays(1)
            val id = addHabit("Exercise", points = 1)
            repository.setProgress(yesterday, id, 1)

            settings.setDefaultGoal(GoalType.MIN_COUNT, threshold = 1)

            // History must not move under the user.
            assertEquals(
                GoalType.POINTS,
                repository
                    .observeDay(yesterday)
                    .first()
                    .day.goalType,
            )
        }

    @Test
    fun `a day with its own goal ignores the default`() =
        runBlocking {
            addHabit("Exercise", points = 1)
            repository.setDayGoal(date, GoalType.MIN_COUNT, threshold = 1)

            settings.setDefaultGoal(GoalType.ALL_REQUIRED, threshold = 9)

            val day = repository.observeDay(date).first().day
            assertEquals(GoalType.MIN_COUNT, day.goalType)
            assertTrue(day.goalOverridden)
        }

    @Test
    fun `an own goal decides the day status`() =
        runBlocking {
            val exercise = addHabit("Exercise", points = 3)
            addHabit("Read", points = 4)
            repository.setProgress(date, exercise, 1)
            // Three of the five points asked for.
            repository.setDayGoal(date, GoalType.POINTS, threshold = 5)
            assertFalse(
                repository
                    .observeDay(date)
                    .first()
                    .day.status == DayStatus.PASSED,
            )

            repository.setDayGoal(date, GoalType.POINTS, threshold = 3)

            assertTrue(
                repository
                    .observeDay(date)
                    .first()
                    .day.status == DayStatus.PASSED,
            )
        }

    @Test
    fun `dropping the override hands the day back to the default`() =
        runBlocking {
            addHabit("Exercise", points = 1)
            repository.setDayGoal(date, GoalType.MIN_COUNT, threshold = 1)

            repository.clearDayGoal(date)

            val day = repository.observeDay(date).first().day
            assertFalse(day.goalOverridden)
            assertEquals(GoalType.POINTS, day.goalType)
        }

    @Test
    fun `an untouched day is already clean for a habit that is avoided`() =
        runBlocking {
            val id = repository.upsertHabit(avoidedHabit("Smoking"))

            val entry =
                repository
                    .observeDay(date)
                    .first()
                    .entries
                    .first { it.habit.id == id }
            assertTrue(entry.fulfilled)
            // Nothing is open there: no tap can make a clean day cleaner.
            assertFalse(entry.open)
        }

    @Test
    fun `a recorded slip takes the day away from a habit that is avoided`() =
        runBlocking {
            val id = repository.upsertHabit(avoidedHabit("Smoking"))

            repository.setProgress(date, id, 1)

            assertFalse(
                repository
                    .observeDay(date)
                    .first()
                    .entries
                    .first { it.habit.id == id }
                    .fulfilled,
            )
        }

    @Test
    fun `completing a habit that is avoided clears the slip instead of booking one`() =
        runBlocking {
            val id = repository.upsertHabit(avoidedHabit("Smoking"))
            repository.setProgress(date, id, 1)

            repository.completeHabit(date, id)

            assertTrue(
                repository
                    .observeDay(date)
                    .first()
                    .entries
                    .first { it.habit.id == id }
                    .fulfilled,
            )
        }

    @Test
    fun `the history of a habit that is avoided marks the days with a slip`() =
        runBlocking {
            val id = repository.upsertHabit(avoidedHabit("Smoking"))
            repository.setProgress(date.minusDays(1), id, 1)
            repository.setProgress(date, id, 0)

            val history = repository.observeHabitHistory(id).first()
            assertEquals(false, history[date.minusDays(1)])
            assertEquals(true, history[date])
        }

    @Test
    fun `a week habit only shows up inside its own week`() =
        runBlocking {
            val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val id = repository.upsertHabit(weekHabit("Reading week", monday))

            assertTrue(habitIdsOn(monday).contains(id))
            assertTrue(habitIdsOn(monday.plusDays(6)).contains(id))
            assertFalse(habitIdsOn(monday.plusWeeks(1)).contains(id))
        }

    @Test
    fun `deleting a week habit takes its sub habits with it`() =
        runBlocking {
            val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val parent = repository.upsertHabit(weekHabit("Reading week", monday))
            val sub =
                repository.upsertHabit(
                    Habit(
                        NEW_HABIT_ID,
                        "Chapter",
                        HabitType.CHECK,
                        target = 1,
                        icon = "task_alt",
                        kind = HabitKind.SUB,
                        parentId = parent,
                        assignedDows = setOf(1),
                    ),
                )

            repository.deleteHabit(parent)

            assertNull(repository.getHabit(sub))
        }

    @Test
    fun `a habit that offers a theme gives it to the day`() =
        runBlocking {
            val id = repository.upsertHabit(themeGiver("Reading week"))

            assertEquals("Reading week", repository.observeDay(date).first().themeName)
        }

    @Test
    fun `two offers wait for the user to pick one`() =
        runBlocking {
            repository.upsertHabit(themeGiver("Reading week"))
            val cooking = repository.upsertHabit(themeGiver("Cooking"))

            val open = repository.observeDay(date).first()
            assertNull(open.themeName)
            assertEquals(2, open.themeChoice.size)

            repository.chooseDayTheme(date, cooking)

            assertEquals("Cooking", repository.observeDay(date).first().themeName)
        }

    @Test
    fun `picking an offered theme drops the one that was typed`() =
        runBlocking {
            val id = repository.upsertHabit(themeGiver("Reading week"))
            repository.setDayTheme(date, "Spring cleaning")

            repository.chooseDayTheme(date, id)

            val day = repository.observeDay(date).first()
            assertEquals("Reading week", day.themeName)
            // The habit generated from the typed theme was never tracked on, so it goes.
            assertTrue(day.entries.none { it.habit.name == "Spring cleaning" })
        }

    @Test
    fun `a step from the widget counts up by one instead of finishing the habit`() =
        runBlocking {
            val id = addHabit("Water", type = HabitType.COUNTER, target = 5)

            repository.incrementHabit(date, id)
            repository.incrementHabit(date, id)

            val entry =
                repository
                    .observeDay(date)
                    .first()
                    .entries
                    .first { it.habit.id == id }
            assertEquals(2, entry.progress)
            assertFalse(entry.fulfilled)
        }

    @Test
    fun `a step ticks a check habit off`() =
        runBlocking {
            val id = addHabit("Exercise")

            repository.incrementHabit(date, id)

            assertTrue(
                repository
                    .observeDay(date)
                    .first()
                    .entries
                    .first { it.habit.id == id }
                    .fulfilled,
            )
        }

    @Test
    fun `a missed day spends a grace day and the run survives it`() =
        runBlocking {
            val id = addHabit("Exercise", points = 6)
            repository.setProgress(date.minusDays(3), id, 1)
            // A stored day that earns nothing is a missed day.
            repository.setProgress(date.minusDays(2), id, 0)
            repository.setProgress(date.minusDays(1), id, 1)

            val frozen = repository.observeFrozenDays().first()
            assertEquals(setOf(date.minusDays(2)), frozen)
            // Both passed days still count: the missed one in between was forgiven.
            assertEquals(2, repository.observeDay(date).first().currentStreak)
        }

    @Test
    fun `the second missed day of a month is on its own`() =
        runBlocking {
            val id = addHabit("Exercise", points = 6)
            repository.setProgress(date.minusDays(6), id, 1)
            repository.setProgress(date.minusDays(5), id, 0)
            repository.setProgress(date.minusDays(4), id, 1)
            repository.setProgress(date.minusDays(3), id, 0)

            // One grace day a month, and the earliest missed day takes it.
            assertEquals(setOf(date.minusDays(5)), repository.observeFrozenDays().first())
            assertEquals(0, repository.observeDay(date).first().currentStreak)
        }

    @Test
    fun `switching the protection off judges the past days again`() =
        runBlocking {
            val id = addHabit("Exercise", points = 6)
            repository.setProgress(date.minusDays(2), id, 1)
            repository.setProgress(date.minusDays(1), id, 0)

            settings.setFreezePerMonth(0)
            repository.refreshDays()

            assertTrue(repository.observeFrozenDays().first().isEmpty())
            assertEquals(0, repository.observeDay(date).first().currentStreak)
        }

    @Test
    fun `the habit history says which days it applied to and how they went`() =
        runBlocking {
            val id = addHabit("Exercise")
            repository.setProgress(date.minusDays(1), id, 1)
            // Touching the day creates its row without fulfilling the habit.
            repository.setDayNote(date, "note")

            val history = repository.observeHabitHistory(id).first()

            assertEquals(true, history[date.minusDays(1)])
            assertEquals(false, history[date])
        }

    @Test
    fun `the history of an unknown habit is empty`() =
        runBlocking {
            assertTrue(repository.observeHabitHistory(999).first().isEmpty())
        }

    @Test
    fun `a global pause makes the day neutral however it was tracked`() =
        runBlocking {
            val id = addHabit("Exercise", points = 1)
            repository.setProgress(date, id, 1)
            assertTrue(
                repository
                    .observeDay(date)
                    .first()
                    .day.status == DayStatus.PASSED,
            )

            repository.upsertPause(Pause(id = 0, from = date, to = date))

            assertEquals(
                DayStatus.NEUTRAL,
                repository
                    .observeDay(date)
                    .first()
                    .day.status,
            )
        }

    @Test
    fun `a paused habit leaves the goal alone`() =
        runBlocking {
            val exercise = addHabit("Exercise", points = 3)
            val read = addHabit("Read", points = 5)
            repository.setProgress(date, exercise, 1)
            // Three of the eight points the day holds: still open.
            assertFalse(
                repository
                    .observeDay(date)
                    .first()
                    .day.status == DayStatus.PASSED,
            )

            repository.upsertPause(Pause(id = 0, from = date, to = date, habitId = read))

            val snapshot = repository.observeDay(date).first()
            assertEquals(DayStatus.PASSED, snapshot.day.status)
            assertTrue(snapshot.entries.none { it.habit.id == read })
        }

    @Test
    fun `removing a pause judges the days again`() =
        runBlocking {
            val id = addHabit("Exercise", points = 1)
            repository.setProgress(date, id, 1)
            val pauseId = repository.upsertPause(Pause(id = 0, from = date, to = date))
            assertEquals(
                DayStatus.NEUTRAL,
                repository
                    .observeDay(date)
                    .first()
                    .day.status,
            )

            repository.deletePause(pauseId)

            assertEquals(
                DayStatus.PASSED,
                repository
                    .observeDay(date)
                    .first()
                    .day.status,
            )
        }

    @Test
    fun `the theme is stored and an empty theme clears it`() =
        runBlocking {
            repository.setDayTheme(date, "  Calm focus  ")
            assertEquals(
                "Calm focus",
                repository
                    .observeDay(date)
                    .first()
                    .themeName,
            )

            repository.setDayTheme(date, "   ")

            assertNull(
                repository
                    .observeDay(date)
                    .first()
                    .themeName,
            )
        }

    @Test
    fun `the streak counts consecutive passed days up to today`() =
        runBlocking {
            val id = addHabit("Exercise", points = 6)
            (1..3).forEach { back -> repository.setProgress(date.minusDays(back.toLong()), id, 1) }

            assertEquals(3, repository.observeDay(date).first().currentStreak)
        }
}
