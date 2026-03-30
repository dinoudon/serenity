package com.serenity.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey
    val id: Int = 1, // single-row table
    val totalXp: Int = 0
)
