package com.hisham.todolist.data.repository

import com.hisham.todolist.data.local.entity.TaskCompletionRecordEntity
import com.hisham.todolist.domain.model.TaskCompletionRecord

fun TaskCompletionRecordEntity.toDomain(): TaskCompletionRecord = TaskCompletionRecord(
    taskId = taskId,
    occurrenceEpochDay = occurrenceEpochDay,
    completedAt = completedAt,
)

fun TaskCompletionRecord.toEntity(): TaskCompletionRecordEntity = TaskCompletionRecordEntity(
    taskId = taskId,
    occurrenceEpochDay = occurrenceEpochDay,
    completedAt = completedAt,
)
