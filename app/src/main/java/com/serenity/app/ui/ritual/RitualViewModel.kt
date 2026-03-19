package com.serenity.app.ui.ritual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serenity.app.domain.usecase.CalculateWellnessScoreUseCase
import com.serenity.app.domain.usecase.CompleteRitualUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RitualUiState(
    val currentStep: Int = 0,
    val mood: Int? = null,
    val sleepHours: Float? = 7.0f,
    val waterGlasses: Int? = 0,
    val breathingCompleted: Boolean? = null,
    val gratitudeNote: String? = null,
    val wellnessScore: Int? = null,
    val isCompleting: Boolean = false,
)

@HiltViewModel
class RitualViewModel @Inject constructor(
    private val completeRitualUseCase: CompleteRitualUseCase,
    private val calculateWellnessScoreUseCase: CalculateWellnessScoreUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RitualUiState())
    val uiState: StateFlow<RitualUiState> = _uiState.asStateFlow()

    fun updateMood(mood: Int) {
        _uiState.update { it.copy(mood = mood) }
    }

    fun updateSleep(hours: Float) {
        _uiState.update { it.copy(sleepHours = hours) }
    }

    fun updateWater(glasses: Int) {
        _uiState.update { it.copy(waterGlasses = glasses) }
    }

    fun toggleBreathing(completed: Boolean) {
        _uiState.update { it.copy(breathingCompleted = completed) }
    }

    fun updateGratitude(note: String) {
        _uiState.update { it.copy(gratitudeNote = note) }
    }

    fun nextStep() {
        _uiState.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(5)) }
    }

    fun previousStep() {
        _uiState.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) }
    }

    fun skipStep() {
        val state = _uiState.value
        when (state.currentStep) {
            0 -> _uiState.update { it.copy(mood = null) }
            1 -> _uiState.update { it.copy(sleepHours = null) }
            2 -> _uiState.update { it.copy(waterGlasses = null) }
            3 -> _uiState.update { it.copy(breathingCompleted = null) }
            4 -> _uiState.update { it.copy(gratitudeNote = null) }
        }
        nextStep()
    }

    fun completeRitual() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCompleting = true) }

            val state = _uiState.value
            try {
                completeRitualUseCase(
                    mood = state.mood,
                    sleepHours = state.sleepHours,
                    waterGlasses = state.waterGlasses,
                    breathingCompleted = state.breathingCompleted,
                    gratitudeNote = state.gratitudeNote,
                )

                val score = calculateWellnessScoreUseCase(
                    mood = state.mood,
                    sleepHours = state.sleepHours,
                    waterGlasses = state.waterGlasses,
                    breathingCompleted = state.breathingCompleted,
                    gratitudeNote = state.gratitudeNote,
                )

                _uiState.update {
                    it.copy(
                        wellnessScore = score,
                        isCompleting = false,
                        currentStep = 5,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCompleting = false) }
            }
        }
    }
}
