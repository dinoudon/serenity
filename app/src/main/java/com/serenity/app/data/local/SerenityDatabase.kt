package com.serenity.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RitualEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SerenityDatabase : RoomDatabase() {
    abstract fun ritualDao(): RitualDao
}
