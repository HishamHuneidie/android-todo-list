package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.repository.TaskRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObservePendingTaskSectionsUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<PendingTaskSections> =
        taskRepository.observeTasks().map { tasks ->
            tasks.toPendingTaskSections(
                currentDate = LocalDate.now(clock),
                clock = clock,
            )
        }
}
