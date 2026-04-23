package com.hisham.todolist.presentation.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale

@Composable
fun StatsRoute(
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StatsScreen(uiState = uiState)
}

@Composable
private fun StatsScreen(uiState: StatsUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Estadisticas",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        WeeklyEffectivenessCard(weeklyEffectiveness = uiState.weeklyEffectiveness)

        MetricCard(
            title = "Total mensual",
            value = uiState.monthlyCompletedTasks.toString(),
            suffix = "tareas",
            icon = Icons.Outlined.TaskAlt,
        )

        MetricCard(
            title = "Promedio diario",
            value = String.format(Locale.getDefault(), "%.1f", uiState.dailyAverage),
            suffix = "tareas/dia",
            icon = Icons.Outlined.Analytics,
        )
    }
}

@Composable
private fun WeeklyEffectivenessCard(weeklyEffectiveness: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f),
        shape = RoundedCornerShape(32.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "Efectividad semanal",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
            )

            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center,
            ) {
                WeeklyRing(
                    progress = weeklyEffectiveness / 100f,
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    progressColor = MaterialTheme.colorScheme.primary,
                )

                Column(
                    modifier = Modifier.width(132.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "$weeklyEffectiveness%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            text = "Cumplimiento de la semana actual",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyRing(
    progress: Float,
    trackColor: androidx.compose.ui.graphics.Color,
    progressColor: androidx.compose.ui.graphics.Color,
) {
    Canvas(modifier = Modifier.size(220.dp)) {
        val strokeWidth = 14.dp.toPx()
        val inset = strokeWidth / 2f
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    suffix: String,
    icon: ImageVector,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f),
        shape = RoundedCornerShape(28.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f),
                )
            }

            Surface(
                modifier = Modifier.size(56.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
