package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.repository.TaskRepository
import javax.inject.Inject

class ReorderTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(taskIdsInOrder: List<Long>) {
        taskRepository.reorderTasks(taskIdsInOrder)
    }
}
