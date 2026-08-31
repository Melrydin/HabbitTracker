package com.example.habbittracker.data

import com.example.habbittracker.data.HabitRepository.Companion.NEW_HABIT_ID
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitType
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
    fun `ein neuer habit bekommt eine id und taucht in der liste auf`() =
        runBlocking {
            val repository = repository()
            val newHabit = Habit(NEW_HABIT_ID, "Spazieren", HabitType.CHECK, target = 1, icon = "directions_run")

            val id = repository.upsertHabit(newHabit)

            assertTrue(id != NEW_HABIT_ID)
            assertEquals("Spazieren", repository.getHabit(id)?.name)
            assertTrue(repository.observeHabits().first().any { it.id == id })
        }

    @Test
    fun `bearbeiten ersetzt den habit statt einen zweiten anzulegen`() =
        runBlocking {
            val repository = repository()
            val before = repository.observeHabits().first().size

            val sport = repository.getHabit(2)!!
            repository.upsertHabit(sport.copy(name = "Joggen", points = 5))

            val habits = repository.observeHabits().first()
            assertEquals(before, habits.size)
            assertEquals("Joggen", habits.first { it.id == 2L }.name)
        }

    @Test
    fun `ein archivierter habit ohne erfassung verschwindet aus dem tag`() =
        runBlocking {
            val repository = repository()
            assertTrue(repository.habitIdsToday().contains(4L))

            repository.setArchived(4, archived = true)

            assertFalse(repository.habitIdsToday().contains(4L))
        }

    @Test
    fun `ein archivierter habit mit erfassung bleibt am tag sichtbar`() =
        runBlocking {
            val repository = repository()
            repository.setProgress(today, habitId = 4, progress = 1)

            repository.setArchived(4, archived = true)

            assertTrue(repository.habitIdsToday().contains(4L))
        }

    @Test
    fun `wiederherstellen holt den habit zurueck`() =
        runBlocking {
            val repository = repository()
            repository.setArchived(4, archived = true)

            repository.setArchived(4, archived = false)

            assertTrue(repository.habitIdsToday().contains(4L))
        }

    @Test
    fun `eine geaenderte punktzahl rechnet den tagesstatus neu`() =
        runBlocking {
            val repository = repository()
            // Sport bringt 3 von 6 noetigen Punkten: der Tag ist damit noch offen.
            repository.setProgress(today, habitId = 2, progress = 1)
            assertFalse(
                repository
                    .observeDay(today)
                    .first()
                    .day.passed,
            )

            val sport = repository.getHabit(2)!!
            repository.upsertHabit(sport.copy(points = 6))

            assertTrue(
                repository
                    .observeDay(today)
                    .first()
                    .day.passed,
            )
        }

    @Test
    fun `archivieren nimmt einem bestandenen tag sein ergebnis nicht weg`() =
        runBlocking {
            val repository = repository()
            repository.setProgress(today, habitId = 2, progress = 1)
            repository.setProgress(today, habitId = 3, progress = 30)
            repository.setProgress(today, habitId = 4, progress = 1)
            assertTrue(
                repository
                    .observeDay(today)
                    .first()
                    .day.passed,
            )

            // Sport hat heute schon gezaehlt, also bleibt der Tag bestanden (F1: alte Eintraege bleiben).
            repository.setArchived(2, archived = true)

            assertTrue(
                repository
                    .observeDay(today)
                    .first()
                    .day.passed,
            )
            assertTrue(repository.habitIdsToday().contains(2L))
        }

    @Test
    fun `loeschen entfernt den habit samt erfassten werten`() =
        runBlocking {
            val repository = repository()
            repository.setProgress(today, habitId = 1, progress = 5)
            assertNotNull(repository.getHabit(1))

            repository.deleteHabit(1)

            assertNull(repository.getHabit(1))
            assertFalse(repository.habitIdsToday().contains(1L))
        }

    @Test
    fun `ein geloeschter habit laesst keinen fortschritt zurueck`() =
        runBlocking {
            val repository = repository()
            repository.setProgress(today, habitId = 1, progress = 5)
            repository.deleteHabit(1)

            // Gleiche Id neu vergeben: der alte Wert darf nicht wieder auftauchen.
            val id =
                repository.upsertHabit(
                    Habit(NEW_HABIT_ID, "Neu", HabitType.COUNTER, target = 8, unit = "x", icon = "water_drop"),
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
