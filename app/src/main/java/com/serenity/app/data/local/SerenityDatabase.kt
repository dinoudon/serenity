package com.serenity.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RitualEntity::class, AchievementUnlockEntity::class, UserProgressEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SerenityDatabase : RoomDatabase() {
    abstract fun ritualDao(): RitualDao
    abstract fun achievementDao(): AchievementDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `achievement_unlock` (
                `achievementId` TEXT NOT NULL PRIMARY KEY,
                `unlockedAt` INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `user_progress` (
                `id` INTEGER NOT NULL PRIMARY KEY,
                `totalXp` INTEGER NOT NULL DEFAULT 0
            )"""
        )
    }
}
