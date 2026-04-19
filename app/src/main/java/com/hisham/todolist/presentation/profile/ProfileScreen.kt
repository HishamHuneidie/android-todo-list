package com.hisham.todolist.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisham.todolist.domain.model.ThemeMode
import com.hisham.todolist.presentation.components.PlaceholderCard

@Composable
fun ProfileRoute(
    onSignedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isAuthResolved, uiState.isAuthenticated) {
        if (uiState.isAuthResolved && !uiState.isAuthenticated) {
            onSignedOut()
        }
    }

    ProfileScreen(
        uiState = uiState,
        onThemeModeSelected = viewModel::updateThemeMode,
        onSignOut = viewModel::signOut,
    )
}

@Composable
private fun ProfileScreen(
    uiState: ProfileUiState,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Perfil y ajustes",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        PlaceholderCard(
            title = uiState.displayName,
            description = if (uiState.email.isBlank()) {
                "Sesión no disponible."
            } else {
                uiState.email
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Tema",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = uiState.themeMode == mode,
                    onClick = { onThemeModeSelected(mode) },
                    label = {
                        Text(
                            mode.name
                                .lowercase()
                                .replaceFirstChar { firstChar -> firstChar.titlecase() },
                        )
                    },
                )
            }
        }

        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(999.dp),
        ) {
            Text("Cerrar sesión")
        }
    }
}
