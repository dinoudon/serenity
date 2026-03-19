package com.serenity.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serenity.app.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class OnboardingUiState(
    val name: String = "",
    val ritualTime: LocalTime = LocalTime.of(8, 0),
    val currentPage: Int = 0,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateRitualTime(time: LocalTime) {
        _uiState.update { it.copy(ritualTime = time) }
    }

    fun nextPage() {
        _uiState.update { it.copy(currentPage = (it.currentPage + 1).coerceAtMost(2)) }
    }

    fun completeOnboarding(onCompleted: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            preferencesRepository.updateName(state.name)
            preferencesRepository.updateRitualTime(state.ritualTime)
            preferencesRepository.setOnboardingCompleted()
            onCompleted()
        }
    }
}
