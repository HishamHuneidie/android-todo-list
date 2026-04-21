package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.repository.TaskRepository
import javax.inject.Inject

class UpdateTaskProgressUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(
        taskId: Long,
        progress: Int,
    ) {
        taskRepository.updateTaskProgress(
            taskId = taskId,
            progress = progress.coerceIn(0, 100),
        )
    }
}
