package com.example.habbittracker.ui.habit

import com.example.habbittracker.data.HabitRepository.Companion.NEW_HABIT_ID
import com.example.habbittracker.domain.model.Habit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitFormStateTest {
    @Test
    fun `an empty name blocks saving`() {
        val form = HabitFormState()

        assertEquals(HabitFormError.NAME_REQUIRED, form.nameError)
        assertFalse(form.canSave)
    }

    @Test
    fun `a name made of spaces counts as empty`() {
        val form = HabitFormState().withName("   ")

        assertEquals(HabitFormError.NAME_REQUIRED, form.nameError)
    }

    @Test
    fun `the name is truncated to the allowed length`() {
        val form = HabitFormState().withName("x".repeat(80))

        assertEquals(Habit.NAME_MAX_LENGTH, form.name.length)
    }

    @Test
    fun `a fresh form starts at a target of one`() {
        val form = HabitFormState().withName("Exercise")

        assertEquals("1", form.target)
        assertNull(form.targetError)
        assertTrue(form.canSave)
    }

    @Test
    fun `the target accepts digits only`() {
        val form = HabitFormState().withTarget("-1a2,5")

        assertEquals("125", form.target)
    }

    @Test
    fun `an empty target blocks saving`() {
        val form = HabitFormState().withName("Read").withTarget("")

        assertEquals(HabitFormError.TARGET_REQUIRED, form.targetError)
        assertFalse(form.canSave)
    }

    @Test
    fun `a target of zero is too small`() {
        val form = HabitFormState().withName("Read").withTarget("0")

        assertEquals(HabitFormError.TARGET_TOO_SMALL, form.targetError)
    }

    @Test
    fun `points stay within the allowed range`() {
        assertEquals(HabitFormState.POINTS_MIN, HabitFormState().withPoints(0).points)
        assertEquals(HabitFormState.POINTS_MAX, HabitFormState().withPoints(500).points)
    }

    @Test
    fun `toHabit trims and carries over the input`() {
        val habit =
            HabitFormState()
                .withName("  Drink water  ")
                .withTarget("8")
                .withUnit(" glasses ")
                .withPoints(2)
                .copy(required = true, icon = "water_drop")
                .toHabit()

        assertEquals("Drink water", habit.name)
        assertEquals(8, habit.target)
        assertEquals("glasses", habit.unit)
        assertEquals(2, habit.points)
        assertTrue(habit.required)
        assertEquals(NEW_HABIT_ID, habit.id)
    }

    @Test
    fun `a blank note is stored as null rather than an empty string`() {
        val habit = HabitFormState().withName("Exercise").withNote("   ").toHabit()

        assertNull(habit.note)
    }

    @Test
    fun `a note is trimmed and kept`() {
        val habit = HabitFormState().withName("Exercise").withNote("  Twenty minutes  ").toHabit()

        assertEquals("Twenty minutes", habit.note)
    }

    @Test
    fun `the note is truncated to the allowed length`() {
        val form = HabitFormState().withNote("x".repeat(Habit.NOTE_MAX_LENGTH + 50))

        assertEquals(Habit.NOTE_MAX_LENGTH, form.note.length)
    }

    @Test
    fun `a blank unit is stored as null rather than an empty string`() {
        val habit = HabitFormState().withName("Exercise").withUnit("   ").toHabit()

        assertNull(habit.unit)
    }

    @Test
    fun `from and toHabit round trip`() {
        val original =
            Habit(
                id = 7,
                name = "Read",
                target = 30,
                unit = "min",
                points = 2,
                required = true,
                icon = "menu_book",
                archived = true,
            )

        assertEquals(original, HabitFormState.from(original).toHabit())
    }
}
