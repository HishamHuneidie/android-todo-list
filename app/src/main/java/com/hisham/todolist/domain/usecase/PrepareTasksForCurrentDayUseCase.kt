package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.repository.TaskRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class PrepareTasksForCurrentDayUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): List<Task> {
        val currentDate = LocalDate.now(clock)
        val currentDay = currentDate.dayOfWeek
        val currentEpochDay = currentDate.toEpochDay()

        return taskRepository.observeTasks().first().mapNotNull { task ->
            task.toPendingTaskForCurrentDay(
                currentDay = currentDay,
                currentEpochDay = currentEpochDay,
            )
        }
    }

    private fun Task.toPendingTaskForCurrentDay(
        currentDay: java.time.DayOfWeek,
        currentEpochDay: Long,
    ): Task? {
        if (!isRecurrent) {
            return takeUnless(Task::isCompleted)
        }

        if (currentDay !in recurrenceDays) {
            return null
        }

        val taskForToday = if (stateDateEpochDay == currentEpochDay) {
            this
        } else {
            copy(
                isCompleted = false,
                progress = 0,
                stateDateEpochDay = currentEpochDay,
            )
        }

        return taskForToday.takeUnless(Task::isCompleted)
    }
}
