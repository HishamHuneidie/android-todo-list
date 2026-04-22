package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.Task

data class PendingTaskSections(
    val activeTasks: List<Task> = emptyList(),
    val completedTasks: List<Task> = emptyList(),
)
