package com.hisham.todolist.presentation.pending

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
    val currentActiveTaskCount by rememberUpdatedState(uiState.tasks.size)
    var draggedOffsetY by remember { mutableFloatStateOf(0f) }
    var completedExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PendingHeader(taskCount = uiState.tasks.size)

        uiState.errorMessage?.let { message ->
            ErrorCard(
                message = message,
                onDismiss = onDismissError,
            )
        }

        BoxWithConstraints(
            modifier = Modifier.weight(1f),
        ) {
            val itemCorner = 18.dp
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (uiState.tasks.isEmpty() && uiState.completedTasks.isEmpty()) {
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
                                        .filter { it.index != currentIndex && it.index < currentActiveTaskCount }
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

                if (uiState.completedTasks.isNotEmpty()) {
                    item {
                        CollapsibleSectionHeader(
                            title = "Finalizadas",
                            count = uiState.completedTasks.size,
                            isExpanded = completedExpanded,
                            onToggle = { completedExpanded = !completedExpanded },
                        )
                    }

                    if (completedExpanded) {
                        items(
                            items = uiState.completedTasks,
                            key = TaskListItemUiModel::id,
                        ) { task ->
                            TaskRow(
                                task = task,
                                modifier = Modifier.fillMaxWidth(),
                                onToggleTask = onToggleTask,
                                showDragHandle = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingHeader(taskCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "Pendientes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "$taskCount tareas activas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.06f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(999.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Hoy",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "No hay tareas pendientes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Las tareas recurrentes del dia tambien apareceran aqui cuando corresponda.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            )
        }
    }
}

@Composable
private fun CollapsibleSectionHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun TaskRow(
    task: TaskListItemUiModel,
    modifier: Modifier = Modifier,
    onToggleTask: (Long, Boolean) -> Unit,
    dragHandleModifier: Modifier = Modifier,
    showDragHandle: Boolean = true,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (task.isCompleted) 0.62f else 1f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        top = 12.dp,
                        end = 12.dp,
                        bottom = if (task.isProgressEnabled && !task.isCompleted) 8.dp else 12.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TaskCheckbox(
                    isChecked = task.isCompleted,
                    onCheckedChange = { checked ->
                        onToggleTask(task.id, checked)
                    },
                )

                Spacer(modifier = Modifier.width(10.dp))

                task.leadingIcon()?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Text(
                    text = task.title,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.96f),
                )

                if (showDragHandle) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.DragIndicator,
                        contentDescription = "Reordenar tarea",
                        modifier = dragHandleModifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
                    )
                }
            }

            if (task.isProgressEnabled && !task.isCompleted) {
                LinearProgressIndicator(
                    progress = { task.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                )
            }
        }
    }
}

@Composable
private fun TaskCheckbox(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isChecked) MaterialTheme.colorScheme.primary
                else Color.Transparent,
            )
            .border(
                width = 1.5.dp,
                color = if (isChecked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f)
                },
                shape = RoundedCornerShape(6.dp),
            )
            .clickable { onCheckedChange(!isChecked) },
        contentAlignment = Alignment.Center,
    ) {
        if (isChecked) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

private fun TaskListItemUiModel.leadingIcon() = when {
    iconName?.contains("task_alt", ignoreCase = true) == true -> Icons.Outlined.TaskAlt
    iconName?.contains("work_outline", ignoreCase = true) == true -> Icons.Outlined.WorkOutline
    iconName?.contains("home", ignoreCase = true) == true -> Icons.Outlined.Home
    iconName?.contains("favorite", ignoreCase = true) == true -> Icons.Outlined.Favorite
    iconName?.contains("menu_book", ignoreCase = true) == true -> Icons.Outlined.MenuBook
    iconName?.contains("mail", ignoreCase = true) == true -> Icons.Outlined.MailOutline
    iconName?.contains("analytic", ignoreCase = true) == true -> Icons.Outlined.Analytics
    iconName?.contains("chart", ignoreCase = true) == true -> Icons.Outlined.Analytics
    iconName?.contains("database", ignoreCase = true) == true -> Icons.Outlined.Storage
    iconName?.contains("backup", ignoreCase = true) == true -> Icons.Outlined.Storage
    iconName?.contains("palette", ignoreCase = true) == true -> Icons.Outlined.Palette
    iconName?.contains("more_horiz", ignoreCase = true) == true -> Icons.Outlined.MoreHoriz
    category == TaskCategory.WORK -> Icons.Outlined.WorkOutline
    category == TaskCategory.HOME -> Icons.Outlined.Home
    category == TaskCategory.HEALTH -> Icons.Outlined.TaskAlt
    category == TaskCategory.HOBBIES -> Icons.Outlined.TaskAlt
    category == TaskCategory.PERSONAL -> Icons.Outlined.Person
    isRecurrent -> Icons.Outlined.TaskAlt
    else -> null
}
