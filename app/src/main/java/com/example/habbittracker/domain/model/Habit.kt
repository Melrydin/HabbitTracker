package com.example.habbittracker.domain.model

import java.time.LocalDate

/** How a habit is tracked (F1). */
enum class HabitType {
    /** Yes/no. `target` is always 1. */
    CHECK,

    /** A count, for example 8 glasses. May exceed the target. */
    COUNTER,

    /** An amount or duration, for example 30 min. May exceed the target. */
    AMOUNT,
}

/** Place in the two-level habit hierarchy (F8). */
enum class HabitKind {
    /** A normal template that stands on its own. */
    SIMPLE,

    /** A habit bound to one calendar week. */
    WEEKLY,

    /** A sub habit of a [WEEKLY] parent, assigned to specific weekdays. */
    SUB,
}

/** Which days of its week a [HabitKind.WEEKLY] habit covers (F8). */
enum class WeekSpan {
    /** Monday to Friday. */
    WORKWEEK,

    /** Monday to Sunday. */
    FULL,
}

/** How a [HabitKind.WEEKLY] habit shows up during its week (F8). */
enum class Recurrence {
    /** On every active day of the week. */
    EVERY_DAY,

    /** Not on its own, only through its sub habits. */
    BY_SUBS,
}

/** Rule behind a habit's own streak (F4, V2). */
enum class StreakRule {
    /** Fulfil it on every active day. */
    DAILY,

    /** A number of fulfillments per week is enough, see `perWeekTarget`. */
    WEEKLY_COUNT,
}

/** Whether a habit is built up or avoided (F11, V2). */
enum class Polarity {
    /** The normal case: doing it counts. */
    GOOD,

    /** Abstinence: a day without an incident counts. */
    BAD,
}

/**
 * The definition of a habit, used as a template. Covers F1 of the feature list.
 *
 * Fields for F4, F8, F11 and F12 are part of the schema from the MVP on even
 * though their UI arrives in V2, so that adding those features needs no Room
 * migration. They all carry defaults that behave like a plain daily habit.
 */
data class Habit(
    val id: Long,
    val name: String,
    val type: HabitType,
    val target: Int,
    val unit: String? = null,
    val points: Int = 1,
    val required: Boolean = false,
    val icon: String,
    val colorTag: Int? = null,
    val note: String? = null,
    val archived: Boolean = false,
    // F8, hierarchy and theme coupling
    val kind: HabitKind = HabitKind.SIMPLE,
    val parentId: Long? = null,
    val weekStart: LocalDate? = null,
    val weekSpan: WeekSpan? = null,
    val recurrence: Recurrence? = null,
    val assignedDows: Set<Int> = emptySet(),
    val givesTheme: Boolean = false,
    val isThemeGenerated: Boolean = false,
    // F4, the habit's own streak
    val streakRule: StreakRule = StreakRule.DAILY,
    val perWeekTarget: Int? = null,
    // F11, abstinence habits
    val polarity: Polarity = Polarity.GOOD,
    // F12, order and filtering
    val category: String? = null,
    val tags: Set<String> = emptySet(),
    val sortIndex: Int = 0,
) {
    init {
        require(name.length in 1..NAME_MAX_LENGTH) { "name must be between 1 and $NAME_MAX_LENGTH characters" }
        require(target >= 1) { "target must be at least 1" }
        require(type != HabitType.CHECK || target == 1) { "CHECK always has target = 1" }
        require((note?.length ?: 0) <= NOTE_MAX_LENGTH) { "note must be at most $NOTE_MAX_LENGTH characters" }
        require(assignedDows.all { it in 1..7 }) { "assignedDows holds weekdays 1..7" }
    }

    companion object {
        const val NAME_MAX_LENGTH = 40
        const val NOTE_MAX_LENGTH = 500

        /** Used for habits the app creates itself, for example from a day theme (F2). */
        const val DEFAULT_ICON = "task_alt"
    }
}
