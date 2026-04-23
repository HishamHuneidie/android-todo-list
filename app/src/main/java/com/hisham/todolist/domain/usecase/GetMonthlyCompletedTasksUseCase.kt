package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class GetMonthlyCompletedTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<Int> =
        taskRepository.observeTaskCompletionRecords().map { records ->
            val currentMonth = YearMonth.now(clock)
            records.count { record ->
                YearMonth.from(record.completedAt.toLocalDate(clock)) == currentMonth
            }
        }
}

private fun Long.toLocalDate(clock: Clock): LocalDate =
    Instant.ofEpochMilli(this).atZone(clock.zone).toLocalDate()
