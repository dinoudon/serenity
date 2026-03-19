package com.serenity.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.serenity.app.ui.history.HistoryScreen
import com.serenity.app.ui.home.HomeScreen
import com.serenity.app.ui.onboarding.OnboardingScreen
import com.serenity.app.ui.ritual.RitualScreen
import com.serenity.app.ui.settings.SettingsScreen

@Composable
fun SerenityNavGraph(
    navController: NavHostController,
    isOnboardingCompleted: Boolean,
    modifier: Modifier = Modifier,
) {
    val startDestination = if (isOnboardingCompleted) {
        Routes.Home.route
    } else {
        Routes.Onboarding.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Routes.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Home.route) {
            HomeScreen(
                onStartRitual = {
                    navController.navigate(Routes.Ritual.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.History.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.Settings.route)
                },
            )
        }
        composable(Routes.Ritual.route) {
            RitualScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.History.route) {
                        popUpTo(Routes.Home.route)
                    }
                },
            )
        }
        composable(Routes.History.route) {
            HistoryScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(Routes.Settings.route) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = name)
    }
}
