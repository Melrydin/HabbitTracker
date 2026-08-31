package com.example.habbittracker.data

import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.habbittracker.data.HabitRepository.Companion.NEW_HABIT_ID
import com.example.habbittracker.data.local.HabitDatabase
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
                    Habit(NEW_HABIT_ID, "Read", HabitType.AMOUNT, target = 30, unit = "min", icon = "menu_book"),
                )
            repository.setProgress(date, id, 30)

            val stored = repository.getHabit(id)!!
            assertEquals(HabitType.AMOUNT, stored.type)
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
                    .day.passed,
            )

            repository.setProgress(date, id, 1)

            assertTrue(
                repository
                    .observeDay(date)
                    .first()
                    .day.passed,
            )
        }

    @Test
    fun `changed points recompute the day status`() =
        runBlocking {
            val id = addHabit("Exercise", points = 3)
            repository.setProgress(date, id, 1)
            assertFalse(
                repository
                    .observeDay(date)
                    .first()
                    .day.passed,
            )

            repository.upsertHabit(repository.getHabit(id)!!.copy(points = 6))

            assertTrue(
                repository
                    .observeDay(date)
                    .first()
                    .day.passed,
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
    fun `the theme is stored and an empty theme clears it`() =
        runBlocking {
            repository.setDayTheme(date, "  Calm focus  ")
            assertEquals(
                "Calm focus",
                repository
                    .observeDay(date)
                    .first()
                    .day.theme,
            )

            repository.setDayTheme(date, "   ")

            assertNull(
                repository
                    .observeDay(date)
                    .first()
                    .day.theme,
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
