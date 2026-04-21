package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.repository.TaskRepository
import javax.inject.Inject

class ToggleTaskCompletionUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(
        taskId: Long,
        isCompleted: Boolean,
    ) {
        taskRepository.updateTaskCompletion(
            taskId = taskId,
            isCompleted = isCompleted,
        )
    }
}
