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
 * The computed outcome of a day (F2).
 *
 * [NEUTRAL] is the important one: a day nothing was asked of is neither a success
 * nor a failure, and it does not break a streak.
 */
enum class DayStatus {
    NEUTRAL,
    PASSED,
    FAILED,
}

/**
 * A calendar day with its theme habit, note and daily goal (F2).
 *
 * [status] is recomputed and stored on every change so that history and streaks
 * stay queryable without recalculating them.
 */
data class Day(
    val date: LocalDate,
    val themeHabitId: Long? = null,
    val dayNote: String? = null,
    val goalType: GoalType = GoalType.POINTS,
    val goalThreshold: Int = 0,
    /**
     * Set once the day carries a goal of its own (F2). Days without it follow the
     * default from the settings, which is why "the same as the default" and
     * "chosen by hand" have to be told apart.
     */
    val goalOverridden: Boolean = false,
    val status: DayStatus = DayStatus.NEUTRAL,
) {
    init {
        require((dayNote?.length ?: 0) <= NOTE_MAX_LENGTH) { "dayNote must be at most $NOTE_MAX_LENGTH characters" }
    }

    companion object {
        const val NOTE_MAX_LENGTH = 1_000
    }
}
