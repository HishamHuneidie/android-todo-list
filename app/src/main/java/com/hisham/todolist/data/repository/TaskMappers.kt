package com.hisham.todolist.data.repository

import com.hisham.todolist.data.local.entity.TaskEntity
import com.hisham.todolist.domain.model.Task

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    isCompleted = isCompleted,
    isProgressEnabled = isProgressEnabled,
    progress = progress.coerceIn(0, 100),
    isRecurrent = isRecurrent,
    recurrenceDays = if (isRecurrent) recurrenceDays else emptySet(),
    stateDateEpochDay = stateDateEpochDay.takeIf { isRecurrent },
    category = category,
    iconName = iconName,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    isCompleted = isCompleted,
    isProgressEnabled = isProgressEnabled,
    progress = progress.coerceIn(0, 100),
    isRecurrent = isRecurrent,
    recurrenceDays = if (isRecurrent) recurrenceDays else emptySet(),
    stateDateEpochDay = stateDateEpochDay.takeIf { isRecurrent },
    category = category,
    iconName = iconName,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
