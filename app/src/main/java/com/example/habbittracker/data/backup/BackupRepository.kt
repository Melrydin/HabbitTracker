package com.example.habbittracker.data.backup

import java.io.InputStream
import java.io.OutputStream

/** Why a file could not be restored (F6). */
enum class BackupProblem {
    /** The ZIP has no manifest, so it is not one of ours. */
    NOT_A_BACKUP,

    /** Written by a newer app version whose schema this build does not know. */
    NEWER_SCHEMA,

    /** Readable as a ZIP, but the contents do not parse. */
    DAMAGED,
}

sealed interface ImportResult {
    data class Restored(val habits: Int, val days: Int) : ImportResult

    data class Rejected(val problem: BackupProblem) : ImportResult
}

/**
 * Backup and restore as a ZIP of JSON files (F6).
 *
 * Restoring replaces the database rather than merging into it, which is what the
 * feature list asks for in V1. It runs in one transaction, so a file that turns
 * out to be damaged half way through leaves the existing data untouched.
 */
interface BackupRepository {
    suspend fun export(target: OutputStream)

    suspend fun import(source: InputStream): ImportResult
}
