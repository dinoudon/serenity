package com.serenity.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.serenity.app.domain.model.AppTheme

@Composable
fun SerenityTheme(
    appTheme: AppTheme = AppTheme.SAGE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when (appTheme) {
        AppTheme.SAGE -> if (darkTheme) SageDarkColorScheme else SageLightColorScheme
        AppTheme.LAVENDER -> if (darkTheme) LavenderDarkColorScheme else LavenderLightColorScheme
        AppTheme.SAND -> if (darkTheme) SandDarkColorScheme else SandLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SerenityTypography,
        shapes = SerenityShapes,
        content = content,
    )
}
