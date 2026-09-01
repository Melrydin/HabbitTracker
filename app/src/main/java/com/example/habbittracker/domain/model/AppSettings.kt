package com.example.habbittracker.domain.model

/** Which color scheme the app follows (F7). */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * App wide settings (F7). Defaults match the decisions in the feature list:
 * the daily goal counts points against a threshold of six, and the theme
 * follows the system.
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultGoalType: GoalType = GoalType.POINTS,
    val defaultGoalThreshold: Int = DEFAULT_GOAL_THRESHOLD,
) {
    companion object {
        const val DEFAULT_GOAL_THRESHOLD = 6
        const val GOAL_THRESHOLD_MIN = 1
        const val GOAL_THRESHOLD_MAX = 99
    }
}
