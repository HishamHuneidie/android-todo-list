package com.hisham.todolist.presentation.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.model.TaskCategory
import com.hisham.todolist.domain.usecase.ObservePendingTaskSectionsUseCase
import com.hisham.todolist.domain.usecase.PendingTaskSections
import com.hisham.todolist.domain.usecase.ReorderTasksUseCase
import com.hisham.todolist.domain.usecase.ToggleTaskCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskListItemUiModel(
    val id: Long,
    val title: String,
    val isCompleted: Boolean,
    val isProgressEnabled: Boolean,
    val progress: Int,
    val isRecurrent: Boolean,
    val category: TaskCategory?,
    val iconName: String?,
)

data class TasksUiState(
    val tasks: List<TaskListItemUiModel> = emptyList(),
    val completedTasks: List<TaskListItemUiModel> = emptyList(),
    val draggingTaskId: Long? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    observePendingTaskSectionsUseCase: ObservePendingTaskSectionsUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val reorderTasksUseCase: ReorderTasksUseCase,
) : ViewModel() {

    private data class TasksBaseUiState(
        val sections: PendingTaskSections,
        val draggingTaskId: Long?,
        val errorMessage: String?,
    )

    private val draggingTaskId = MutableStateFlow<Long?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val previewTasks = MutableStateFlow<List<TaskListItemUiModel>?>(null)

    private val pendingTasks = observePendingTaskSectionsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = PendingTaskSections(),
        )

    val uiState: StateFlow<TasksUiState> = combine(
        pendingTasks,
        draggingTaskId,
        errorMessage,
    ) { sections, draggedTaskId, error ->
        TasksBaseUiState(
            sections = sections,
            draggingTaskId = draggedTaskId,
            errorMessage = error,
        )
    }.combine(previewTasks) { baseState, preview ->
        val mappedTasks = preview ?: baseState.sections.activeTasks.map { task -> task.toUiModel() }
        TasksUiState(
            tasks = mappedTasks,
            completedTasks = baseState.sections.completedTasks.map { task -> task.toUiModel() },
            draggingTaskId = baseState.draggingTaskId,
            errorMessage = baseState.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TasksUiState(),
    )

    fun onToggleTask(
        taskId: Long,
        isCompleted: Boolean,
    ) {
        viewModelScope.launch {
            try {
                toggleTaskCompletionUseCase(
                    taskId = taskId,
                    isCompleted = isCompleted,
                )
            } catch (error: Exception) {
                errorMessage.value = error.message ?: "No se pudo actualizar la tarea."
            }
        }
    }

    fun onDragStart(taskId: Long) {
        draggingTaskId.value = taskId
        if (previewTasks.value == null) {
            previewTasks.value = uiState.value.tasks
        }
    }

    fun onReorderPreview(
        fromIndex: Int,
        toIndex: Int,
    ) {
        val current = previewTasks.value ?: uiState.value.tasks
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) {
            return
        }

        val reordered = current.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        previewTasks.value = reordered
    }

    fun onReorderCommit(taskIdsInOrder: List<Long>) {
        if (taskIdsInOrder.isEmpty()) {
            onReorderCancel()
            return
        }

        viewModelScope.launch {
            try {
                reorderTasksUseCase(taskIdsInOrder)
            } catch (error: Exception) {
                errorMessage.value = error.message ?: "No se pudo guardar el nuevo orden."
            } finally {
                previewTasks.value = null
                draggingTaskId.value = null
            }
        }
    }

    fun onReorderCancel() {
        previewTasks.value = null
        draggingTaskId.value = null
    }

    fun onDismissError() {
        errorMessage.value = null
    }

    private fun Task.toUiModel(): TaskListItemUiModel = TaskListItemUiModel(
        id = id,
        title = title,
        isCompleted = isCompleted,
        isProgressEnabled = isProgressEnabled,
        progress = progress,
        isRecurrent = isRecurrent,
        category = category,
        iconName = iconName,
    )
}
