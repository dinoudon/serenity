package com.serenity.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serenity.app.domain.model.AppTheme
import com.serenity.app.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val theme: AppTheme = AppTheme.SAGE,
    val darkMode: Boolean = false,
    val isOnboardingCompleted: Boolean = false,
    val isLoading: Boolean = true,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _isOnboardingCompleted = MutableStateFlow(false)

    val uiState: StateFlow<MainUiState> = combine(
        preferencesRepository.getUserPreferences(),
        _isOnboardingCompleted,
    ) { prefs, onboardingCompleted ->
        MainUiState(
            theme = prefs.theme,
            darkMode = prefs.darkMode,
            isOnboardingCompleted = onboardingCompleted,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    init {
        viewModelScope.launch {
            _isOnboardingCompleted.value = preferencesRepository.isOnboardingCompleted()
        }
    }
}
