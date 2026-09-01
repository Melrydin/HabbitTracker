package com.example.habbittracker.data.backup

import androidx.room.withTransaction
import com.example.habbittracker.data.SettingsRepository
import com.example.habbittracker.data.local.DayDao
import com.example.habbittracker.data.local.DayHabitDao
import com.example.habbittracker.data.local.GoalDao
import com.example.habbittracker.data.local.HabitDao
import com.example.habbittracker.data.local.HabitDatabase
import com.example.habbittracker.data.local.PauseDao
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ZipBackupRepository(
    private val database: HabitDatabase,
    private val habitDao: HabitDao,
    private val dayDao: DayDao,
    private val dayHabitDao: DayHabitDao,
    private val goalDao: GoalDao,
    private val pauseDao: PauseDao,
    private val settingsRepository: SettingsRepository,
    private val appVersion: String,
    private val clock: Clock = Clock.systemDefaultZone(),
) : BackupRepository {
    private val json =
        Json {
            prettyPrint = true
            // A backup written by a newer version may carry fields this build does not
            // know; dropping them beats refusing to restore anything at all.
            ignoreUnknownKeys = true
        }

    override suspend fun export(target: OutputStream) {
        val content = collect()
        ZipOutputStream(target.buffered()).use { zip ->
            content.forEach { (name, body) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(body.toByteArray())
                zip.closeEntry()
            }
        }
    }

    override suspend fun import(source: InputStream): ImportResult {
        val entries = readEntries(source)
        val manifest =
            entries[BackupEntries.MANIFEST]?.let {
                runCatching { json.decodeFromString<BackupManifest>(it) }.getOrNull()
            } ?: return ImportResult.Rejected(BackupProblem.NOT_A_BACKUP)

        if (manifest.schemaVersion > HabitDatabase.SCHEMA_VERSION) {
            return ImportResult.Rejected(BackupProblem.NEWER_SCHEMA)
        }
        return runCatching { restore(entries) }
            .getOrElse { error ->
                if (error is SerializationException) {
                    ImportResult.Rejected(BackupProblem.DAMAGED)
                } else {
                    throw error
                }
            }
    }

    private suspend fun collect(): Map<String, String> {
        val settings = settingsRepository.settings.first()
        return mapOf(
            BackupEntries.MANIFEST to
                json.encodeToString(
                    BackupManifest(
                        appVersion = appVersion,
                        schemaVersion = HabitDatabase.SCHEMA_VERSION,
                        exportedAt = LocalDate.now(clock).toString(),
                    ),
                ),
            BackupEntries.HABITS to json.encodeToString(habitDao.getAll().map { it.toBackup() }),
            BackupEntries.DAYS to json.encodeToString(dayDao.getAll().map { it.toBackup() }),
            BackupEntries.DAY_HABITS to json.encodeToString(dayHabitDao.getAll().map { it.toBackup() }),
            BackupEntries.GOALS to json.encodeToString(goalDao.getAll().map { it.toBackup() }),
            BackupEntries.SETTINGS to json.encodeToString(settings.toBackup()),
        )
    }

    /**
     * Older backups simply lack the newer fields; the defaults on the backup models
     * fill them in, which is all the migration a restore needs so far.
     */
    private suspend fun restore(entries: Map<String, String>): ImportResult {
        val habits = entries.decodeList<BackupHabit>(BackupEntries.HABITS)
        val days = entries.decodeList<BackupDay>(BackupEntries.DAYS)
        val recorded = entries.decodeList<BackupDayHabit>(BackupEntries.DAY_HABITS)
        val goals = entries.decodeList<BackupGoal>(BackupEntries.GOALS)
        val settings = entries[BackupEntries.SETTINGS]?.let { json.decodeFromString<BackupSettings>(it) }

        database.withTransaction {
            clearAll()
            // Parents first: a sub habit references its weekly parent by id.
            habitDao.insertAll(habits.sortedBy { it.parentId != null }.map { it.toEntity() })
            dayDao.upsertAll(days.map { it.toEntity() })
            dayHabitDao.upsertAll(recorded.map { it.toEntity() })
            goalDao.upsertAll(goals.map { it.toEntity() })
        }
        settings?.let { settingsRepository.replace(it.toDomain()) }
        return ImportResult.Restored(habits = habits.size, days = days.size)
    }

    private suspend fun clearAll() {
        pauseDao.deleteAll()
        goalDao.deleteAll()
        dayHabitDao.deleteAll()
        dayDao.deleteAll()
        habitDao.deleteAll()
    }

    private inline fun <reified T> Map<String, String>.decodeList(name: String): List<T> =
        this[name]?.let { json.decodeFromString<List<T>>(it) } ?: emptyList()

    private fun readEntries(source: InputStream): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        ZipInputStream(source.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) entries[entry.name] = zip.readBytes().decodeToString()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return entries
    }
}
