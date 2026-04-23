package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.model.TaskCompletionRecord
import com.hisham.todolist.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlin.math.roundToInt

class CalculateWeeklyEffectivenessUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<Int> = combine(
        taskRepository.observeTasks(),
        taskRepository.observeTaskCompletionRecords(),
    ) { tasks, records ->
        val currentDate = LocalDate.now(clock)
        val weekStart = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekStartEpochDay = weekStart.toEpochDay()
        val currentEpochDay = currentDate.toEpochDay()
        val recordsByTaskId = records.groupBy(TaskCompletionRecord::taskId)

        val totalScheduled = tasks.sumOf { task ->
            task.scheduledOccurrencesThisWeek(
                weekStart = weekStart,
                currentDate = currentDate,
                records = recordsByTaskId[task.id].orEmpty(),
                clock = clock,
            )
        }
        val totalCompleted = tasks.sumOf { task ->
            task.completedOccurrencesThisWeek(
                weekStartEpochDay = weekStartEpochDay,
                currentEpochDay = currentEpochDay,
                records = recordsByTaskId[task.id].orEmpty(),
            )
        }

        if (totalScheduled == 0) {
            0
        } else {
            ((totalCompleted.toDouble() / totalScheduled.toDouble()) * 100)
                .roundToInt()
                .coerceIn(0, 100)
        }
    }
}

private fun Task.scheduledOccurrencesThisWeek(
    weekStart: LocalDate,
    currentDate: LocalDate,
    records: List<TaskCompletionRecord>,
    clock: Clock,
): Int {
    val createdDate = createdAt.toLocalDate(clock)
    if (createdDate.isAfter(currentDate)) {
        return 0
    }

    if (isRecurrent) {
        var scheduledCount = 0
        var date = maxOf(createdDate, weekStart)
        while (!date.isAfter(currentDate)) {
            if (date.dayOfWeek in recurrenceDays) {
                scheduledCount += 1
            }
            date = date.plusDays(1)
        }
        return scheduledCount
    }

    val latestCompletion = records.maxByOrNull(TaskCompletionRecord::completedAt)
    return if (latestCompletion?.occurrenceEpochDay?.let { it < weekStart.toEpochDay() } == true) {
        0
    } else {
        1
    }
}

private fun Task.completedOccurrencesThisWeek(
    weekStartEpochDay: Long,
    currentEpochDay: Long,
    records: List<TaskCompletionRecord>,
): Int {
    if (isRecurrent) {
        return records.count { record ->
            record.occurrenceEpochDay in weekStartEpochDay..currentEpochDay
        }
    }

    return if (records.any { record -> record.occurrenceEpochDay in weekStartEpochDay..currentEpochDay }) {
        1
    } else {
        0
    }
}

private fun Long.toLocalDate(clock: Clock): LocalDate =
    Instant.ofEpochMilli(this).atZone(clock.zone).toLocalDate()
