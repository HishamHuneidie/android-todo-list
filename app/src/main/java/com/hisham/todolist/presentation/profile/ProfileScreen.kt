package com.hisham.todolist.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisham.todolist.domain.model.ThemeMode

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
    val effectiveThemeMode = when (uiState.themeMode) {
        ThemeMode.DARK -> ThemeMode.DARK
        ThemeMode.LIGHT -> ThemeMode.LIGHT
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) ThemeMode.DARK else ThemeMode.LIGHT
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Perfil y ajustes",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        ProfileIdentityCard(uiState = uiState)

        ThemeCard(
            selectedMode = effectiveThemeMode,
            onThemeModeSelected = onThemeModeSelected,
        )

        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            shape = RoundedCornerShape(999.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Cerrar sesion",
                modifier = Modifier.padding(start = 10.dp, top = 2.dp, bottom = 2.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ProfileIdentityCard(uiState: ProfileUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f),
        shape = RoundedCornerShape(32.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                if (uiState.displayName.isNotBlank() && uiState.displayName != "Invitado") {
                    Text(
                        text = uiState.displayName
                            .split(" ")
                            .filter(String::isNotBlank)
                            .take(2)
                            .joinToString(separator = "") { token ->
                                token.first().uppercase()
                            },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = uiState.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (uiState.email.isBlank()) "Sesion no disponible." else uiState.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(
    selectedMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Apariencia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = "Modo claro / oscuro",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ThemeOption(
                    modifier = Modifier.weight(1f),
                    title = "Claro",
                    icon = Icons.Outlined.LightMode,
                    isSelected = selectedMode == ThemeMode.LIGHT,
                    onClick = { onThemeModeSelected(ThemeMode.LIGHT) },
                )
                ThemeOption(
                    modifier = Modifier.weight(1f),
                    title = "Oscuro",
                    icon = Icons.Outlined.DarkMode,
                    isSelected = selectedMode == ThemeMode.DARK,
                    onClick = { onThemeModeSelected(ThemeMode.DARK) },
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.46f)
        },
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (isSelected) {
                    Text(
                        text = "Activo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
