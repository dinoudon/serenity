package com.serenity.app.ui.ritual.steps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun SleepStep(
    sleepHours: Float,
    onSleepChanged: (Float) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "How did you sleep?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "\uD83C\uDF19",
            fontSize = 64.sp,
        )

        Spacer(modifier = Modifier.height(24.dp))

        val displayHours = (sleepHours * 2).roundToInt() / 2f
        Text(
            text = if (displayHours == displayHours.toInt().toFloat()) {
                "${displayHours.toInt()} hours"
            } else {
                "$displayHours hours"
            },
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Slider(
            value = sleepHours,
            onValueChange = { newValue ->
                val snapped = (newValue * 2).roundToInt() / 2f
                onSleepChanged(snapped)
            },
            valueRange = 0f..12f,
            steps = 23,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("Next")
        }

        TextButton(onClick = onSkip) {
            Text("Skip")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
