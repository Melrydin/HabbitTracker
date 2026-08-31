package com.example.habbittracker.domain.model

import java.time.LocalDate

/** Tagesziel-Regel, genau eine je Tag (F2). */
enum class GoalType {
    /** Bestanden, wenn alle Habits mit `required = true` erfuellt sind. */
    ALL_REQUIRED,

    /** Bestanden, wenn mindestens `goalThreshold` Habits erfuellt sind. */
    MIN_COUNT,

    /** Bestanden, wenn die Punktsumme erfuellter Habits >= `goalThreshold` ist. */
    POINTS,
}

/**
 * Ein Kalendertag mit Thema und Tagesziel (F2).
 *
 * [passed] wird bei jeder Erfassungsaenderung neu berechnet und persistiert,
 * damit Verlauf und Streak ohne Nachrechnen abfragbar bleiben.
 */
data class Day(
    val date: LocalDate,
    val theme: String? = null,
    val goalType: GoalType = GoalType.POINTS,
    val goalThreshold: Int = 0,
    val passed: Boolean = false,
) {
    init {
        require((theme?.length ?: 0) <= THEME_MAX_LENGTH) { "theme darf hoechstens $THEME_MAX_LENGTH Zeichen haben" }
    }

    companion object {
        const val THEME_MAX_LENGTH = 40
    }
}
