package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class CalculateDailyAverageUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<Double> =
        taskRepository.observeTaskCompletionRecords().map { records ->
            val currentDate = LocalDate.now(clock)
            val currentMonth = YearMonth.from(currentDate)
            val monthlyCompleted = records.count { record ->
                YearMonth.from(record.completedAt.toLocalDate(clock)) == currentMonth
            }
            monthlyCompleted.toDouble() / currentDate.dayOfMonth.toDouble()
        }
}

private fun Long.toLocalDate(clock: Clock): LocalDate =
    Instant.ofEpochMilli(this).atZone(clock.zone).toLocalDate()
