package com.serenity.app.domain.repository

import com.serenity.app.domain.model.AppTheme
import com.serenity.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

interface PreferencesRepository {
    fun getUserPreferences(): Flow<UserPreferences>
    suspend fun updateName(name: String)
    suspend fun updateRitualTime(time: LocalTime)
    suspend fun updateDarkMode(enabled: Boolean)
    suspend fun updateNotificationsEnabled(enabled: Boolean)
    suspend fun updateTheme(theme: AppTheme)
    suspend fun isOnboardingCompleted(): Boolean
    suspend fun setOnboardingCompleted()
}
