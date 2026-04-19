package com.hisham.todolist.data.repository

import androidx.room.withTransaction
import com.hisham.todolist.data.local.dao.TaskDao
import com.hisham.todolist.data.local.database.AppDatabase
import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.repository.TaskRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val taskDao: TaskDao,
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> =
        taskDao.observeTasks().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getTask(taskId: Long): Task? = taskDao.getTaskById(taskId)?.toDomain()

    override suspend fun upsertTask(task: Task) {
        taskDao.upsertTask(task.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    override suspend fun deleteTask(taskId: Long) {
        taskDao.deleteTaskById(taskId)
    }

    override suspend fun updateTaskCompletion(taskId: Long, isCompleted: Boolean) {
        taskDao.updateCompletion(
            taskId = taskId,
            isCompleted = isCompleted,
            updatedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun updateTaskProgress(taskId: Long, progress: Int) {
        taskDao.updateProgress(
            taskId = taskId,
            progress = progress.coerceIn(0, 100),
            updatedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun reorderTasks(taskIdsInOrder: List<Long>) {
        database.withTransaction {
            taskIdsInOrder.forEachIndexed { index, taskId ->
                taskDao.updatePosition(
                    taskId = taskId,
                    position = index,
                    updatedAt = System.currentTimeMillis(),
                )
            }
        }
    }
}
