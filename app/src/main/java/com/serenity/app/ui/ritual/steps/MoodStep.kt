package com.serenity.app.ui.ritual.steps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class MoodOption(
    val value: Int,
    val emoji: String,
    val label: String,
)

private val moodOptions = listOf(
    MoodOption(1, "\uD83D\uDE22", "Awful"),
    MoodOption(2, "\uD83D\uDE15", "Bad"),
    MoodOption(3, "\uD83D\uDE10", "Okay"),
    MoodOption(4, "\uD83D\uDE0A", "Good"),
    MoodOption(5, "\uD83D\uDE04", "Great"),
)

@Composable
fun MoodStep(
    selectedMood: Int?,
    onMoodSelected: (Int) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "How are you feeling?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            moodOptions.forEach { option ->
                val isSelected = selectedMood == option.value
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.2f else 1.0f,
                    animationSpec = tween(200),
                    label = "mood_scale_${option.value}",
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        )
                        .clickable { onMoodSelected(option.value) }
                        .padding(8.dp)
                        .scale(scale),
                ) {
                    Text(
                        text = option.emoji,
                        fontSize = 32.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedMood != null,
        ) {
            Text("Next")
        }

        TextButton(onClick = onSkip) {
            Text("Skip")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
