package com.example.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AppManagerScreen
import com.example.ui.screens.AppUsageScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FileSystemScreen
import com.example.ui.screens.LiveResourcesScreen
import com.example.ui.screens.HardwareScreen
import com.example.ui.screens.NetworkScreen
import com.example.ui.screens.SensorsScreen
import com.example.ui.screens.SettingsScreen
import com.example.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Hardware : Screen("hardware", "Hardware", Icons.Default.Hardware)
    object Sensors : Screen("sensors", "Sensors", Icons.Default.Sensors)
    object AppManager : Screen("apps", "Apps", Icons.Default.Apps)
    object FileSystem : Screen("files", "Files", Icons.Default.Folder)
    object Network : Screen("network", "Network", Icons.Default.Public)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object AppUsage : Screen("app_usage", "App Usage", Icons.Default.Apps)
    object LiveResources : Screen("live_resources", "Live Resources", Icons.Default.DeveloperBoard)
}

val items = listOf(
    Screen.Dashboard,
    Screen.Hardware,
    Screen.Sensors,
    Screen.AppManager,
    Screen.FileSystem,
    Screen.Network,
    Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceMonitorApp(settingsViewModel: SettingsViewModel = viewModel()) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.padding(16.dp))
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp))
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val currentScreenTitle = items.find { it.route == currentRoute }?.title ?: "Device Monitor"
                TopAppBar(
                    title = { Text(currentScreenTitle) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(navController, startDestination = Screen.Dashboard.route, Modifier.padding(innerPadding)) {
                composable(Screen.Dashboard.route) { 
                    DashboardScreen(
                        settingsViewModel = settingsViewModel,
                        onNavigateToApps = {
                            navController.navigate(Screen.AppManager.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToAppUsage = {
                            navController.navigate(Screen.AppUsage.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToLiveResources = {
                            navController.navigate(Screen.LiveResources.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    ) 
                }
                composable(Screen.Hardware.route) { HardwareScreen() }
                composable(Screen.Sensors.route) { SensorsScreen() }
                composable(Screen.AppManager.route) { AppManagerScreen() }
                composable(Screen.FileSystem.route) { FileSystemScreen() }
                composable(Screen.Network.route) { NetworkScreen() }
                composable(Screen.Settings.route) { SettingsScreen(viewModel = settingsViewModel) }
                composable(Screen.AppUsage.route) { 
                    AppUsageScreen(onBack = { navController.popBackStack() }) 
                }
                composable(Screen.LiveResources.route) {
                    LiveResourcesScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

