package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.repository.TaskRepository
import javax.inject.Inject

class GetTaskByIdUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(taskId: Long): Task? = taskRepository.getTask(taskId)
}
