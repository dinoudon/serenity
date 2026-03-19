package com.serenity.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.serenity.app.domain.model.AppTheme
import com.serenity.app.domain.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val NAME = stringPreferencesKey("name")
        val RITUAL_TIME = stringPreferencesKey("ritual_time")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val THEME = stringPreferencesKey("theme")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            name = prefs[Keys.NAME] ?: "",
            ritualTime = prefs[Keys.RITUAL_TIME]?.let { LocalTime.parse(it) } ?: LocalTime.of(8, 0),
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            theme = prefs[Keys.THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.SAGE
        )
    }

    suspend fun updateName(name: String) {
        context.dataStore.edit { it[Keys.NAME] = name }
    }

    suspend fun updateRitualTime(time: LocalTime) {
        context.dataStore.edit { it[Keys.RITUAL_TIME] = time.toString() }
    }

    suspend fun updateDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun isOnboardingCompleted(): Boolean {
        return context.dataStore.data.first()[Keys.ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = true }
    }
}
