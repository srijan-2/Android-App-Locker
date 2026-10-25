package com.antimatter.applocker.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.antimatter.applocker.ui.apps.AppsScreen
import com.antimatter.applocker.ui.dashboard.DashboardScreen
import com.antimatter.applocker.ui.monitoring.MonitoringSetupScreen
import com.antimatter.applocker.ui.pin.PinFlowMode
import com.antimatter.applocker.ui.pin.PinSetupScreen
import com.antimatter.applocker.ui.settings.SettingsScreen
import com.antimatter.applocker.ui.setup.SetupScreen
import com.antimatter.applocker.ui.unlock.UnlockGate

private data class TopTab(
    val route: Route,
    val label: String,
    val icon: ImageVector
)

private val topTabs = listOf(
    TopTab(Route.Protection, "Protection", Icons.Filled.Shield),
    TopTab(Route.Apps, "Apps", Icons.Filled.Apps),
    TopTab(Route.Settings, "Settings", Icons.Filled.Settings)
)

/** Full-screen flows that hide the tab bar. */
private val flowRoutes = setOf(
    Route.PinCreate.path,
    Route.PinChange.path,
    Route.MonitoringSetup.path,
    Route.Setup.path
)

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showBottomBar = currentRoute == null || currentRoute !in flowRoutes

    // Switches to a tab and drops any full-screen flow that was on top of it.
    fun openTab(route: Route) {
        navController.navigate(route.path) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                Column {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        topTabs.forEach { tab ->
                            val selected =
                                currentDestination?.hierarchy?.any { it.route == tab.route.path } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { openTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Protection.path,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Route.Protection.path) {
                DashboardScreen(
                    onSetupPin = { navController.navigate(Route.PinCreate.path) },
                    onChangePin = { navController.navigate(Route.PinChange.path) },
                    onOpenMonitoring = { navController.navigate(Route.MonitoringSetup.path) },
                    onOpenSetup = { navController.navigate(Route.Setup.path) }
                )
            }
            composable(Route.Apps.path) {
                UnlockGate(
                    title = "Unlock Apps",
                    subtitle = "Enter your PIN to manage protected apps."
                ) {
                    AppsScreen()
                }
            }
            composable(Route.Settings.path) {
                UnlockGate(
                    title = "Unlock Settings",
                    subtitle = "Enter your PIN to continue."
                ) {
                    SettingsScreen(onChangePin = { navController.navigate(Route.PinChange.path) })
                }
            }
            composable(Route.PinCreate.path) {
                PinSetupScreen(
                    mode = PinFlowMode.Create,
                    onClose = { navController.popBackStack() }
                )
            }
            composable(Route.PinChange.path) {
                PinSetupScreen(
                    mode = PinFlowMode.Change,
                    onClose = { navController.popBackStack() }
                )
            }
            composable(Route.MonitoringSetup.path) {
                MonitoringSetupScreen(onClose = { navController.popBackStack() })
            }
            composable(Route.Setup.path) {
                SetupScreen(
                    onClose = { navController.popBackStack() },
                    onCreatePin = { navController.navigate(Route.PinCreate.path) },
                    onOpenMonitoring = { navController.navigate(Route.MonitoringSetup.path) },
                    onOpenApps = { openTab(Route.Apps) },
                    onOpenSettings = { openTab(Route.Settings) }
                )
            }
        }
    }
}
