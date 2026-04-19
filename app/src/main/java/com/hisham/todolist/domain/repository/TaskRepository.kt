package com.hisham.todolist.domain.repository

import com.hisham.todolist.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    suspend fun getTask(taskId: Long): Task?
    suspend fun upsertTask(task: Task)
    suspend fun deleteTask(taskId: Long)
    suspend fun updateTaskCompletion(taskId: Long, isCompleted: Boolean)
    suspend fun updateTaskProgress(taskId: Long, progress: Int)
    suspend fun reorderTasks(taskIdsInOrder: List<Long>)
}
