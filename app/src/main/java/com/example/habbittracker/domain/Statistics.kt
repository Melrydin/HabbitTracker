package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.DayStatus
import java.time.DayOfWeek
import java.time.LocalDate

/** How one weekday tends to go (F4). */
data class WeekdayRate(val day: DayOfWeek, val rate: CompletionRate)

/** Two periods side by side, for "this week against last" (F4). */
data class RateComparison(val current: CompletionRate, val previous: CompletionRate) {
    /** Positive when the current period is doing better. */
    val differenceInPercent: Int get() = current.percent - previous.percent
}

/**
 * Statistics over the stored day statuses (F4). All of it is derived from data
 * the app already holds; nothing is recorded just to be counted later.
 */
object Statistics {
    /** One rate per weekday, Monday first, weekdays without judged days included. */
    fun weekdayRates(statuses: Map<LocalDate, DayStatus>): List<WeekdayRate> =
        DayOfWeek.entries.map { day ->
            val ofDay = statuses.filterKeys { it.dayOfWeek == day }.values
            WeekdayRate(
                day = day,
                rate =
                    CompletionRate(
                        passed = ofDay.count { it == DayStatus.PASSED },
                        failed = ofDay.count { it == DayStatus.FAILED },
                    ),
            )
        }

    /** The strongest weekday, ignoring those nothing was ever judged on. */
    fun bestWeekday(statuses: Map<LocalDate, DayStatus>): WeekdayRate? =
        weekdayRates(statuses).filter { it.rate.evaluated > 0 }.maxByOrNull { it.rate.fraction }

    fun weakestWeekday(statuses: Map<LocalDate, DayStatus>): WeekdayRate? =
        weekdayRates(statuses).filter { it.rate.evaluated > 0 }.minByOrNull { it.rate.fraction }

    /** How a habit did over a period: fulfilled days against the days it applied. */
    fun habitRate(fulfillment: Map<LocalDate, Boolean>, from: LocalDate, to: LocalDate): CompletionRate {
        val inRange = fulfillment.filterKeys { it >= from && it <= to }.values
        return CompletionRate(passed = inRange.count { it }, failed = inRange.count { !it })
    }
}
