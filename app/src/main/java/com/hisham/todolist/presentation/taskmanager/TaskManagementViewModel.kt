package com.hisham.todolist.presentation.taskmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.model.TaskCategory
import com.hisham.todolist.domain.usecase.CreateQuickTaskUseCase
import com.hisham.todolist.domain.usecase.CreateTaskUseCase
import com.hisham.todolist.domain.usecase.DeleteTaskUseCase
import com.hisham.todolist.domain.usecase.GetTaskByIdUseCase
import com.hisham.todolist.domain.usecase.ObserveTasksUseCase
import com.hisham.todolist.domain.usecase.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

enum class TaskIconOption(
    val iconName: String,
    val label: String,
) {
    TASKS("task_alt", "General"),
    WORK("work_outline", "Trabajo"),
    HOME("home", "Hogar"),
    HEALTH("favorite", "Salud"),
    STUDY("menu_book", "Estudio"),
    MAIL("mail_outline", "Correo"),
    STORAGE("database", "Datos"),
    PALETTE("palette", "Diseno"),
    OTHERS("more_horiz", "Otros"),
    ;

    companion object {
        fun fromIconName(iconName: String?): TaskIconOption =
            entries.firstOrNull { it.iconName == iconName } ?: TASKS
    }
}

sealed interface TaskSheetMode {
    data object Create : TaskSheetMode

    data class Edit(val taskId: Long) : TaskSheetMode
}

data class TaskManagementListItemUiModel(
    val id: Long,
    val title: String,
    val category: TaskCategory?,
    val isProgressEnabled: Boolean,
    val progress: Int,
    val isCompleted: Boolean,
    val isRecurrent: Boolean,
    val iconName: String?,
)

data class TaskFormState(
    val title: String = "",
    val selectedIcon: TaskIconOption = TaskIconOption.TASKS,
    val isRecurrent: Boolean = false,
    val recurrenceDays: Set<DayOfWeek> = emptySet(),
    val category: TaskCategory? = TaskCategory.PERSONAL,
    val isProgressEnabled: Boolean = false,
    val progressText: String = "0",
    val progressValue: Int = 0,
    val titleError: String? = null,
    val progressError: String? = null,
    val recurrenceError: String? = null,
    val isDirty: Boolean = false,
) {
    val hasErrors: Boolean
        get() = titleError != null || progressError != null || recurrenceError != null
}

data class TaskManagementUiState(
    val tasks: List<TaskManagementListItemUiModel> = emptyList(),
    val completedTasks: List<TaskManagementListItemUiModel> = emptyList(),
    val quickCreateText: String = "",
    val isSubmittingQuickCreate: Boolean = false,
    val isSheetVisible: Boolean = false,
    val sheetMode: TaskSheetMode = TaskSheetMode.Create,
    val selectedTaskId: Long? = null,
    val formState: TaskFormState = TaskFormState(),
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
)

private data class TaskManagementSheetState(
    val isSheetVisible: Boolean,
    val sheetMode: TaskSheetMode,
    val selectedTaskId: Long?,
    val formState: TaskFormState,
)

private data class TaskManagementOperationState(
    val quickCreateText: String,
    val isSubmittingQuickCreate: Boolean,
    val isSaving: Boolean,
    val isDeleting: Boolean,
    val errorMessage: String?,
)

@HiltViewModel
class TaskManagementViewModel @Inject constructor(
    observeTasksUseCase: ObserveTasksUseCase,
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val createQuickTaskUseCase: CreateQuickTaskUseCase,
) : ViewModel() {

    private val formState = MutableStateFlow(TaskFormState())
    private val quickCreateText = MutableStateFlow("")
    private val isSubmittingQuickCreate = MutableStateFlow(false)
    private val isSheetVisible = MutableStateFlow(false)
    private val sheetMode = MutableStateFlow<TaskSheetMode>(TaskSheetMode.Create)
    private val selectedTaskId = MutableStateFlow<Long?>(null)
    private val isSaving = MutableStateFlow(false)
    private val isDeleting = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val tasks = observeTasksUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    private val sheetState = combine(
        isSheetVisible,
        sheetMode,
        selectedTaskId,
        formState,
    ) { visible, mode, taskId, form ->
        TaskManagementSheetState(
            isSheetVisible = visible,
            sheetMode = mode,
            selectedTaskId = taskId,
            formState = form,
        )
    }

    private val operationState = combine(
        quickCreateText,
        isSubmittingQuickCreate,
        isSaving,
        isDeleting,
        errorMessage,
    ) { quickText, submittingQuickCreate, saving, deleting, error ->
        TaskManagementOperationState(
            quickCreateText = quickText,
            isSubmittingQuickCreate = submittingQuickCreate,
            isSaving = saving,
            isDeleting = deleting,
            errorMessage = error,
        )
    }

    val uiState: StateFlow<TaskManagementUiState> = combine(
        tasks,
        sheetState,
        operationState,
    ) { observedTasks, sheet, operation ->
        val mappedTasks = observedTasks.map { task -> task.toManagementListItem() }
        TaskManagementUiState(
            tasks = mappedTasks.filterNot(TaskManagementListItemUiModel::isCompleted),
            completedTasks = mappedTasks.filter(TaskManagementListItemUiModel::isCompleted),
            quickCreateText = operation.quickCreateText,
            isSubmittingQuickCreate = operation.isSubmittingQuickCreate,
            isSheetVisible = sheet.isSheetVisible,
            sheetMode = sheet.sheetMode,
            selectedTaskId = sheet.selectedTaskId,
            formState = sheet.formState,
            isSaving = operation.isSaving,
            isDeleting = operation.isDeleting,
            errorMessage = operation.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TaskManagementUiState(),
    )

    fun onQuickCreateTextChange(value: String) {
        quickCreateText.value = value
    }

    fun onQuickCreateSubmit() {
        if (isSubmittingQuickCreate.value) {
            return
        }

        val title = quickCreateText.value.trim()
        if (title.isBlank()) {
            quickCreateText.value = ""
            return
        }

        viewModelScope.launch {
            isSubmittingQuickCreate.value = true
            try {
                createQuickTaskUseCase(title)
                quickCreateText.value = ""
            } catch (error: Exception) {
                errorMessage.value = error.message ?: "No se pudo crear la tarea."
            } finally {
                isSubmittingQuickCreate.value = false
            }
        }
    }

    fun onCreateTaskClick() {
        sheetMode.value = TaskSheetMode.Create
        selectedTaskId.value = null
        formState.value = TaskFormState()
        isSheetVisible.value = true
    }

    fun onTaskClick(taskId: Long) {
        viewModelScope.launch {
            val task = getTaskByIdUseCase(taskId)
            if (task == null) {
                errorMessage.value = "La tarea seleccionada ya no existe."
                dismissSheet()
                return@launch
            }

            sheetMode.value = TaskSheetMode.Edit(taskId)
            selectedTaskId.value = taskId
            formState.value = task.toFormState()
            isSheetVisible.value = true
        }
    }

    fun onTitleChange(value: String) {
        formState.updateDirty {
            copy(
                title = value,
                titleError = null,
            )
        }
    }

    fun onIconSelected(icon: TaskIconOption) {
        formState.updateDirty {
            copy(selectedIcon = icon)
        }
    }

    fun onCategorySelected(category: TaskCategory) {
        formState.updateDirty {
            copy(category = category)
        }
    }

    fun onRecurrenceToggle(enabled: Boolean) {
        formState.updateDirty {
            copy(
                isRecurrent = enabled,
                recurrenceDays = if (enabled) recurrenceDays else emptySet(),
                recurrenceError = null,
            )
        }
    }

    fun onRecurrenceDayToggle(dayOfWeek: DayOfWeek) {
        formState.updateDirty {
            val updatedDays = recurrenceDays.toMutableSet().apply {
                if (!add(dayOfWeek)) {
                    remove(dayOfWeek)
                }
            }
            copy(
                recurrenceDays = updatedDays,
                recurrenceError = null,
            )
        }
    }

    fun onProgressToggle(enabled: Boolean) {
        formState.updateDirty {
            copy(
                isProgressEnabled = enabled,
                progressError = null,
            )
        }
    }

    fun onProgressValueChange(value: Int) {
        val normalized = value.coerceIn(0, 100)
        formState.updateDirty {
            copy(
                progressValue = normalized,
                progressText = normalized.toString(),
                progressError = null,
            )
        }
    }

    fun onProgressTextChange(value: String) {
        val sanitized = value.filter(Char::isDigit).take(3)
        val numericValue = sanitized.toIntOrNull()?.coerceIn(0, 100) ?: 0
        formState.updateDirty {
            copy(
                progressText = sanitized,
                progressValue = if (sanitized.isBlank()) 0 else numericValue,
                progressError = null,
            )
        }
    }

    fun onSheetDismiss() {
        dismissSheet()
    }

    fun onSaveClick() {
        if (isSaving.value || isDeleting.value) {
            return
        }

        viewModelScope.launch {
            val validated = formState.value.validated()
            formState.value = validated
            if (validated.hasErrors) {
                return@launch
            }

            isSaving.value = true
            try {
                when (val mode = sheetMode.value) {
                    TaskSheetMode.Create -> createTaskUseCase(validated.toNewTask())

                    is TaskSheetMode.Edit -> {
                        val currentTask = getTaskByIdUseCase(mode.taskId)
                        if (currentTask == null) {
                            errorMessage.value = "La tarea seleccionada ya no existe."
                            dismissSheet()
                            return@launch
                        }
                        updateTaskUseCase(validated.toUpdatedTask(currentTask))
                    }
                }
                dismissSheet()
            } catch (error: Exception) {
                errorMessage.value = error.message ?: "No se pudo guardar la tarea."
            } finally {
                isSaving.value = false
            }
        }
    }

    fun onDeleteClick() {
        val taskId = selectedTaskId.value ?: return
        if (isSaving.value || isDeleting.value) {
            return
        }

        viewModelScope.launch {
            isDeleting.value = true
            try {
                deleteTaskUseCase(taskId)
                dismissSheet()
            } catch (error: Exception) {
                errorMessage.value = error.message ?: "No se pudo eliminar la tarea."
            } finally {
                isDeleting.value = false
            }
        }
    }

    fun onDismissError() {
        errorMessage.value = null
    }

    private fun dismissSheet() {
        isSheetVisible.value = false
        sheetMode.value = TaskSheetMode.Create
        selectedTaskId.value = null
        formState.value = TaskFormState()
    }

    private fun MutableStateFlow<TaskFormState>.updateDirty(
        transform: TaskFormState.() -> TaskFormState,
    ) {
        value = value.transform().copy(isDirty = true)
    }

    private fun Task.toManagementListItem(): TaskManagementListItemUiModel =
        TaskManagementListItemUiModel(
            id = id,
            title = title,
            category = category,
            isProgressEnabled = isProgressEnabled,
            progress = progress,
            isCompleted = isCompleted,
            isRecurrent = isRecurrent,
            iconName = iconName,
        )

    private fun Task.toFormState(): TaskFormState = TaskFormState(
        title = title,
        selectedIcon = TaskIconOption.fromIconName(iconName),
        isRecurrent = isRecurrent,
        recurrenceDays = recurrenceDays,
        category = category ?: TaskCategory.PERSONAL,
        isProgressEnabled = isProgressEnabled,
        progressText = progress.toString(),
        progressValue = progress,
        isDirty = false,
    )

    private fun TaskFormState.validated(): TaskFormState {
        val trimmedTitle = title.trim()
        val parsedProgress = progressText.toIntOrNull()
        val progressError = if (!isProgressEnabled) {
            null
        } else {
            when {
                progressText.isBlank() -> "El progreso es obligatorio."
                parsedProgress == null -> "Introduce un numero valido."
                parsedProgress !in 0..100 -> "El progreso debe estar entre 0 y 100."
                else -> null
            }
        }

        return copy(
            title = title,
            titleError = if (trimmedTitle.isBlank()) "El titulo es obligatorio." else null,
            progressError = progressError,
            recurrenceError = if (isRecurrent && recurrenceDays.isEmpty()) {
                "Selecciona al menos un dia."
            } else {
                null
            },
            progressValue = if (isProgressEnabled) {
                parsedProgress?.coerceIn(0, 100) ?: progressValue
            } else {
                progressValue
            },
        )
    }

    private fun TaskFormState.toNewTask(): Task = Task(
        title = title.trim(),
        isProgressEnabled = isProgressEnabled,
        progress = progressValue.coerceIn(0, 100),
        isRecurrent = isRecurrent,
        recurrenceDays = if (isRecurrent) recurrenceDays else emptySet(),
        stateDateEpochDay = null,
        category = category ?: TaskCategory.PERSONAL,
        iconName = selectedIcon.iconName,
        isCompleted = false,
    )

    private fun TaskFormState.toUpdatedTask(currentTask: Task): Task = currentTask.copy(
        title = title.trim(),
        isProgressEnabled = isProgressEnabled,
        progress = progressValue.coerceIn(0, 100),
        isRecurrent = isRecurrent,
        recurrenceDays = if (isRecurrent) recurrenceDays else emptySet(),
        stateDateEpochDay = if (isRecurrent) currentTask.stateDateEpochDay else null,
        category = category ?: TaskCategory.PERSONAL,
        iconName = selectedIcon.iconName,
    )
}
