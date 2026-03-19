package com.serenity.app.data.repository

import com.serenity.app.data.local.UserPreferencesDataStore
import com.serenity.app.domain.model.AppTheme
import com.serenity.app.domain.model.UserPreferences
import com.serenity.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) : PreferencesRepository {

    override fun getUserPreferences(): Flow<UserPreferences> {
        return dataStore.userPreferences
    }

    override suspend fun updateName(name: String) {
        dataStore.updateName(name)
    }

    override suspend fun updateRitualTime(time: LocalTime) {
        dataStore.updateRitualTime(time)
    }

    override suspend fun updateDarkMode(enabled: Boolean) {
        dataStore.updateDarkMode(enabled)
    }

    override suspend fun updateNotificationsEnabled(enabled: Boolean) {
        dataStore.updateNotificationsEnabled(enabled)
    }

    override suspend fun updateTheme(theme: AppTheme) {
        dataStore.updateTheme(theme)
    }

    override suspend fun isOnboardingCompleted(): Boolean {
        return dataStore.isOnboardingCompleted()
    }

    override suspend fun setOnboardingCompleted() {
        dataStore.setOnboardingCompleted()
    }
}
