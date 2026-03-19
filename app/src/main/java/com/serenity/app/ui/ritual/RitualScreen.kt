package com.serenity.app.ui.ritual

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serenity.app.ui.ritual.steps.BreathingStep
import com.serenity.app.ui.ritual.steps.CompletionStep
import com.serenity.app.ui.ritual.steps.GratitudeStep
import com.serenity.app.ui.ritual.steps.HydrationStep
import com.serenity.app.ui.ritual.steps.MoodStep
import com.serenity.app.ui.ritual.steps.SleepStep

@Composable
fun RitualScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: RitualViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.currentStep > 0 && uiState.currentStep < 5) {
        viewModel.previousStep()
    }

    BackHandler(enabled = uiState.currentStep == 0) {
        onNavigateToHome()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        if (uiState.currentStep < 5) {
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { (uiState.currentStep + 1) / 5f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        AnimatedContent(
            targetState = uiState.currentStep,
            transitionSpec = {
                fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(300),
                ) togetherWith fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(300),
                )
            },
            label = "ritual_step",
        ) { step ->
            when (step) {
                0 -> MoodStep(
                    selectedMood = uiState.mood,
                    onMoodSelected = viewModel::updateMood,
                    onNext = viewModel::nextStep,
                    onSkip = viewModel::skipStep,
                )
                1 -> SleepStep(
                    sleepHours = uiState.sleepHours ?: 7.0f,
                    onSleepChanged = viewModel::updateSleep,
                    onNext = viewModel::nextStep,
                    onSkip = viewModel::skipStep,
                )
                2 -> HydrationStep(
                    waterGlasses = uiState.waterGlasses ?: 0,
                    onWaterChanged = viewModel::updateWater,
                    onNext = viewModel::nextStep,
                    onSkip = viewModel::skipStep,
                )
                3 -> BreathingStep(
                    onComplete = {
                        viewModel.toggleBreathing(true)
                        viewModel.nextStep()
                    },
                    onSkip = viewModel::skipStep,
                )
                4 -> GratitudeStep(
                    gratitudeNote = uiState.gratitudeNote ?: "",
                    onGratitudeChanged = viewModel::updateGratitude,
                    onComplete = viewModel::completeRitual,
                    onSkip = {
                        viewModel.updateGratitude("")
                        viewModel.completeRitual()
                    },
                    isCompleting = uiState.isCompleting,
                )
                5 -> CompletionStep(
                    wellnessScore = uiState.wellnessScore ?: 0,
                    onSeeWeek = onNavigateToHistory,
                    onDone = onNavigateToHome,
                )
            }
        }
    }
}
