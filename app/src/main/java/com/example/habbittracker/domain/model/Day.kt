package com.example.habbittracker.domain.model

import java.time.LocalDate

/** Daily goal rule, exactly one per day (F2). */
enum class GoalType {
    /** Passed once every habit with `required = true` is fulfilled. */
    ALL_REQUIRED,

    /** Passed once at least `goalThreshold` habits are fulfilled. */
    MIN_COUNT,

    /** Passed once the points of fulfilled habits reach `goalThreshold`. */
    POINTS,
}

/**
 * A calendar day with its theme and daily goal (F2).
 *
 * [passed] is recomputed and stored on every change so that history and streaks
 * stay queryable without recalculating them.
 */
data class Day(
    val date: LocalDate,
    val theme: String? = null,
    val goalType: GoalType = GoalType.POINTS,
    val goalThreshold: Int = 0,
    val passed: Boolean = false,
) {
    init {
        require((theme?.length ?: 0) <= THEME_MAX_LENGTH) { "theme must be at most $THEME_MAX_LENGTH characters" }
    }

    companion object {
        const val THEME_MAX_LENGTH = 40
    }
}
