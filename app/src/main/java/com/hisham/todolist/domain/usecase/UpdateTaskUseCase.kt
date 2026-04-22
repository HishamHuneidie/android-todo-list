package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.repository.TaskRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class UpdateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(task: Task) {
        val existingTask = if (task.id == 0L) {
            null
        } else {
            taskRepository.getTask(task.id)
        }

        val normalizedTask = existingTask?.let { currentTask ->
            task.withCurrentDayStateIfNeeded(
                currentTask = currentTask,
                currentEpochDay = LocalDate.now(clock).toEpochDay(),
            )
        } ?: task

        taskRepository.upsertTask(
            normalizedTask.copy(updatedAt = clock.millis()),
        )
    }
}

private fun Task.withCurrentDayStateIfNeeded(
    currentTask: Task,
    currentEpochDay: Long,
): Task {
    if (!isRecurrent) {
        return copy(stateDateEpochDay = null)
    }

    val shouldRefreshStateDate = currentTask.isRecurrent != isRecurrent ||
            currentTask.isCompleted != isCompleted ||
            currentTask.isProgressEnabled != isProgressEnabled ||
            currentTask.progress != progress

    return if (shouldRefreshStateDate) {
        copy(stateDateEpochDay = currentEpochDay)
    } else {
        this
    }
}
