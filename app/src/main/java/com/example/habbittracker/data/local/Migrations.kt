package com.example.habbittracker.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema 1 to 2: adds every field the V2 and V3 features will need (F4, F8, F10,
 * F11, F12) so that those can land without a further migration, and turns the
 * boolean `passed` into the three-valued day status.
 *
 * The app has no released version yet, so this could have been folded into
 * schema 1. It is a real migration anyway, because any installed debug build
 * carries data that should not be thrown away silently.
 *
 * Both existing tables are rebuilt rather than altered. `ALTER TABLE ADD COLUMN`
 * would leave column defaults behind that Room does not expect, and SQLite cannot
 * add the self-referencing foreign key on `parent_id` after the fact at all.
 */
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.rebuildHabits()
            db.rebuildDays()
            db.createGoalsAndPauses()
        }
    }

private fun SupportSQLiteDatabase.rebuildHabits() {
    execSQL(
        "CREATE TABLE IF NOT EXISTS `habits_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`name` TEXT NOT NULL, `type` TEXT NOT NULL, `target` INTEGER NOT NULL, `unit` TEXT, " +
            "`points` INTEGER NOT NULL, `required` INTEGER NOT NULL, `icon` TEXT NOT NULL, " +
            "`color_tag` INTEGER, `note` TEXT, `archived` INTEGER NOT NULL, `kind` TEXT NOT NULL, " +
            "`parent_id` INTEGER, `week_start` TEXT, `week_span` TEXT, `recurrence` TEXT, " +
            "`assigned_dows` TEXT NOT NULL, `gives_theme` INTEGER NOT NULL, " +
            "`is_theme_generated` INTEGER NOT NULL, `streak_rule` TEXT NOT NULL, " +
            "`per_week_target` INTEGER, `polarity` TEXT NOT NULL, `category` TEXT, " +
            "`tags` TEXT NOT NULL, `sort_index` INTEGER NOT NULL, " +
            "FOREIGN KEY(`parent_id`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
    )
    execSQL(
        "INSERT INTO habits_new (id, name, type, target, unit, points, required, icon, color_tag, " +
            "note, archived, kind, parent_id, week_start, week_span, recurrence, assigned_dows, " +
            "gives_theme, is_theme_generated, streak_rule, per_week_target, polarity, category, " +
            "tags, sort_index) " +
            "SELECT id, name, type, target, unit, points, required, icon, color_tag, " +
            "NULL, archived, 'SIMPLE', NULL, NULL, NULL, NULL, '', 0, 0, 'DAILY', NULL, 'GOOD', " +
            "NULL, '', 0 FROM habits",
    )
    execSQL("DROP TABLE habits")
    execSQL("ALTER TABLE habits_new RENAME TO habits")
    execSQL("CREATE INDEX IF NOT EXISTS `index_habits_parent_id` ON `habits` (`parent_id`)")
    execSQL("CREATE INDEX IF NOT EXISTS `index_habits_sort_index` ON `habits` (`sort_index`)")
}

/**
 * The free-text theme becomes a habit reference and `passed` becomes a status.
 * The old theme text is dropped: it has no habit to point at, and inventing one
 * during a migration would put rows into the day that the user never created.
 */
private fun SupportSQLiteDatabase.rebuildDays() {
    execSQL(
        "CREATE TABLE IF NOT EXISTS `days_new` (`date` TEXT NOT NULL, `theme_habit_id` INTEGER, " +
            "`day_note` TEXT, `goal_type` TEXT NOT NULL, `goal_threshold` INTEGER NOT NULL, " +
            "`status` TEXT NOT NULL, PRIMARY KEY(`date`))",
    )
    execSQL(
        "INSERT INTO days_new (date, theme_habit_id, day_note, goal_type, goal_threshold, status) " +
            "SELECT date, NULL, NULL, goal_type, goal_threshold, " +
            "CASE WHEN passed = 1 THEN 'PASSED' ELSE 'NEUTRAL' END FROM days",
    )
    execSQL("DROP TABLE days")
    execSQL("ALTER TABLE days_new RENAME TO days")
}

private fun SupportSQLiteDatabase.createGoalsAndPauses() {
    execSQL(
        "CREATE TABLE IF NOT EXISTS `goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`habit_id` INTEGER NOT NULL, `target_count` INTEGER NOT NULL, `period_start` TEXT NOT NULL, " +
            "`period_end` TEXT NOT NULL, `reward` TEXT, `achieved` INTEGER NOT NULL, " +
            "FOREIGN KEY(`habit_id`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
    )
    execSQL("CREATE INDEX IF NOT EXISTS `index_goals_habit_id` ON `goals` (`habit_id`)")
    execSQL(
        "CREATE TABLE IF NOT EXISTS `pauses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`from_date` TEXT NOT NULL, `to_date` TEXT NOT NULL, `habit_id` INTEGER, " +
            "FOREIGN KEY(`habit_id`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
    )
    execSQL("CREATE INDEX IF NOT EXISTS `index_pauses_habit_id` ON `pauses` (`habit_id`)")
}

/**
 * Schema 2 to 3: the AMOUNT habit type was merged into COUNTER.
 *
 * The tables are unchanged, only their contents: a row still naming AMOUNT would
 * fail to read once the enum no longer has that constant, which would take the
 * whole habit list down.
 */
val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("UPDATE habits SET type = 'COUNTER' WHERE type = 'AMOUNT'")
        }
    }

/**
 * Schema 3 to 4: the habit type is gone, every habit is a count with a target.
 *
 * A done/not-done habit was a CHECK with a target of one, which is exactly what a
 * count of one means, so no value needs converting - the column simply goes.
 */
val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `habits_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, `target` INTEGER NOT NULL, `unit` TEXT, " +
                    "`points` INTEGER NOT NULL, `required` INTEGER NOT NULL, `icon` TEXT NOT NULL, " +
                    "`color_tag` INTEGER, `note` TEXT, `archived` INTEGER NOT NULL, `kind` TEXT NOT NULL, " +
                    "`parent_id` INTEGER, `week_start` TEXT, `week_span` TEXT, `recurrence` TEXT, " +
                    "`assigned_dows` TEXT NOT NULL, `gives_theme` INTEGER NOT NULL, " +
                    "`is_theme_generated` INTEGER NOT NULL, `streak_rule` TEXT NOT NULL, " +
                    "`per_week_target` INTEGER, `polarity` TEXT NOT NULL, `category` TEXT, " +
                    "`tags` TEXT NOT NULL, `sort_index` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`parent_id`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL(
                "INSERT INTO habits_new (id, name, target, unit, points, required, icon, color_tag, " +
                    "note, archived, kind, parent_id, week_start, week_span, recurrence, assigned_dows, " +
                    "gives_theme, is_theme_generated, streak_rule, per_week_target, polarity, category, " +
                    "tags, sort_index) " +
                    "SELECT id, name, target, unit, points, required, icon, color_tag, " +
                    "note, archived, kind, parent_id, week_start, week_span, recurrence, assigned_dows, " +
                    "gives_theme, is_theme_generated, streak_rule, per_week_target, polarity, category, " +
                    "tags, sort_index FROM habits",
            )
            db.execSQL("DROP TABLE habits")
            db.execSQL("ALTER TABLE habits_new RENAME TO habits")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_parent_id` ON `habits` (`parent_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_sort_index` ON `habits` (`sort_index`)")
        }
    }
