package com.serenity.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievement_unlock")
data class AchievementUnlockEntity(
    @PrimaryKey
    val achievementId: String,
    val unlockedAt: Long // epoch millis
)
