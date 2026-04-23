package com.hisham.todolist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hisham.todolist.core.navigation.AppDestination
import com.hisham.todolist.domain.model.AuthState
import com.hisham.todolist.presentation.AppStateViewModel
import com.hisham.todolist.presentation.loader.LoaderRoute
import com.hisham.todolist.presentation.login.LoginRoute
import com.hisham.todolist.presentation.pending.PendingRoute
import com.hisham.todolist.presentation.profile.ProfileRoute
import com.hisham.todolist.presentation.stats.StatsRoute
import com.hisham.todolist.presentation.taskmanager.TaskManagerRoute

@Composable
fun TodoListRoot(
    appStateViewModel: AppStateViewModel,
) {
    val navController = rememberNavController()
    val appUiState by appStateViewModel.uiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val bottomBarDestinations = listOf(
        AppDestination.Pending,
        AppDestination.TaskManager,
        AppDestination.Stats,
        AppDestination.Profile,
    )
    val protectedRoutes = bottomBarDestinations.map { it.route }.toSet()
    val publicRoutes = setOf(
        AppDestination.Loader.route,
        AppDestination.Login.route,
    )

    val shouldShowBottomBar = appUiState.isAuthenticated &&
            appUiState.isBootstrapped &&
            currentRoute in protectedRoutes

    LaunchedEffect(appUiState.isBootstrapped, appUiState.authState, currentRoute) {
        if (!appUiState.isBootstrapped) {
            return@LaunchedEffect
        }

        when (appUiState.authState) {
            is AuthState.Authenticated -> {
                if (currentRoute == null || currentRoute in publicRoutes) {
                    navController.navigate(AppDestination.Pending.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }

            AuthState.Unauthenticated -> {
                if (currentRoute == null || currentRoute in protectedRoutes || currentRoute == AppDestination.Loader.route) {
                    navController.navigate(AppDestination.Login.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            NavHost(
                navController = navController,
                startDestination = AppDestination.Loader.route,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(AppDestination.Loader.route) {
                    LoaderRoute()
                }

                composable(AppDestination.Login.route) {
                    LoginRoute(onLoginSuccess = {})
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
                    ProfileRoute(onSignedOut = {})
                }
            }

            if (appUiState.loadingState.isMutatingTasks) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                )
            }

            if (appUiState.loadingState.showBlockingOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
