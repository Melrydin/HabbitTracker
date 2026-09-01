package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.Pause
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PausesTest {
    private val from = LocalDate.of(2026, 8, 10)
    private val to = LocalDate.of(2026, 8, 14)

    private fun pause(habitId: Long? = null) = Pause(id = 1, from = from, to = to, habitId = habitId)

    @Test
    fun `both ends belong to the period`() {
        val pauses = listOf(pause())

        assertTrue(Pauses.isPaused(pauses, from))
        assertTrue(Pauses.isPaused(pauses, to))
    }

    @Test
    fun `a day outside the period is not paused`() {
        val pauses = listOf(pause())

        assertFalse(Pauses.isPaused(pauses, from.minusDays(1)))
        assertFalse(Pauses.isPaused(pauses, to.plusDays(1)))
    }

    @Test
    fun `a pause for one habit does not stop the app`() {
        val pauses = listOf(pause(habitId = 7))

        assertFalse(Pauses.isPaused(pauses, from))
        assertEquals(setOf(7L), Pauses.pausedHabits(pauses, from))
    }

    @Test
    fun `a global pause stops nothing habit specific`() {
        val pauses = listOf(pause())

        assertTrue(Pauses.pausedHabits(pauses, from).isEmpty())
    }

    @Test
    fun `pauses of several habits add up`() {
        val pauses = listOf(pause(habitId = 7), Pause(2, from, to, habitId = 9))

        assertEquals(setOf(7L, 9L), Pauses.pausedHabits(pauses, from))
    }
}
