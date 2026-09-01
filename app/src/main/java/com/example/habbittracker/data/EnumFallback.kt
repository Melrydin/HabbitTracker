package com.example.habbittracker.data

/**
 * Reads an enum by name and falls back instead of throwing.
 *
 * Stored data and backups can carry a name this build does not know, from a newer
 * version or a damaged file. One unexpected word should not cost a user their
 * settings or their history.
 */
internal inline fun <reified T : Enum<T>> String?.toEnumOr(fallback: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: fallback
