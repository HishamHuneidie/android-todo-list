package com.hisham.todolist.domain.model

data class TaskCompletionRecord(
    val taskId: Long,
    val occurrenceEpochDay: Long,
    val completedAt: Long,
)
