package com.example.habbittracker.domain.model

/** Which color scheme the app follows (F7). */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * App wide settings (F7).
 *
 * [defaultGoalThreshold] applies to [GoalType.POINTS] only. The other rules derive
 * their bar from the habits of the day, see
 * [com.example.habbittracker.domain.DayEvaluator].
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultGoalType: GoalType = GoalType.POINTS,
    val defaultGoalThreshold: Int = DEFAULT_GOAL_THRESHOLD,
    /** Grace days per calendar month; zero switches the protection off (F4). */
    val freezePerMonth: Int = DEFAULT_FREEZE_PER_MONTH,
) {
    companion object {
        const val DEFAULT_GOAL_THRESHOLD = 6
        const val GOAL_THRESHOLD_MIN = 1
        const val GOAL_THRESHOLD_MAX = 99
        const val DEFAULT_FREEZE_PER_MONTH = 1
        const val FREEZE_MAX = 10
    }
}
