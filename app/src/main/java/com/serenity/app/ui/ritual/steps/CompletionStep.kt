package com.serenity.app.ui.ritual.steps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun CompletionStep(
    wellnessScore: Int,
    onSeeWeek: () -> Unit,
    onDone: () -> Unit,
) {
    var targetProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(wellnessScore) {
        targetProgress = wellnessScore / 100f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1200),
        label = "score_progress",
    )

    val animatedScore = (animatedProgress * 100).toInt()

    val message = when {
        wellnessScore >= 80 -> "Amazing! You're thriving! \uD83C\uDF1F"
        wellnessScore >= 60 -> "Great job taking care of yourself! \uD83D\uDCAA"
        wellnessScore >= 40 -> "Every step counts. Keep going! \uD83C\uDF31"
        else -> "Thanks for checking in. Tomorrow is a new day! \uD83C\uDF05"
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        Text(
            text = "Ritual Complete",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(48.dp))

        androidx.compose.foundation.layout.Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp),
        ) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(180.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 12.dp,
            )
            Text(
                text = "$animatedScore",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onSeeWeek,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("See Your Week")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("Done")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
