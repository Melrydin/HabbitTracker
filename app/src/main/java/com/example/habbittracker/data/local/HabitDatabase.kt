package com.example.habbittracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The local database (F6 exports from these tables).
 *
 * Bump [SCHEMA_VERSION] together with a migration whenever the tables change.
 * There is no destructive fallback: losing a user's history silently is worse
 * than a crash that shows up in testing.
 */
@Database(
    entities = [HabitEntity::class, DayEntity::class, DayHabitEntity::class],
    version = HabitDatabase.SCHEMA_VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    abstract fun dayDao(): DayDao

    abstract fun dayHabitDao(): DayHabitDao

    companion object {
        const val SCHEMA_VERSION = 1
        const val NAME = "habits.db"
    }
}
