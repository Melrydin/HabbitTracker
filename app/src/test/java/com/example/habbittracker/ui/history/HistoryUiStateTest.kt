package com.example.habbittracker.ui.history

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class HistoryUiStateTest {
    private val today = LocalDate.of(2026, 8, 31)

    private fun state(month: YearMonth) = HistoryUiState(month = month, today = today)

    @Test
    fun `a past month can move forward`() {
        assertTrue(state(YearMonth.of(2026, 7)).canGoForward)
    }

    @Test
    fun `the current month cannot move forward`() {
        assertFalse(state(YearMonth.of(2026, 8)).canGoForward)
    }

    @Test
    fun `a month in the next year cannot move forward either`() {
        assertFalse(state(YearMonth.of(2027, 1)).canGoForward)
    }
}
