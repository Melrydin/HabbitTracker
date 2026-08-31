package com.example.habbittracker.ui.habit

import com.example.habbittracker.data.HabitRepository.Companion.NEW_HABIT_ID
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitType
import com.example.habbittracker.ui.icons.HabitIcons

/** Gruende, aus denen das Formular noch nicht gespeichert werden kann. */
enum class HabitFormError {
    NAME_REQUIRED,
    TARGET_REQUIRED,
    TARGET_TOO_SMALL,
    TARGET_TOO_LARGE,
}

/**
 * Zustand des Habit-Editors (F1).
 *
 * [target] und [points] liegen bewusst als Text vor: ein Zahlenfeld darf
 * zwischendurch leer sein, ohne dass der Zustand einen erfundenen Wert erfindet.
 * Die Umwandlung nach [Habit] passiert erst beim Speichern.
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
    val archived: Boolean = false,
) {
    val isNew: Boolean get() = id == NEW_HABIT_ID

    /** CHECK hat laut Featureliste immer das Ziel 1, das Feld wird dort nicht gezeigt. */
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

    // --- Bearbeitungen ---

    fun withName(value: String) = copy(name = value.take(Habit.NAME_MAX_LENGTH))

    /** Nur Ziffern, damit Vorzeichen und Trennzeichen gar nicht erst entstehen. */
    fun withTarget(value: String) = copy(target = value.filter(Char::isDigit).take(TARGET_MAX_DIGITS))

    fun withUnit(value: String) = copy(unit = value.take(UNIT_MAX_LENGTH))

    fun withPoints(value: Int) = copy(points = value.coerceIn(POINTS_MIN, POINTS_MAX))

    /** Beim Wechsel auf CHECK verlieren Ziel und Einheit ihre Bedeutung. */
    fun withType(value: HabitType): HabitFormState =
        when {
            value == type -> this

            value == HabitType.CHECK -> copy(type = value, target = "1", unit = "")

            // Von CHECK kommend ist das Ziel "1": gueltig, also ohne Fehlermeldung beim Umschalten.
            else -> copy(type = value)
        }

    fun toHabit(): Habit {
        check(canSave) { "Formular ist unvollstaendig" }
        return Habit(
            id = id,
            name = name.trim(),
            type = type,
            target = parsedTarget ?: 1,
            unit = if (type == HabitType.CHECK) null else unit.trim().ifBlank { null },
            points = points,
            required = required,
            icon = icon,
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
                archived = habit.archived,
            )
    }
}
