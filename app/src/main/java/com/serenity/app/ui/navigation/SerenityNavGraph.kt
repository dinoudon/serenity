package com.serenity.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.serenity.app.ui.history.HistoryScreen
import com.serenity.app.ui.home.HomeScreen
import com.serenity.app.ui.onboarding.OnboardingScreen
import com.serenity.app.ui.profile.ProfileScreen
import com.serenity.app.ui.ritual.RitualScreen
import com.serenity.app.ui.settings.SettingsScreen

private data class NavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomNavItems = listOf(
    NavItem(Routes.Home.route,     "Home",    Icons.Default.Home),
    NavItem(Routes.History.route,  "History", Icons.Default.ShowChart),
    NavItem(Routes.Profile.route,  "Profile", Icons.Default.Person),
    NavItem(Routes.Settings.route, "Settings",Icons.Default.Settings),
)

@Composable
fun SerenityNavGraph(
    navController: NavHostController,
    isOnboardingCompleted: Boolean,
    modifier: Modifier = Modifier,
) {
    val startDestination = if (isOnboardingCompleted) Routes.Home.route else Routes.Onboarding.route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomNav = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
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
                    onStartRitual = { navController.navigate(Routes.Ritual.route) },
                    onNavigateToProfile = { navController.navigate(Routes.Profile.route) },
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
                HistoryScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.Profile.route) {
                ProfileScreen()
            }
        }
    }
}
