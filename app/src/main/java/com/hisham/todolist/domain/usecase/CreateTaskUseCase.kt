package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject

class CreateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(task: Task): Task {
        val nextPosition = taskRepository.observeTasks()
            .first()
            .maxOfOrNull(Task::position)
            ?.plus(1)
            ?: 0
        val now = clock.millis()
        val newTask = task.copy(
            id = 0L,
            position = nextPosition,
            createdAt = now,
            updatedAt = now,
        )
        taskRepository.upsertTask(newTask)
        return newTask
    }
}
