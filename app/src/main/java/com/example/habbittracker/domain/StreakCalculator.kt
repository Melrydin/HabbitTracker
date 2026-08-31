package com.example.habbittracker.domain

import java.time.LocalDate

/**
 * Aktuelle und laengste Streak (F4). Gezaehlt werden nur Tage mit `passed = true`,
 * ein nicht bestandener oder leerer Tag setzt die Serie zurueck.
 */
object StreakCalculator {

    /**
     * Laenge der Serie, die auf [today] endet.
     *
     * Ist [today] noch nicht bestanden, zaehlt die Serie bis gestern weiter. Der laufende
     * Tag bricht die Serie also nicht, solange er nicht vorbei ist.
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

    /** Laengste zusammenhaengende Serie ueber den gesamten Verlauf. */
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
