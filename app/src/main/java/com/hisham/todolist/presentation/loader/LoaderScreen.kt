package com.hisham.todolist.presentation.loader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoaderRoute(
    onAuthenticated: () -> Unit,
    onUnauthenticated: () -> Unit,
    viewModel: LoaderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var hasNavigated by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.status) {
        if (hasNavigated) return@LaunchedEffect

        when (uiState.status) {
            LoaderStatus.AUTHENTICATED -> {
                hasNavigated = true
                onAuthenticated()
            }
            LoaderStatus.UNAUTHENTICATED -> {
                hasNavigated = true
                onUnauthenticated()
            }
            LoaderStatus.LOADING -> Unit
        }
    }

    LoaderScreen()
}

@Composable
private fun LoaderScreen() {
    val loaderDotColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(260.dp)
                .graphicsLayer { alpha = 0.22f }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(220.dp)
                .graphicsLayer { alpha = 0.18f }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary,
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .size(188.dp)
                .rotate(45f)
                .clip(RoundedCornerShape(42.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        ),
                    ),
                ),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(132.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                            ),
                        ),
                    ),
            )
        }

        Canvas(
            modifier = Modifier.size(64.dp),
        ) {
            drawCircle(
                color = loaderDotColor,
                radius = size.minDimension / 2,
            )
        }
    }
}
