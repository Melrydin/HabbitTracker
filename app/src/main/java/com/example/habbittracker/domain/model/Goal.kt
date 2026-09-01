package com.example.habbittracker.domain.model

import java.time.LocalDate

/**
 * A long term goal for one habit (F10, V3).
 *
 * Part of the schema from the MVP on so that V3 needs no Room migration; nothing
 * writes to it yet.
 */
data class Goal(
    val id: Long,
    val habitId: Long,
    val targetCount: Int,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val reward: String? = null,
    val achieved: Boolean = false,
)
