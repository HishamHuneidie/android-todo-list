package com.hisham.todolist.domain.model

import java.time.DayOfWeek

data class Task(
    val id: Long = 0L,
    val title: String,
    val isCompleted: Boolean = false,
    val isProgressEnabled: Boolean = false,
    val progress: Int = 0,
    val isRecurrent: Boolean = false,
    val recurrenceDays: Set<DayOfWeek> = emptySet(),
    val stateDateEpochDay: Long? = null,
    val category: TaskCategory? = null,
    val iconName: String? = null,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
