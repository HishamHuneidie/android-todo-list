package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.repository.TaskRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class PrepareTasksForCurrentDayUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(): List<Task> {
        val currentDay = LocalDate.now().dayOfWeek
        return taskRepository.observeTasks().first().filter { task ->
            !task.isRecurrent || currentDay in task.recurrenceDays
        }
    }
}
