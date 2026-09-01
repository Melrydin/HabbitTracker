package com.example.habbittracker.domain.model

import java.time.LocalDate

/**
 * A break during which nothing is expected and streaks pause instead of breaking
 * (F4, V2).
 *
 * Part of the schema from the MVP on so that V2 needs no Room migration; nothing
 * writes to it yet. A null [habitId] pauses the whole app.
 */
data class Pause(
    val id: Long,
    val from: LocalDate,
    val to: LocalDate,
    val habitId: Long? = null,
)
