package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.DayStatus
import java.time.LocalDate
import java.time.YearMonth

/**
 * Grace days that keep a run alive (F4).
 *
 * A missed day spends one only when there is a run to protect and the month still
 * has budget. Spending one on a day with nothing behind it would burn the budget
 * for free, and a neutral day never needs one because it does not break a run
 * anyway.
 */
object StreakProtection {
    fun shouldFreeze(
        date: LocalDate,
        status: DayStatus,
        statuses: Map<LocalDate, DayStatus>,
        frozen: Set<LocalDate>,
        budgetPerMonth: Int,
    ): Boolean {
        if (status != DayStatus.FAILED || budgetPerMonth < 1) return false
        if (date in frozen) return true
        if (spentIn(YearMonth.from(date), frozen) >= budgetPerMonth) return false
        return protectsARun(date, statuses, frozen)
    }

    /** How many grace days a calendar month has already used. */
    fun spentIn(month: YearMonth, frozen: Set<LocalDate>): Int =
        frozen.count { YearMonth.from(it) == month }

    /** True when the nearest earlier day that counts for anything was passed. */
    private fun protectsARun(
        date: LocalDate,
        statuses: Map<LocalDate, DayStatus>,
        frozen: Set<LocalDate>,
    ): Boolean {
        val earliest = statuses.keys.minOrNull() ?: return false
        var cursor = date.minusDays(1)
        while (!cursor.isBefore(earliest)) {
            when (statuses.effectiveStatus(cursor, frozen)) {
                DayStatus.PASSED -> return true
                DayStatus.FAILED -> return false
                DayStatus.NEUTRAL -> Unit
            }
            cursor = cursor.minusDays(1)
        }
        return false
    }
}

/** A missed day that spent a grace day counts as neutral: skipped, not a break. */
internal fun Map<LocalDate, DayStatus>.effectiveStatus(date: LocalDate, frozen: Set<LocalDate>): DayStatus {
    val status = this[date] ?: DayStatus.NEUTRAL
    return if (status == DayStatus.FAILED && date in frozen) DayStatus.NEUTRAL else status
}
