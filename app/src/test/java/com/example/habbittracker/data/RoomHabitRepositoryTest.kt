package com.example.habbittracker.data

import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.habbittracker.data.HabitRepository.Companion.NEW_HABIT_ID
import com.example.habbittracker.data.local.HabitDatabase
import com.example.habbittracker.domain.model.DayStatus
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Exercises the real Room implementation on the JVM, so the DAOs, type converters
 * and the foreign key cascade are covered by the normal unit test run rather than
 * only on a device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class RoomHabitRepositoryTest {
    private val date = LocalDate.of(2026, 8, 31)

    private lateinit var database: HabitDatabase
    private lateinit var repository: RoomHabitRepository

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HabitDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository =
            RoomHabitRepository(
                database = database,
                habitDao = database.habitDao(),
                dayDao = database.dayDao(),
                dayHabitDao = database.dayHabitDao(),
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
    fun `changed points recompute the day status`() =
        runBlocking {
            // A second habit keeps the day worth more than the threshold, so the
            // threshold is not capped and the points actually decide the outcome.
            val exercise = addHabit("Exercise", points = 3)
            addHabit("Read", points = 5)
            repository.setProgress(date, exercise, 1)
            assertFalse(
                repository
                    .observeDay(date)
                    .first()
                    .day.status == DayStatus.PASSED,
            )

            repository.upsertHabit(repository.getHabit(exercise)!!.copy(points = 6))

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
