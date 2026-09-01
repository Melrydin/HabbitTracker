package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitEntry
import com.example.habbittracker.domain.model.HabitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exactly one habit gives a day its theme, or none does (F8). */
class DayThemeTest {
    private fun habit(id: Long, name: String, givesTheme: Boolean = false) =
        Habit(
            id = id,
            name = name,
            type = HabitType.CHECK,
            target = 1,
            icon = "task_alt",
            givesTheme = givesTheme,
        )

    private fun entries(vararg habits: Habit) = habits.map { HabitEntry(it, progress = 0) }

    @Test
    fun `a single habit offering a theme takes it`() {
        val day = entries(habit(1, "Reading week", givesTheme = true), habit(2, "Exercise"))

        assertEquals(1L, DayTheme.of(chosenHabitId = null, entries = day)?.id)
        assertTrue(DayTheme.openChoice(null, day).isEmpty())
    }

    @Test
    fun `several offers leave the day without a theme until one is picked`() {
        val day = entries(habit(1, "Reading week", givesTheme = true), habit(2, "Cooking", givesTheme = true))

        assertNull(DayTheme.of(chosenHabitId = null, entries = day))
        assertEquals(listOf(1L, 2L), DayTheme.openChoice(null, day).map { it.id })
    }

    @Test
    fun `a pick wins over the offers`() {
        val day = entries(habit(1, "Reading week", givesTheme = true), habit(2, "Cooking", givesTheme = true))

        assertEquals(2L, DayTheme.of(chosenHabitId = 2, entries = day)?.id)
        assertTrue(DayTheme.openChoice(2, day).isEmpty())
    }

    @Test
    fun `a theme set by hand wins over a habit that offers one`() {
        val typed = habit(3, "Spring cleaning").copy(isThemeGenerated = true)
        val day = entries(habit(1, "Reading week", givesTheme = true), typed)

        assertEquals(3L, DayTheme.of(chosenHabitId = 3, entries = day)?.id)
    }

    @Test
    fun `a theme habit that is not part of the day decides nothing`() {
        val day = entries(habit(1, "Exercise"))

        assertNull(DayTheme.of(chosenHabitId = 99, entries = day))
    }

    @Test
    fun `a day without any offer has no theme`() {
        assertNull(DayTheme.of(chosenHabitId = null, entries = entries(habit(1, "Exercise"))))
    }
}
