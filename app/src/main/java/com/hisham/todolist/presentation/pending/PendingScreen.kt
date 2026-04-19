package com.hisham.todolist.presentation.pending

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisham.todolist.presentation.components.PlaceholderCard

@Composable
fun PendingRoute(
    viewModel: PendingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Pendientes",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        PlaceholderCard(
            title = "Base funcional lista",
            description = "Room, Hilt y Navigation ya están conectados. El repositorio de tareas devuelve ${uiState.taskCount} tareas registradas.",
        )

        PlaceholderCard(
            title = "Siguiente capa",
            description = "La implementación detallada de la lista unificada, creación rápida y reordenación se deja para el ticket específico de tareas.",
        )
    }
}
