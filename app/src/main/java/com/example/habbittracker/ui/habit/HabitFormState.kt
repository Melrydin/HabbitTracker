package com.example.habbittracker.ui.habit

import com.example.habbittracker.data.HabitRepository.Companion.NEW_HABIT_ID
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitKind
import com.example.habbittracker.domain.model.HabitType
import com.example.habbittracker.domain.model.Recurrence
import com.example.habbittracker.domain.model.StreakRule
import com.example.habbittracker.domain.model.WeekSpan
import com.example.habbittracker.domain.model.covers
import com.example.habbittracker.ui.icons.HabitIcons
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Reasons why the form cannot be saved yet. */
enum class HabitFormError {
    NAME_REQUIRED,
    TARGET_REQUIRED,
    TARGET_TOO_SMALL,
    TARGET_TOO_LARGE,
    PARENT_REQUIRED,
    WEEKDAYS_REQUIRED,
}

/**
 * State of the habit editor (F1).
 *
 * [target] is deliberately held as text: a number field may be empty in between
 * without the state inventing a value for it. The conversion into a [Habit] only
 * happens on save.
 */
data class HabitFormState(
    val id: Long = NEW_HABIT_ID,
    val name: String = "",
    val type: HabitType = HabitType.CHECK,
    val target: String = "1",
    val unit: String = "",
    val points: Int = 1,
    val required: Boolean = false,
    val icon: String = HabitIcons.FALLBACK,
    val note: String = "",
    val streakRule: StreakRule = StreakRule.DAILY,
    val perWeekTarget: Int = DEFAULT_PER_WEEK,
    val archived: Boolean = false,
    // F8, where the habit sits in the week
    val kind: HabitKind = HabitKind.SIMPLE,
    val weekStart: LocalDate? = null,
    val weekSpan: WeekSpan = WeekSpan.FULL,
    val recurrence: Recurrence = Recurrence.EVERY_DAY,
    val parentId: Long? = null,
    val assignedDows: Set<Int> = emptySet(),
    val givesTheme: Boolean = false,
) {
    val isNew: Boolean get() = id == NEW_HABIT_ID

    /** Per the feature list CHECK always has target 1, so the field is hidden there. */
    val showsTargetAndUnit: Boolean get() = type != HabitType.CHECK

    private val parsedTarget: Int? get() = if (type == HabitType.CHECK) 1 else target.trim().toIntOrNull()

    val nameError: HabitFormError?
        get() = if (name.isBlank()) HabitFormError.NAME_REQUIRED else null

    val targetError: HabitFormError?
        get() {
            if (type == HabitType.CHECK) return null
            val value = parsedTarget ?: return HabitFormError.TARGET_REQUIRED
            return when {
                value < 1 -> HabitFormError.TARGET_TOO_SMALL
                value > TARGET_MAX -> HabitFormError.TARGET_TOO_LARGE
                else -> null
            }
        }

    /** A sub habit without a week to hang off would never turn up anywhere (F8). */
    val parentError: HabitFormError?
        get() = if (kind == HabitKind.SUB && parentId == null) HabitFormError.PARENT_REQUIRED else null

    val weekdaysError: HabitFormError?
        get() = if (kind == HabitKind.SUB && assignedDows.isEmpty()) HabitFormError.WEEKDAYS_REQUIRED else null

    val canSave: Boolean
        get() =
            nameError == null && targetError == null && parentError == null && weekdaysError == null &&
                (kind != HabitKind.WEEKLY || weekStart != null)

    /** Only a week habit is bound to a week; only a sub habit hangs off one. */
    val showsWeek: Boolean get() = kind == HabitKind.WEEKLY

    val showsParent: Boolean get() = kind == HabitKind.SUB

    // --- Edits ---

    fun withName(value: String) = copy(name = value.take(Habit.NAME_MAX_LENGTH))

    /** Digits only, so signs and separators never appear in the first place. */
    fun withTarget(value: String) = copy(target = value.filter(Char::isDigit).take(TARGET_MAX_DIGITS))

    fun withUnit(value: String) = copy(unit = value.take(UNIT_MAX_LENGTH))

    fun withNote(value: String) = copy(note = value.take(Habit.NOTE_MAX_LENGTH))

    fun withStreakRule(value: StreakRule) = copy(streakRule = value)

    fun withPerWeekTarget(value: Int) = copy(perWeekTarget = value.coerceIn(1, DAYS_IN_WEEK))

    /** Only the weekly rule needs a count; the daily one asks for every active day. */
    val showsPerWeekTarget: Boolean get() = streakRule == StreakRule.WEEKLY_COUNT

    fun withPoints(value: Int) = copy(points = value.coerceIn(POINTS_MIN, POINTS_MAX))

    /**
     * Switching the kind drops what the other kinds carry, so a habit cannot keep a
     * week it no longer lives in. [thisMonday] seeds a new week habit with the
     * current week, which is the one the user almost always means.
     */
    fun withKind(value: HabitKind, thisMonday: LocalDate): HabitFormState =
        when (value) {
            HabitKind.SIMPLE -> copy(kind = value, weekStart = null, parentId = null, assignedDows = emptySet())
            HabitKind.WEEKLY -> copy(kind = value, weekStart = weekStart ?: thisMonday, parentId = null)
            HabitKind.SUB -> copy(kind = value, weekStart = null)
        }

    /** A week is always addressed by its Monday, whichever day of it was picked. */
    fun withWeek(value: LocalDate) = copy(weekStart = value.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))

    fun withWeekSpan(value: WeekSpan) =
        copy(
            weekSpan = value,
            assignedDows = assignedDows.filter(value::covers).toSet(),
        )

    fun withRecurrence(value: Recurrence) = copy(recurrence = value)

    fun withParent(value: Long) = copy(parentId = value)

    fun toggleDow(value: Int) =
        copy(assignedDows = if (value in assignedDows) assignedDows - value else assignedDows + value)

    fun withGivesTheme(value: Boolean) = copy(givesTheme = value)

    /** Switching to CHECK makes target and unit meaningless. */
    fun withType(value: HabitType): HabitFormState =
        when {
            value == type -> this

            value == HabitType.CHECK -> copy(type = value, target = "1", unit = "")

            // Coming from CHECK the target is "1", which is valid, so switching shows no error.
            else -> copy(type = value)
        }

    fun toHabit(): Habit {
        check(canSave) { "the form is incomplete" }
        return Habit(
            id = id,
            name = name.trim(),
            type = type,
            target = parsedTarget ?: 1,
            unit = if (type == HabitType.CHECK) null else unit.trim().ifBlank { null },
            points = points,
            required = required,
            icon = icon,
            note = note.trim().ifBlank { null },
            streakRule = streakRule,
            perWeekTarget = if (streakRule == StreakRule.WEEKLY_COUNT) perWeekTarget else null,
            archived = archived,
            kind = kind,
            parentId = parentId.takeIf { kind == HabitKind.SUB },
            weekStart = weekStart.takeIf { kind == HabitKind.WEEKLY },
            weekSpan = weekSpan.takeIf { kind == HabitKind.WEEKLY },
            recurrence = recurrence.takeIf { kind == HabitKind.WEEKLY },
            assignedDows = if (kind == HabitKind.SUB) assignedDows else emptySet(),
            givesTheme = givesTheme,
        )
    }

    companion object {
        const val TARGET_MAX = 9_999
        const val POINTS_MIN = 1
        const val POINTS_MAX = 99
        const val UNIT_MAX_LENGTH = 12
        const val DAYS_IN_WEEK = 7
        private const val DEFAULT_PER_WEEK = 3
        private const val TARGET_MAX_DIGITS = 4

        fun from(habit: Habit) =
            HabitFormState(
                id = habit.id,
                name = habit.name,
                type = habit.type,
                target = habit.target.toString(),
                unit = habit.unit.orEmpty(),
                points = habit.points,
                required = habit.required,
                icon = habit.icon,
                note = habit.note.orEmpty(),
                streakRule = habit.streakRule,
                perWeekTarget = habit.perWeekTarget ?: DEFAULT_PER_WEEK,
                archived = habit.archived,
                kind = habit.kind,
                weekStart = habit.weekStart,
                weekSpan = habit.weekSpan ?: WeekSpan.FULL,
                recurrence = habit.recurrence ?: Recurrence.EVERY_DAY,
                parentId = habit.parentId,
                assignedDows = habit.assignedDows,
                givesTheme = habit.givesTheme,
            )
    }
}
