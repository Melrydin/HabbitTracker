package com.example.habbittracker.domain

import java.time.LocalDate

/**
 * Current and longest streak (F4). Only days with `passed = true` count; a failed
 * or empty day resets the run.
 */
object StreakCalculator {
    /**
     * Length of the run ending on [today].
     *
     * While [today] has not passed yet the run is counted up to yesterday, so the day
     * in progress does not break the streak before it is over.
     */
    fun currentStreak(passedDates: Set<LocalDate>, today: LocalDate): Int {
        var cursor = if (today in passedDates) today else today.minusDays(1)
        var streak = 0
        while (cursor in passedDates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /** Longest uninterrupted run across the whole history. */
    fun longestStreak(passedDates: Set<LocalDate>): Int {
        var longest = 0
        var run = 0
        var previous: LocalDate? = null
        for (date in passedDates.sorted()) {
            run = if (previous != null && previous.plusDays(1) == date) run + 1 else 1
            longest = maxOf(longest, run)
            previous = date
        }
        return longest
    }
}
