package com.example.habbittracker.ui.habit

import com.example.habbittracker.data.HabitRepository.Companion.NEW_HABIT_ID
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitType
import com.example.habbittracker.ui.icons.HabitIcons

/** Reasons why the form cannot be saved yet. */
enum class HabitFormError {
    NAME_REQUIRED,
    TARGET_REQUIRED,
    TARGET_TOO_SMALL,
    TARGET_TOO_LARGE,
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
    val archived: Boolean = false,
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

    val canSave: Boolean get() = nameError == null && targetError == null

    // --- Edits ---

    fun withName(value: String) = copy(name = value.take(Habit.NAME_MAX_LENGTH))

    /** Digits only, so signs and separators never appear in the first place. */
    fun withTarget(value: String) = copy(target = value.filter(Char::isDigit).take(TARGET_MAX_DIGITS))

    fun withUnit(value: String) = copy(unit = value.take(UNIT_MAX_LENGTH))

    fun withNote(value: String) = copy(note = value.take(Habit.NOTE_MAX_LENGTH))

    fun withPoints(value: Int) = copy(points = value.coerceIn(POINTS_MIN, POINTS_MAX))

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
            archived = archived,
        )
    }

    companion object {
        const val TARGET_MAX = 9_999
        const val POINTS_MIN = 1
        const val POINTS_MAX = 99
        const val UNIT_MAX_LENGTH = 12
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
                archived = habit.archived,
            )
    }
}
