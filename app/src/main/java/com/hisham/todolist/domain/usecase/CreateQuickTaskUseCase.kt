package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject

class CreateQuickTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(rawTitle: String): Task? {
        val title = rawTitle.trim()
        if (title.isBlank()) {
            return null
        }

        val nextPosition = taskRepository.observeTasks()
            .first()
            .maxOfOrNull(Task::position)
            ?.plus(1)
            ?: 0
        val now = clock.millis()
        val task = Task(
            title = title,
            position = nextPosition,
            createdAt = now,
            updatedAt = now,
        )

        taskRepository.upsertTask(task)
        return task
    }
}
