package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.DayStatus
import java.time.LocalDate

/**
 * Current and longest streak (F4).
 *
 * Only [DayStatus.PASSED] counts up. [DayStatus.FAILED] resets the run, while
 * [DayStatus.NEUTRAL] is skipped: a day nothing was asked of neither extends nor
 * breaks a streak. Dates without a stored day count as neutral.
 */
object StreakCalculator {
    /**
     * Length of the run ending on [today].
     *
     * While [today] has not passed yet the run is counted up to yesterday, so the
     * day in progress does not break the streak before it is over.
     */
    fun currentStreak(statuses: Map<LocalDate, DayStatus>, today: LocalDate): Int {
        val earliest = statuses.keys.minOrNull() ?: return 0
        var cursor = if (statuses[today] == DayStatus.PASSED) today else today.minusDays(1)
        var streak = 0
        while (!cursor.isBefore(earliest)) {
            when (statuses[cursor] ?: DayStatus.NEUTRAL) {
                DayStatus.PASSED -> streak++
                DayStatus.FAILED -> return streak
                DayStatus.NEUTRAL -> Unit
            }
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /** Longest run across the whole history, under the same rules. */
    fun longestStreak(statuses: Map<LocalDate, DayStatus>): Int {
        var longest = 0
        var run = 0
        for (date in statuses.keys.sorted()) {
            when (statuses.getValue(date)) {
                DayStatus.PASSED -> run++
                DayStatus.FAILED -> run = 0
                DayStatus.NEUTRAL -> Unit
            }
            longest = maxOf(longest, run)
        }
        return longest
    }
}
