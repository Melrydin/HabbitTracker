package com.example.habbittracker.data.backup

import android.os.Build
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.habbittracker.data.HabitRepository.Companion.NEW_HABIT_ID
import com.example.habbittracker.data.RoomHabitRepository
import com.example.habbittracker.data.local.DataStoreSettingsRepository
import com.example.habbittracker.data.local.HabitDatabase
import com.example.habbittracker.domain.model.GoalType
import com.example.habbittracker.domain.model.Habit
import com.example.habbittracker.domain.model.HabitType
import com.example.habbittracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class ZipBackupRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val date = LocalDate.of(2026, 8, 31)

    private lateinit var database: HabitDatabase
    private lateinit var settings: DataStoreSettingsRepository
    private lateinit var habits: RoomHabitRepository
    private lateinit var backup: ZipBackupRepository

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HabitDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        settings =
            DataStoreSettingsRepository(
                PreferenceDataStoreFactory.create { folder.newFile("settings.preferences_pb") },
            )
        habits =
            RoomHabitRepository(
                database = database,
                habitDao = database.habitDao(),
                dayDao = database.dayDao(),
                dayHabitDao = database.dayHabitDao(),
                settings = settings.settings,
            )
        backup =
            ZipBackupRepository(
                database = database,
                habitDao = database.habitDao(),
                dayDao = database.dayDao(),
                dayHabitDao = database.dayHabitDao(),
                goalDao = database.goalDao(),
                pauseDao = database.pauseDao(),
                settingsRepository = settings,
                appVersion = "1.0",
            )
    }

    @After
    fun tearDown() = database.close()

    private suspend fun seed(): Long {
        val id =
            habits.upsertHabit(
                Habit(
                    id = NEW_HABIT_ID,
                    name = "Drink water",
                    type = HabitType.COUNTER,
                    target = 8,
                    unit = "glasses",
                    points = 2,
                    icon = "water_drop",
                    note = "Spread over the day",
                    tags = setOf("health"),
                ),
            )
        habits.setProgress(date, id, 5)
        habits.setDayNote(date, "A good one")
        return id
    }

    private suspend fun exported(): ByteArray =
        ByteArrayOutputStream()
            .also { backup.export(it) }
            .toByteArray()

    @Test
    fun `the archive holds every file the feature list names`() =
        runBlocking {
            seed()

            val names = mutableSetOf<String>()
            ZipInputStream(ByteArrayInputStream(exported())).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    names += entry.name
                    entry = zip.nextEntry
                }
            }

            assertTrue(
                names.containsAll(
                    setOf(
                        BackupEntries.MANIFEST,
                        BackupEntries.HABITS,
                        BackupEntries.DAYS,
                        BackupEntries.DAY_HABITS,
                        BackupEntries.GOALS,
                        BackupEntries.SETTINGS,
                    ),
                ),
            )
        }

    @Test
    fun `a round trip keeps habits, tracking and notes`() =
        runBlocking {
            val id = seed()
            val archive = exported()

            habits.deleteHabit(id)
            assertTrue(habits.observeHabits().first().isEmpty())

            val result = backup.import(ByteArrayInputStream(archive))

            assertEquals(ImportResult.Restored(habits = 1, days = 1), result)
            val restored = habits.observeHabits().first().single()
            assertEquals("Drink water", restored.name)
            assertEquals("Spread over the day", restored.note)
            assertEquals(setOf("health"), restored.tags)
            val snapshot = habits.observeDay(date).first()
            assertEquals(5, snapshot.entries.single().progress)
            assertEquals("A good one", snapshot.day.dayNote)
        }

    @Test
    fun `restoring replaces rather than merges`() =
        runBlocking {
            seed()
            val archive = exported()

            habits.upsertHabit(Habit(NEW_HABIT_ID, "Added later", HabitType.CHECK, 1, icon = "task_alt"))
            assertEquals(2, habits.observeHabits().first().size)

            backup.import(ByteArrayInputStream(archive))

            assertEquals(1, habits.observeHabits().first().size)
        }

    @Test
    fun `settings come back with the data`() =
        runBlocking {
            seed()
            settings.setThemeMode(ThemeMode.DARK)
            settings.setDefaultGoal(GoalType.MIN_COUNT, threshold = 3)
            val archive = exported()

            settings.setThemeMode(ThemeMode.LIGHT)
            settings.setDefaultGoal(GoalType.POINTS, threshold = 9)
            backup.import(ByteArrayInputStream(archive))

            val restored = settings.settings.first()
            assertEquals(ThemeMode.DARK, restored.themeMode)
            assertEquals(GoalType.MIN_COUNT, restored.defaultGoalType)
            assertEquals(3, restored.defaultGoalThreshold)
        }

    /** Builds a one-entry archive, for the cases where the contents are the point. */
    private fun archiveOf(name: String, body: String): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(body.toByteArray())
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    @Test
    fun `a file without a manifest is refused`() =
        runBlocking {
            val result = backup.import(ByteArrayInputStream(archiveOf("something.txt", "hello")))

            assertEquals(ImportResult.Rejected(BackupProblem.NOT_A_BACKUP), result)
        }

    @Test
    fun `a newer schema is refused instead of guessed at`() =
        runBlocking {
            val future =
                archiveOf(
                    BackupEntries.MANIFEST,
                    """{"appVersion":"9.9","schemaVersion":99,"exportedAt":"2026-08-31"}""",
                )

            val result = backup.import(ByteArrayInputStream(future))

            assertEquals(ImportResult.Rejected(BackupProblem.NEWER_SCHEMA), result)
        }

    @Test
    fun `an older backup naming the removed amount type restores as a counter`() =
        runBlocking {
            val withHabit = ByteArrayOutputStream()
            ZipOutputStream(withHabit).use { zip ->
                zip.putNextEntry(ZipEntry(BackupEntries.MANIFEST))
                zip.write("""{"appVersion":"0.9","schemaVersion":2,"exportedAt":"2026-08-01"}""".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry(BackupEntries.HABITS))
                zip.write(
                    """[{"id":1,"name":"Read","type":"AMOUNT","target":30,"unit":"min","icon":"menu_book"}]"""
                        .toByteArray(),
                )
                zip.closeEntry()
            }

            backup.import(ByteArrayInputStream(withHabit.toByteArray()))

            val restored = habits.observeHabits().first().single()
            assertEquals(HabitType.COUNTER, restored.type)
            assertEquals(30, restored.target)
        }

    @Test
    fun `a damaged archive leaves the existing data alone`() =
        runBlocking {
            val id = seed()
            val broken = archiveOf(BackupEntries.MANIFEST, "not json at all")

            val result = backup.import(ByteArrayInputStream(broken))

            assertEquals(ImportResult.Rejected(BackupProblem.NOT_A_BACKUP), result)
            assertEquals(
                id,
                habits
                    .observeHabits()
                    .first()
                    .single()
                    .id,
            )
        }
}
