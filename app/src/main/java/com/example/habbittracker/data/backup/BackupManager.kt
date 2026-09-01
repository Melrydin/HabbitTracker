package com.example.habbittracker.data.backup

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Clock
import java.time.LocalDate

/** What happened when the user triggered a backup, ready to be shown (F6). */
sealed interface BackupOutcome {
    data object Exported : BackupOutcome

    data class Imported(val habits: Int, val days: Int) : BackupOutcome

    data class Failed(val problem: BackupProblem?) : BackupOutcome
}

/**
 * Bridges the storage access framework to [BackupRepository].
 *
 * Keeping the URI handling here means the repository stays a plain stream reader
 * and writer, and stays testable without Android.
 */
class BackupManager(
    private val context: Context,
    private val repository: BackupRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    /** `habits_backup_YYYY-MM-DD.zip`, the name the feature list asks for. */
    fun suggestedFileName(): String = "habits_backup_${LocalDate.now(clock)}.zip"

    suspend fun exportTo(target: Uri): BackupOutcome =
        withContext(Dispatchers.IO) {
            runCatching {
                val stream =
                    context.contentResolver.openOutputStream(target)
                        ?: throw IOException("cannot open $target for writing")
                stream.use { repository.export(it) }
                BackupOutcome.Exported
            }.getOrElse { BackupOutcome.Failed(problem = null) }
        }

    suspend fun importFrom(source: Uri): BackupOutcome =
        withContext(Dispatchers.IO) {
            runCatching {
                val stream =
                    context.contentResolver.openInputStream(source)
                        ?: throw IOException("cannot open $source for reading")
                when (val result = stream.use { repository.import(it) }) {
                    is ImportResult.Restored -> BackupOutcome.Imported(result.habits, result.days)
                    is ImportResult.Rejected -> BackupOutcome.Failed(result.problem)
                }
            }.getOrElse { BackupOutcome.Failed(problem = null) }
        }

    companion object {
        const val MIME_TYPE = "application/zip"
    }
}
