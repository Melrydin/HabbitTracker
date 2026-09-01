package com.example.habbittracker.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.habbittracker.domain.model.DayStatus
import com.example.habbittracker.domain.model.HabitKind
import com.example.habbittracker.domain.model.HabitType
import com.example.habbittracker.domain.model.Polarity
import com.example.habbittracker.domain.model.StreakRule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Checks that an installed schema 1 database survives the upgrade. A broken
 * migration only shows up when a user updates, which is the worst moment to find
 * out, so the old schema is rebuilt by hand here and migrated for real.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class MigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(name)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    private fun createSchemaOne() {
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null)
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `type` TEXT NOT NULL, `target` INTEGER NOT NULL, `unit` TEXT, " +
                "`points` INTEGER NOT NULL, `required` INTEGER NOT NULL, `icon` TEXT NOT NULL, " +
                "`color_tag` INTEGER, `archived` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `days` (`date` TEXT NOT NULL, `theme` TEXT, " +
                "`goal_type` TEXT NOT NULL, `goal_threshold` INTEGER NOT NULL, " +
                "`passed` INTEGER NOT NULL, PRIMARY KEY(`date`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `day_habits` (`date` TEXT NOT NULL, `habit_id` INTEGER NOT NULL, " +
                "`progress` INTEGER NOT NULL, PRIMARY KEY(`date`, `habit_id`), " +
                "FOREIGN KEY(`habit_id`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_day_habits_habit_id` ON `day_habits` (`habit_id`)")
        db.execSQL(
            "INSERT INTO habits (id, name, type, target, unit, points, required, icon, color_tag, archived) " +
                "VALUES (1, 'Exercise', 'CHECK', 1, NULL, 3, 1, 'directions_run', NULL, 0)",
        )
        db.execSQL(
            "INSERT INTO days (date, theme, goal_type, goal_threshold, passed) " +
                "VALUES ('2026-08-30', 'Calm focus', 'POINTS', 6, 1)",
        )
        db.execSQL("INSERT INTO day_habits (date, habit_id, progress) VALUES ('2026-08-30', 1, 1)")
        db.version = 1
        db.close()
    }

    private fun openMigrated(): HabitDatabase =
        Room
            .databaseBuilder(context, HabitDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()

    @Test
    fun `habits survive the upgrade and gain their new defaults`() =
        runBlocking {
            createSchemaOne()

            val db = openMigrated()
            val habit = db.habitDao().getById(1)!!.toDomain()
            db.close()

            assertEquals("Exercise", habit.name)
            assertEquals(3, habit.points)
            assertEquals(HabitKind.SIMPLE, habit.kind)
            assertEquals(StreakRule.DAILY, habit.streakRule)
            assertEquals(Polarity.GOOD, habit.polarity)
            assertEquals(emptySet<Int>(), habit.assignedDows)
            assertNull(habit.note)
        }

    @Test
    fun `a passed day keeps its result as a status`() =
        runBlocking {
            createSchemaOne()

            val db = openMigrated()
            val day = db.dayDao().get(LocalDate.of(2026, 8, 30))!!.toDomain()
            db.close()

            assertEquals(DayStatus.PASSED, day.status)
            assertEquals(6, day.goalThreshold)
            // The free-text theme has no habit to point at, so it is dropped.
            assertNull(day.themeHabitId)
            assertNull(day.dayNote)
        }

    @Test
    fun `recorded values survive the rebuild of the days table`() =
        runBlocking {
            createSchemaOne()

            val db = openMigrated()
            val recorded = db.dayHabitDao().getAll()
            db.close()

            assertEquals(1, recorded.size)
            assertEquals(1, recorded.single().progress)
        }

    @Test
    fun `an amount habit becomes a counter`() =
        runBlocking {
            createSchemaOne()
            // Schema 1 knew a third habit type that no longer exists.
            SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
                db.execSQL(
                    "INSERT INTO habits (id, name, type, target, unit, points, required, icon, " +
                        "color_tag, archived) " +
                        "VALUES (2, 'Read', 'AMOUNT', 30, 'min', 2, 0, 'menu_book', NULL, 0)",
                )
            }

            val db = openMigrated()
            val habit = db.habitDao().getById(2)!!.toDomain()
            db.close()

            assertEquals(HabitType.COUNTER, habit.type)
            assertEquals(30, habit.target)
            assertEquals("min", habit.unit)
        }

    @Test
    fun `an upgraded day follows the default until it is given its own goal`() =
        runBlocking {
            createSchemaOne()

            val db = openMigrated()
            val day = db.dayDao().get(LocalDate.of(2026, 8, 30))!!.toDomain()
            db.close()

            // Days that existed before the flag never chose a goal of their own.
            assertFalse(day.goalOverridden)
        }

    @Test
    fun `the new tables exist and start empty`() =
        runBlocking {
            createSchemaOne()

            val db = openMigrated()
            val goals = db.goalDao().getAll()
            val pauses = db.pauseDao().getAll()
            db.close()

            assertEquals(0, goals.size)
            assertEquals(0, pauses.size)
        }
}
