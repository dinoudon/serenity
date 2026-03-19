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
            PlaceholderScreen("Onboarding")
        }
        composable(Routes.Home.route) {
            PlaceholderScreen("Home")
        }
        composable(Routes.Ritual.route) {
            PlaceholderScreen("Ritual")
        }
        composable(Routes.History.route) {
            PlaceholderScreen("History")
        }
        composable(Routes.Settings.route) {
            PlaceholderScreen("Settings")
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
