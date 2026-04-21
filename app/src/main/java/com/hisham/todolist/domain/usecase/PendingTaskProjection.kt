package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.Task
import java.time.DayOfWeek
import java.time.LocalDate

internal fun List<Task>.toPendingTasksForDate(currentDate: LocalDate): List<Task> {
    val currentDay = currentDate.dayOfWeek
    val currentEpochDay = currentDate.toEpochDay()

    return mapNotNull { task ->
        task.toPendingTaskForDate(
            currentDay = currentDay,
            currentEpochDay = currentEpochDay,
        )
    }.sortedWith(compareBy<Task> { it.position }.thenBy { it.id })
}

internal fun Task.toPendingTaskForDate(
    currentDay: DayOfWeek,
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
