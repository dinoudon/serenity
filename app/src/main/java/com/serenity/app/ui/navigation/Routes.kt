package com.serenity.app.ui.navigation

sealed class Routes(val route: String) {
    data object Onboarding : Routes("onboarding")
    data object Home : Routes("home")
    data object Ritual : Routes("ritual")
    data object History : Routes("history")
    data object Settings : Routes("settings")
}
