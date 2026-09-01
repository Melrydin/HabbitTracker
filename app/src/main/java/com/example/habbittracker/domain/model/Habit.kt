package com.example.habbittracker.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

/** How a habit is tracked (F1). */
enum class HabitType {
    /** Yes/no. `target` is always 1. */
    CHECK,

    /** A count or amount, for example 8 glasses or 30 minutes. May exceed the target. */
    COUNTER,
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
        requireWeekly()
        requireSub()
    }

    /** A week habit is bound to one calendar week, which starts on a Monday (F8). */
    private fun requireWeekly() {
        if (kind != HabitKind.WEEKLY) {
            require(weekStart == null && weekSpan == null && recurrence == null) {
                "only a WEEKLY habit is bound to a week"
            }
            return
        }
        require(weekStart != null && weekSpan != null && recurrence != null) {
            "a WEEKLY habit needs a week, a span and a recurrence"
        }
        require(weekStart.dayOfWeek == DayOfWeek.MONDAY) { "weekStart is the Monday of its week" }
    }

    /** A sub habit hangs off a week habit and needs weekdays to appear on (F8). */
    private fun requireSub() {
        if (kind == HabitKind.SUB) {
            require(parentId != null) { "a SUB habit needs its WEEKLY parent" }
            require(assignedDows.isNotEmpty()) { "a SUB habit needs at least one weekday" }
        } else {
            require(parentId == null) { "only a SUB habit has a parent" }
        }
    }

    /**
     * The most a habit to avoid may reach and still count as a clean day (F11).
     *
     * A plain abstinence allows nothing at all; a reduction allows its target,
     * which is a ceiling there rather than something to reach.
     */
    val allowance: Int get() = if (type == HabitType.CHECK) 0 else target

    /**
     * What counts as done (F1, F11). A habit to build up asks for its target, one
     * to avoid asks to stay within what it allows — so an untouched day is already
     * a clean one, which is the whole point of tracking abstinence.
     */
    fun isFulfilledBy(progress: Int): Boolean =
        when (polarity) {
            Polarity.GOOD -> progress >= target
            Polarity.BAD -> progress <= allowance
        }

    companion object {
        const val NAME_MAX_LENGTH = 40
        const val NOTE_MAX_LENGTH = 500

        /** Used for habits the app creates itself, for example from a day theme (F2). */
        const val DEFAULT_ICON = "task_alt"
    }
}

/** Whether a weekday belongs to the span of a week habit (F8). */
fun WeekSpan.covers(dayOfWeek: Int): Boolean =
    when (this) {
        WeekSpan.WORKWEEK -> dayOfWeek in DayOfWeek.MONDAY.value..DayOfWeek.FRIDAY.value
        WeekSpan.FULL -> dayOfWeek in DayOfWeek.MONDAY.value..DayOfWeek.SUNDAY.value
    }
