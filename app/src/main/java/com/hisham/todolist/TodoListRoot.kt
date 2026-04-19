package com.hisham.todolist

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hisham.todolist.core.navigation.AppDestination
import com.hisham.todolist.presentation.login.LoginRoute
import com.hisham.todolist.presentation.loader.LoaderRoute
import com.hisham.todolist.presentation.pending.PendingRoute
import com.hisham.todolist.presentation.profile.ProfileRoute
import com.hisham.todolist.presentation.stats.StatsRoute
import com.hisham.todolist.presentation.taskmanager.TaskManagerRoute

@Composable
fun TodoListRoot() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomBarDestinations = listOf(
        AppDestination.Pending,
        AppDestination.TaskManager,
        AppDestination.Stats,
        AppDestination.Profile,
    )
    val shouldShowBottomBar = currentDestination?.route in bottomBarDestinations.map { it.route }

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar {
                    bottomBarDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination
                                ?.hierarchy
                                ?.any { it.route == destination.route } == true,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = when (destination) {
                                        AppDestination.Pending -> Icons.Outlined.CheckCircle
                                        AppDestination.TaskManager -> Icons.Outlined.EditNote
                                        AppDestination.Stats -> Icons.Outlined.BarChart
                                        AppDestination.Profile -> Icons.Outlined.Person
                                        else -> Icons.Outlined.CheckCircle
                                    },
                                    contentDescription = destination.label,
                                )
                            },
                            label = { androidx.compose.material3.Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Loader.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestination.Loader.route) {
                LoaderRoute(
                    onAuthenticated = {
                        navController.navigate(AppDestination.Pending.route) {
                            popUpTo(AppDestination.Loader.route) { inclusive = true }
                        }
                    },
                    onUnauthenticated = {
                        navController.navigate(AppDestination.Login.route) {
                            popUpTo(AppDestination.Loader.route) { inclusive = true }
                        }
                    },
                )
            }

            composable(AppDestination.Login.route) {
                LoginRoute(
                    onLoginSuccess = {
                        navController.navigate(AppDestination.Pending.route) {
                            popUpTo(AppDestination.Login.route) { inclusive = true }
                        }
                    },
                )
            }

            composable(AppDestination.Pending.route) {
                PendingRoute()
            }

            composable(AppDestination.TaskManager.route) {
                TaskManagerRoute()
            }

            composable(AppDestination.Stats.route) {
                StatsRoute()
            }

            composable(AppDestination.Profile.route) {
                ProfileRoute(
                    onSignedOut = {
                        navController.navigate(AppDestination.Login.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                        }
                    },
                )
            }
        }
    }
}
