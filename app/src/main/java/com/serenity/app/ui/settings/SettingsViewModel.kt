package com.serenity.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serenity.app.domain.model.AppTheme
import com.serenity.app.domain.model.UserPreferences
import com.serenity.app.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class SettingsUiState(
    val name: String = "",
    val ritualTime: LocalTime = LocalTime.of(8, 0),
    val darkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val theme: AppTheme = AppTheme.SAGE,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = preferencesRepository
        .getUserPreferences()
        .map { it.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateDarkMode(enabled)
        }
    }

    fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateNotificationsEnabled(enabled)
        }
    }

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesRepository.updateTheme(theme)
        }
    }

    fun updateRitualTime(time: LocalTime) {
        viewModelScope.launch {
            preferencesRepository.updateRitualTime(time)
        }
    }

    private fun UserPreferences.toUiState() = SettingsUiState(
        name = name,
        ritualTime = ritualTime,
        darkMode = darkMode,
        notificationsEnabled = notificationsEnabled,
        theme = theme,
    )
}
