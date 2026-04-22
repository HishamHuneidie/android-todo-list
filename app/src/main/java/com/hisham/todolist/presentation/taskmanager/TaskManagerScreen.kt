package com.hisham.todolist.presentation.taskmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisham.todolist.domain.model.TaskCategory
import java.time.DayOfWeek

@Composable
fun TaskManagerRoute(
    viewModel: TaskManagementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TaskManagementScreen(
        uiState = uiState,
        onQuickCreateTextChange = viewModel::onQuickCreateTextChange,
        onQuickCreateSubmit = viewModel::onQuickCreateSubmit,
        onCreateTaskClick = viewModel::onCreateTaskClick,
        onTaskClick = viewModel::onTaskClick,
        onDismissError = viewModel::onDismissError,
        onSheetDismiss = viewModel::onSheetDismiss,
        onTitleChange = viewModel::onTitleChange,
        onIconSelected = viewModel::onIconSelected,
        onCategorySelected = viewModel::onCategorySelected,
        onRecurrenceToggle = viewModel::onRecurrenceToggle,
        onRecurrenceDayToggle = viewModel::onRecurrenceDayToggle,
        onProgressToggle = viewModel::onProgressToggle,
        onProgressValueChange = viewModel::onProgressValueChange,
        onProgressTextChange = viewModel::onProgressTextChange,
        onSaveClick = viewModel::onSaveClick,
        onDeleteClick = viewModel::onDeleteClick,
    )
}

@Composable
private fun TaskManagementScreen(
    uiState: TaskManagementUiState,
    onQuickCreateTextChange: (String) -> Unit,
    onQuickCreateSubmit: () -> Unit,
    onCreateTaskClick: () -> Unit,
    onTaskClick: (Long) -> Unit,
    onDismissError: () -> Unit,
    onSheetDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onIconSelected: (TaskIconOption) -> Unit,
    onCategorySelected: (TaskCategory) -> Unit,
    onRecurrenceToggle: (Boolean) -> Unit,
    onRecurrenceDayToggle: (DayOfWeek) -> Unit,
    onProgressToggle: (Boolean) -> Unit,
    onProgressValueChange: (Int) -> Unit,
    onProgressTextChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    var completedExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TaskManagerHeader(onCreateTaskClick = onCreateTaskClick)

        uiState.errorMessage?.let { message ->
            TaskManagerErrorCard(
                message = message,
                onDismiss = onDismissError,
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (uiState.tasks.isEmpty() && uiState.completedTasks.isEmpty()) {
                item {
                    TaskManagerEmptyState()
                }
            } else {
                items(
                    items = uiState.tasks,
                    key = TaskManagementListItemUiModel::id,
                ) { task ->
                    TaskManagementListRow(
                        task = task,
                        onClick = { onTaskClick(task.id) },
                    )
                }

                if (uiState.completedTasks.isNotEmpty()) {
                    item {
                        TaskManagerSectionHeader(
                            title = "Finalizadas",
                            count = uiState.completedTasks.size,
                            isExpanded = completedExpanded,
                            onToggle = { completedExpanded = !completedExpanded },
                        )
                    }
                }

                if (completedExpanded) {
                    items(
                        items = uiState.completedTasks,
                        key = TaskManagementListItemUiModel::id,
                    ) { task ->
                        TaskManagementListRow(
                            task = task,
                            onClick = { onTaskClick(task.id) },
                        )
                    }
                }
            }
        }

        QuickCreateComposer(
            text = uiState.quickCreateText,
            isSubmitting = uiState.isSubmittingQuickCreate,
            onTextChange = onQuickCreateTextChange,
            onSubmit = onQuickCreateSubmit,
        )
    }

    if (uiState.isSheetVisible) {
        TaskEditorSheet(
            uiState = uiState,
            onDismiss = onSheetDismiss,
            onTitleChange = onTitleChange,
            onIconSelected = onIconSelected,
            onCategorySelected = onCategorySelected,
            onRecurrenceToggle = onRecurrenceToggle,
            onRecurrenceDayToggle = onRecurrenceDayToggle,
            onProgressToggle = onProgressToggle,
            onProgressValueChange = onProgressValueChange,
            onProgressTextChange = onProgressTextChange,
            onSaveClick = onSaveClick,
            onDeleteClick = onDeleteClick,
        )
    }
}

@Composable
private fun TaskManagerSectionHeader(
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
            .padding(horizontal = 4.dp, vertical = 6.dp),
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
private fun TaskManagerHeader(onCreateTaskClick: () -> Unit) {
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
                text = "Administracion",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onCreateTaskClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Crear tarea",
                    tint = MaterialTheme.colorScheme.onPrimary,
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
}

@Composable
private fun TaskManagerErrorCard(
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
private fun TaskManagerEmptyState() {
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
                text = "No hay tareas para administrar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Usa el boton + o la creacion rapida inferior para anadir nuevas tareas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TaskManagementListRow(
    task: TaskManagementListItemUiModel,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .alpha(if (task.isCompleted) 0.66f else 1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.36f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = task.managementIcon(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (task.isCompleted) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = task.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    val subtitle = buildString {
                        append(task.category.label())
                        if (task.isRecurrent) {
                            append(" | Recurrente")
                        }
                        if (task.isCompleted) {
                            append(" | Completada")
                        }
                    }

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                    )
                }

                if (task.isProgressEnabled && !task.isCompleted) {
                    Text(
                        text = "${task.progress}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (task.isProgressEnabled && !task.isCompleted) {
                LinearProgressIndicator(
                    progress = { task.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Black.copy(alpha = 0.35f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TaskEditorSheet(
    uiState: TaskManagementUiState,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onIconSelected: (TaskIconOption) -> Unit,
    onCategorySelected: (TaskCategory) -> Unit,
    onRecurrenceToggle: (Boolean) -> Unit,
    onRecurrenceDayToggle: (DayOfWeek) -> Unit,
    onProgressToggle: (Boolean) -> Unit,
    onProgressValueChange: (Int) -> Unit,
    onProgressTextChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val formState = uiState.formState
    val isEditMode = uiState.sheetMode is TaskSheetMode.Edit
    var categoryExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isEditMode) {
                    IconButton(
                        onClick = onDeleteClick,
                        enabled = !uiState.isDeleting && !uiState.isSaving,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.24f)),
                    ) {
                        if (uiState.isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Eliminar tarea",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(44.dp))
                }

                Text(
                    text = if (isEditMode) "Editar tarea" else "Nueva tarea",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Button(
                    onClick = onSaveClick,
                    enabled = !uiState.isSaving && !uiState.isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(999.dp),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(if (isEditMode) "Guardar" else "Crear")
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    text = "Icono",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TaskIconOption.entries.forEach { option ->
                        FilterChip(
                            selected = formState.selectedIcon == option,
                            onClick = { onIconSelected(option) },
                            label = { Text(option.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = option.icon(),
                                    contentDescription = null,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.2f
                                ),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = formState.title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Titulo") },
                isError = formState.titleError != null,
                supportingText = {
                    formState.titleError?.let { error ->
                        Text(error)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(),
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Recurrente",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Activa la tarea en dias concretos de la semana",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = formState.isRecurrent,
                    onCheckedChange = onRecurrenceToggle,
                )
            }

            if (formState.isRecurrent) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        dayLabels.forEach { (day, label) ->
                            FilterChip(
                                selected = day in formState.recurrenceDays,
                                onClick = { onRecurrenceDayToggle(day) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.22f
                                    ),
                                ),
                            )
                        }
                    }
                    formState.recurrenceError?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded },
            ) {
                OutlinedTextField(
                    value = formState.category.label(),
                    onValueChange = {},
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    label = { Text("Categoria") },
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                    },
                )

                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                ) {
                    TaskCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.label()) },
                            onClick = {
                                onCategorySelected(category)
                                categoryExpanded = false
                            },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Progreso",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Activa barra y porcentaje para esta tarea",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = formState.isProgressEnabled,
                    onCheckedChange = onProgressToggle,
                )
            }

            if (formState.isProgressEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Logro",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        OutlinedTextField(
                            value = formState.progressText,
                            onValueChange = onProgressTextChange,
                            modifier = Modifier.width(100.dp),
                            label = { Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = formState.progressError != null,
                        )
                    }

                    androidx.compose.material3.Slider(
                        value = formState.progressValue.toFloat(),
                        onValueChange = { onProgressValueChange(it.toInt()) },
                        valueRange = 0f..100f,
                    )

                    LinearProgressIndicator(
                        progress = { formState.progressValue / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Black.copy(alpha = 0.35f),
                    )

                    formState.progressError?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun QuickCreateComposer(
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
                        text = "Anadir una nueva tarea...",
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

private val dayLabels = listOf(
    DayOfWeek.MONDAY to "L",
    DayOfWeek.TUESDAY to "M",
    DayOfWeek.WEDNESDAY to "X",
    DayOfWeek.THURSDAY to "J",
    DayOfWeek.FRIDAY to "V",
    DayOfWeek.SATURDAY to "S",
    DayOfWeek.SUNDAY to "D",
)

private fun TaskCategory?.label(): String = when (this) {
    TaskCategory.WORK -> "Trabajo"
    TaskCategory.HOME -> "Hogar"
    TaskCategory.HEALTH -> "Salud"
    TaskCategory.HOBBIES -> "Hobbies"
    TaskCategory.PERSONAL, null -> "Personal"
}

private fun TaskManagementListItemUiModel.managementIcon() = TaskIconOption
    .fromIconName(iconName)
    .icon()

private fun TaskIconOption.icon() = when (this) {
    TaskIconOption.TASKS -> Icons.Outlined.TaskAlt
    TaskIconOption.WORK -> Icons.Outlined.WorkOutline
    TaskIconOption.HOME -> Icons.Outlined.Home
    TaskIconOption.HEALTH -> Icons.Outlined.Favorite
    TaskIconOption.STUDY -> Icons.Outlined.MenuBook
    TaskIconOption.MAIL -> Icons.Outlined.MailOutline
    TaskIconOption.STORAGE -> Icons.Outlined.Storage
    TaskIconOption.PALETTE -> Icons.Outlined.Palette
    TaskIconOption.OTHERS -> Icons.Outlined.MoreHoriz
}
