package com.hisham.todolist.presentation.pending

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisham.todolist.domain.model.TaskCategory
import kotlinx.coroutines.launch

@Composable
fun PendingRoute(
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TasksScreen(
        uiState = uiState,
        onQuickAddTextChange = viewModel::onQuickAddTextChange,
        onQuickAddSubmit = viewModel::onQuickAddSubmit,
        onToggleTask = viewModel::onToggleTask,
        onReorderStart = viewModel::onDragStart,
        onReorderPreview = viewModel::onReorderPreview,
        onReorderCommit = viewModel::onReorderCommit,
        onReorderCancel = viewModel::onReorderCancel,
        onDismissError = viewModel::onDismissError,
    )
}

@Composable
private fun TasksScreen(
    uiState: TasksUiState,
    onQuickAddTextChange: (String) -> Unit,
    onQuickAddSubmit: () -> Unit,
    onToggleTask: (Long, Boolean) -> Unit,
    onReorderStart: (Long) -> Unit,
    onReorderPreview: (Int, Int) -> Unit,
    onReorderCommit: (List<Long>) -> Unit,
    onReorderCancel: () -> Unit,
    onDismissError: () -> Unit,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentTaskOrder by rememberUpdatedState(uiState.tasks.map(TaskListItemUiModel::id))
    var draggedOffsetY by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PendingHeader()

        uiState.errorMessage?.let { message ->
            ErrorCard(
                message = message,
                onDismiss = onDismissError,
            )
        }

        BoxWithConstraints(
            modifier = Modifier.weight(1f),
        ) {
            val itemCorner = 28.dp
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (uiState.tasks.isEmpty()) {
                    item {
                        EmptyTasksCard()
                    }
                }

                itemsIndexed(
                    items = uiState.tasks,
                    key = { _, task -> task.id },
                ) { index, task ->
                    TaskRow(
                        task = task,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                if (uiState.draggingTaskId == task.id) {
                                    translationY = draggedOffsetY
                                }
                            }
                            .then(
                                if (uiState.draggingTaskId == task.id) {
                                    Modifier.shadow(
                                        elevation = 16.dp,
                                        shape = RoundedCornerShape(itemCorner),
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                        onToggleTask = onToggleTask,
                        dragHandleModifier = Modifier.pointerInput(task.id, uiState.tasks.size) {
                            var currentIndex = index
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedOffsetY = 0f
                                    onReorderStart(task.id)
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    draggedOffsetY += dragAmount.y
                                    val currentItem = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.index == currentIndex }
                                        ?: return@detectDragGesturesAfterLongPress
                                    val itemMidPoint =
                                        currentItem.offset + draggedOffsetY + (currentItem.size / 2f)
                                    val targetItem = listState.layoutInfo.visibleItemsInfo
                                        .filter { it.index != currentIndex }
                                        .firstOrNull { itemMidPoint.toInt() in it.offset..(it.offset + it.size) }

                                    if (targetItem != null) {
                                        onReorderPreview(currentIndex, targetItem.index)
                                        draggedOffsetY += currentItem.offset - targetItem.offset
                                        currentIndex = targetItem.index
                                    }

                                    val currentTop = currentItem.offset + draggedOffsetY
                                    val currentBottom = currentTop + currentItem.size
                                    val overscroll = when {
                                        currentTop < listState.layoutInfo.viewportStartOffset ->
                                            currentTop - listState.layoutInfo.viewportStartOffset

                                        currentBottom > listState.layoutInfo.viewportEndOffset ->
                                            currentBottom - listState.layoutInfo.viewportEndOffset

                                        else -> 0f
                                    }

                                    if (overscroll != 0f) {
                                        coroutineScope.launch {
                                            listState.scrollBy(overscroll)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggedOffsetY = 0f
                                    onReorderCommit(currentTaskOrder)
                                },
                                onDragCancel = {
                                    draggedOffsetY = 0f
                                    onReorderCancel()
                                },
                            )
                        },
                    )
                }
            }
        }

        QuickAddComposer(
            text = uiState.quickAddText,
            isSubmitting = uiState.isSubmittingQuickTask,
            onTextChange = onQuickAddTextChange,
            onSubmit = onQuickAddSubmit,
        )
    }
}

@Composable
private fun PendingHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Text(
                text = "Pendientes",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.84f),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onError,
            )
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onError,
            )
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    }
}

@Composable
private fun EmptyTasksCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "No hay tareas pendientes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Crea una nueva tarea desde la parte inferior para empezar a llenar tu lista.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskListItemUiModel,
    modifier: Modifier = Modifier,
    onToggleTask: (Long, Boolean) -> Unit,
    dragHandleModifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TaskCheckbox(
                    isChecked = task.isCompleted,
                    onCheckedChange = { checked ->
                        onToggleTask(task.id, checked)
                    },
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = task.title,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        if (task.isRecurrent) {
                            Icon(
                                imageVector = Icons.Outlined.Repeat,
                                contentDescription = "Tarea recurrente",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                task.trailingIcon()?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Icon(
                    imageVector = Icons.Outlined.DragIndicator,
                    contentDescription = "Reordenar tarea",
                    modifier = dragHandleModifier,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                )
            }

            if (task.progress in 1..99) {
                Column(
                    modifier = Modifier.padding(start = 48.dp, end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LinearProgressIndicator(
                        progress = { task.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Black.copy(alpha = 0.42f),
                    )
                    Text(
                        text = "${task.progress}% completado",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskCheckbox(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    IconButton(
        onClick = { onCheckedChange(!isChecked) },
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.32f))
            .border(
                width = 2.dp,
                color = if (isChecked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)
                },
                shape = RoundedCornerShape(13.dp),
            ),
    ) {
        if (isChecked) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun QuickAddComposer(
    text: String,
    isSubmitting: Boolean,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(28.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )

            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Añadir una nueva tarea...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )

            Button(
                onClick = onSubmit,
                enabled = !isSubmitting,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = "Crear",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private fun TaskListItemUiModel.trailingIcon() = when {
    iconName?.contains("mail", ignoreCase = true) == true -> Icons.Outlined.MailOutline
    iconName?.contains("analytic", ignoreCase = true) == true -> Icons.Outlined.Analytics
    iconName?.contains("chart", ignoreCase = true) == true -> Icons.Outlined.Analytics
    iconName?.contains("database", ignoreCase = true) == true -> Icons.Outlined.Storage
    iconName?.contains("backup", ignoreCase = true) == true -> Icons.Outlined.Storage
    category == TaskCategory.WORK -> Icons.Outlined.WorkOutline
    category == TaskCategory.HOME -> Icons.Outlined.Home
    category == TaskCategory.HEALTH -> Icons.Outlined.TaskAlt
    category == TaskCategory.HOBBIES -> Icons.Outlined.TaskAlt
    category == TaskCategory.PERSONAL -> Icons.Outlined.Person
    else -> null
}
