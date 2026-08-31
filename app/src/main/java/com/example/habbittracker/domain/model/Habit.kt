package com.example.habbittracker.domain.model

/** How a habit is tracked (F1). */
enum class HabitType {
    /** Yes/no. `target` is always 1. */
    CHECK,

    /** A count, for example 8 glasses. May exceed the target. */
    COUNTER,

    /** An amount or duration, for example 30 min. May exceed the target. */
    AMOUNT,
}

/**
 * The definition of a habit, used as a template. Covers F1 of the feature list.
 * A plain domain model, later mapped onto a Room entity.
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
    val archived: Boolean = false,
) {
    init {
        require(name.length in 1..NAME_MAX_LENGTH) { "name must be between 1 and $NAME_MAX_LENGTH characters" }
        require(target >= 1) { "target must be at least 1" }
        require(type != HabitType.CHECK || target == 1) { "CHECK always has target = 1" }
    }

    companion object {
        const val NAME_MAX_LENGTH = 40
    }
}
