package com.example.habbittracker.domain

import com.example.habbittracker.domain.model.DayStatus
import java.time.LocalDate

/**
 * Share of passed days among the days that were actually judged (F4).
 *
 * Neutral days are left out of both numbers on purpose: counting them would
 * punish a break, and counting them as successes would flatter the result.
 */
data class CompletionRate(val passed: Int, val failed: Int) {
    val evaluated: Int get() = passed + failed

    val fraction: Float get() = if (evaluated == 0) 0f else passed.toFloat() / evaluated

    val percent: Int get() = Math.round(fraction * 100)
}

/** Completion rate over [from] to [to], both inclusive. */
fun completionRate(statuses: Map<LocalDate, DayStatus>, from: LocalDate, to: LocalDate): CompletionRate {
    val inRange = statuses.filterKeys { it >= from && it <= to }.values
    return CompletionRate(
        passed = inRange.count { it == DayStatus.PASSED },
        failed = inRange.count { it == DayStatus.FAILED },
    )
}
