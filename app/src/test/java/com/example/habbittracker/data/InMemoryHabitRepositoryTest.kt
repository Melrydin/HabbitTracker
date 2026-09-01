package com.example.habbittracker.data

import com.example.habbittracker.data.HabitRepository.Companion.NEW_HABIT_ID
import com.example.habbittracker.domain.model.DayStatus
import com.example.habbittracker.domain.model.Habit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InMemoryHabitRepositoryTest {
    private val today = LocalDate.of(2026, 8, 31)

    private fun repository() = InMemoryHabitRepository(today = today)

    private suspend fun InMemoryHabitRepository.habitIdsToday() =
        observeDay(today).first().entries.map { it.habit.id }

    @Test
    fun `a new habit receives an id and appears in the list`() =
        runBlocking {
            val repository = repository()
            val newHabit = Habit(NEW_HABIT_ID, "Walk", target = 1, icon = "directions_run")

            val id = repository.upsertHabit(newHabit)

            assertTrue(id != NEW_HABIT_ID)
            assertEquals("Walk", repository.getHabit(id)?.name)
            assertTrue(repository.observeHabits().first().any { it.id == id })
        }

    @Test
    fun `editing replaces the habit instead of creating a second one`() =
        runBlocking {
            val repository = repository()
            val before = repository.observeHabits().first().size

            val exercise = repository.getHabit(2)!!
            repository.upsertHabit(exercise.copy(name = "Jogging", points = 5))

            val habits = repository.observeHabits().first()
            assertEquals(before, habits.size)
            assertEquals("Jogging", habits.first { it.id == 2L }.name)
        }

    @Test
    fun `an archived habit without a record disappears from the day`() =
        runBlocking {
            val repository = repository()
            assertTrue(repository.habitIdsToday().contains(4L))

            repository.setArchived(4, archived = true)

            assertFalse(repository.habitIdsToday().contains(4L))
        }

    @Test
    fun `an archived habit with a record stays visible on the day`() =
        runBlocking {
            val repository = repository()
            repository.setProgress(today, habitId = 4, progress = 1)

            repository.setArchived(4, archived = true)

            assertTrue(repository.habitIdsToday().contains(4L))
        }

    @Test
    fun `restoring brings the habit back`() =
        runBlocking {
            val repository = repository()
            repository.setArchived(4, archived = true)

            repository.setArchived(4, archived = false)

            assertTrue(repository.habitIdsToday().contains(4L))
        }

    @Test
    fun `changed points recompute the day status`() =
        runBlocking {
            val repository = repository()
            // Exercise contributes 3 of the 6 required points, so the day is still open.
            repository.setProgress(today, habitId = 2, progress = 1)
            assertFalse(
                repository
                    .observeDay(today)
                    .first()
                    .day.status == DayStatus.PASSED,
            )

            val exercise = repository.getHabit(2)!!
            repository.upsertHabit(exercise.copy(points = 6))

            assertTrue(
                repository
                    .observeDay(today)
                    .first()
                    .day.status == DayStatus.PASSED,
            )
        }

    @Test
    fun `archiving does not take the result away from a passed day`() =
        runBlocking {
            val repository = repository()
            repository.setProgress(today, habitId = 2, progress = 1)
            repository.setProgress(today, habitId = 3, progress = 30)
            repository.setProgress(today, habitId = 4, progress = 1)
            assertTrue(
                repository
                    .observeDay(today)
                    .first()
                    .day.status == DayStatus.PASSED,
            )

            // Exercise already counted today, so the day stays passed (F1: old entries remain).
            repository.setArchived(2, archived = true)

            assertTrue(
                repository
                    .observeDay(today)
                    .first()
                    .day.status == DayStatus.PASSED,
            )
            assertTrue(repository.habitIdsToday().contains(2L))
        }

    @Test
    fun `deleting removes the habit together with its recorded values`() =
        runBlocking {
            val repository = repository()
            repository.setProgress(today, habitId = 1, progress = 5)
            assertNotNull(repository.getHabit(1))

            repository.deleteHabit(1)

            assertNull(repository.getHabit(1))
            assertFalse(repository.habitIdsToday().contains(1L))
        }

    @Test
    fun `a deleted habit leaves no progress behind`() =
        runBlocking {
            val repository = repository()
            repository.setProgress(today, habitId = 1, progress = 5)
            repository.deleteHabit(1)

            // The same id is handed out again: the old value must not reappear.
            val id =
                repository.upsertHabit(
                    Habit(NEW_HABIT_ID, "New", target = 8, unit = "x", icon = "water_drop"),
                )
            val entry =
                repository
                    .observeDay(today)
                    .first()
                    .entries
                    .first { it.habit.id == id }

            assertEquals(0, entry.progress)
        }
}
