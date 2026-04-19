package com.hisham.todolist.core.navigation

sealed class AppDestination(
    val route: String,
    val label: String,
) {
    data object Loader : AppDestination("loader", "Loader")
    data object Login : AppDestination("login", "Login")
    data object Pending : AppDestination("pending", "Pendientes")
    data object TaskManager : AppDestination("task_manager", "Gestionar")
    data object Stats : AppDestination("stats", "Estadísticas")
    data object Profile : AppDestination("profile", "Perfil")
}
