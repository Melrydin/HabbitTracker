package com.example.habbittracker.domain.model

/** Erfassungsart eines Habits (F1). */
enum class HabitType {
    /** Ja/Nein, `target` ist immer 1. */
    CHECK,

    /** Anzahl, z. B. 8 Glaeser. Darf das Ziel ueberschreiten. */
    COUNTER,

    /** Menge oder Dauer, z. B. 30 min. Darf das Ziel ueberschreiten. */
    AMOUNT,
}

/**
 * Definition eines Habits (Vorlage). Entspricht F1 der Featureliste.
 * Reines Domain-Modell, spaeter auf eine Room-Entity gemappt.
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
        require(name.length in 1..NAME_MAX_LENGTH) { "name muss 1 bis $NAME_MAX_LENGTH Zeichen haben" }
        require(target >= 1) { "target muss mindestens 1 sein" }
        require(type != HabitType.CHECK || target == 1) { "CHECK hat immer target = 1" }
    }

    companion object {
        const val NAME_MAX_LENGTH = 40
    }
}
