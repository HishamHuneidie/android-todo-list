package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.Task
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
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

internal fun List<Task>.toPendingTaskSections(
    currentDate: LocalDate,
    clock: Clock,
): PendingTaskSections {
    val currentDay = currentDate.dayOfWeek
    val currentEpochDay = currentDate.toEpochDay()
    val activeTasks = mutableListOf<Task>()
    val completedTasks = mutableListOf<Task>()

    forEach { task ->
        if (!task.isRecurrent) {
            when {
                task.isCompleted && task.updatedAt.toLocalDate(clock) == currentDate -> {
                    completedTasks += task
                }

                !task.isCompleted -> {
                    activeTasks += task
                }
            }
            return@forEach
        }

        if (currentDay !in task.recurrenceDays) {
            return@forEach
        }

        val taskForToday = if (task.stateDateEpochDay == currentEpochDay) {
            task
        } else {
            task.copy(
                isCompleted = false,
                progress = 0,
                stateDateEpochDay = currentEpochDay,
            )
        }

        if (taskForToday.isCompleted) {
            completedTasks += taskForToday
        } else {
            activeTasks += taskForToday
        }
    }

    return PendingTaskSections(
        activeTasks = activeTasks.sortedWith(compareBy<Task> { it.position }.thenBy { it.id }),
        completedTasks = completedTasks.sortedWith(compareBy<Task> { it.position }.thenBy { it.id }),
    )
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

private fun Long.toLocalDate(clock: Clock): LocalDate =
    Instant.ofEpochMilli(this)
        .atZone(clock.zone)
        .toLocalDate()
