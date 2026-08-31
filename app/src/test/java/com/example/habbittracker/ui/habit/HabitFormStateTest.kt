package com.example.habbittracker.ui.habit

import com.example.habbittracker.data.HabitRepository.Companion.NEW_HABIT_ID
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitFormStateTest {
    @Test
    fun `ein leerer name blockiert das speichern`() {
        val form = HabitFormState()

        assertEquals(HabitFormError.NAME_REQUIRED, form.nameError)
        assertFalse(form.canSave)
    }

    @Test
    fun `ein name aus leerzeichen zaehlt als leer`() {
        val form = HabitFormState().withName("   ")

        assertEquals(HabitFormError.NAME_REQUIRED, form.nameError)
    }

    @Test
    fun `der name wird auf die erlaubte laenge gekappt`() {
        val form = HabitFormState().withName("x".repeat(80))

        assertEquals(Habit.NAME_MAX_LENGTH, form.name.length)
    }

    @Test
    fun `der wechsel auf ja nein setzt ziel und einheit zurueck`() {
        val form =
            HabitFormState()
                .withName("Wasser")
                .withType(HabitType.COUNTER)
                .withTarget("8")
                .withUnit("Glaeser")
                .withType(HabitType.CHECK)

        assertEquals("1", form.target)
        assertEquals("", form.unit)
        assertFalse(form.showsTargetAndUnit)
    }

    @Test
    fun `der wechsel von ja nein auf anzahl zeigt keinen fehler`() {
        val form = HabitFormState().withName("Wasser").withType(HabitType.COUNTER)

        assertNull(form.targetError)
        assertTrue(form.canSave)
    }

    @Test
    fun `das ziel nimmt nur ziffern an`() {
        val form = HabitFormState().withType(HabitType.COUNTER).withTarget("-1a2,5")

        assertEquals("125", form.target)
    }

    @Test
    fun `ein leeres ziel blockiert das speichern`() {
        val form = HabitFormState().withName("Lesen").withType(HabitType.AMOUNT).withTarget("")

        assertEquals(HabitFormError.TARGET_REQUIRED, form.targetError)
        assertFalse(form.canSave)
    }

    @Test
    fun `ziel null ist zu klein`() {
        val form = HabitFormState().withName("Lesen").withType(HabitType.AMOUNT).withTarget("0")

        assertEquals(HabitFormError.TARGET_TOO_SMALL, form.targetError)
    }

    @Test
    fun `ein ja nein habit braucht kein ziel im formular`() {
        val form = HabitFormState().withName("Sport").withTarget("")

        assertNull(form.targetError)
        assertTrue(form.canSave)
        assertEquals(1, form.toHabit().target)
    }

    @Test
    fun `punkte bleiben im erlaubten bereich`() {
        assertEquals(HabitFormState.POINTS_MIN, HabitFormState().withPoints(0).points)
        assertEquals(HabitFormState.POINTS_MAX, HabitFormState().withPoints(500).points)
    }

    @Test
    fun `toHabit trimmt und uebernimmt die eingaben`() {
        val habit =
            HabitFormState()
                .withName("  Wasser trinken  ")
                .withType(HabitType.COUNTER)
                .withTarget("8")
                .withUnit(" Glaeser ")
                .withPoints(2)
                .copy(required = true, icon = "water_drop")
                .toHabit()

        assertEquals("Wasser trinken", habit.name)
        assertEquals(8, habit.target)
        assertEquals("Glaeser", habit.unit)
        assertEquals(2, habit.points)
        assertTrue(habit.required)
        assertEquals(NEW_HABIT_ID, habit.id)
    }

    @Test
    fun `ja nein speichert keine einheit`() {
        val habit = HabitFormState().withName("Sport").withUnit("min").toHabit()

        assertNull(habit.unit)
    }

    @Test
    fun `from und toHabit sind zueinander passend`() {
        val original =
            Habit(
                id = 7,
                name = "Lesen",
                type = HabitType.AMOUNT,
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
