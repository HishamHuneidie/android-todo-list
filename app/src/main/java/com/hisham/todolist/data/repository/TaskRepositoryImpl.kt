package com.hisham.todolist.data.repository

import androidx.room.withTransaction
import com.hisham.todolist.data.local.dao.TaskCompletionRecordDao
import com.hisham.todolist.data.local.dao.TaskDao
import com.hisham.todolist.data.local.database.AppDatabase
import com.hisham.todolist.data.local.entity.TaskCompletionRecordEntity
import com.hisham.todolist.data.local.entity.TaskEntity
import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.model.TaskCompletionRecord
import com.hisham.todolist.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val taskDao: TaskDao,
    private val taskCompletionRecordDao: TaskCompletionRecordDao,
    private val clock: Clock,
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> =
        taskDao.observeTasks().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeTaskCompletionRecords(): Flow<List<TaskCompletionRecord>> =
        taskCompletionRecordDao.observeRecords().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getTask(taskId: Long): Task? = taskDao.getTaskById(taskId)?.toDomain()

    override suspend fun upsertTask(task: Task) {
        taskDao.upsertTask(task.copy(updatedAt = clock.millis()).toEntity())
    }

    override suspend fun deleteTask(taskId: Long) {
        taskDao.deleteTaskById(taskId)
    }

    override suspend fun updateTaskCompletion(taskId: Long, isCompleted: Boolean) {
        val task = taskDao.getTaskById(taskId) ?: return
        val updatedAt = clock.millis()
        val occurrenceEpochDay = task.occurrenceEpochDay(clock)
        database.withTransaction {
            taskDao.updateCompletion(
                taskId = taskId,
                isCompleted = isCompleted,
                updatedAt = updatedAt,
                stateDateEpochDay = task.stateDateForInteraction(),
            )
            if (isCompleted) {
                taskCompletionRecordDao.upsertRecord(
                    TaskCompletionRecordEntity(
                        taskId = taskId,
                        occurrenceEpochDay = occurrenceEpochDay,
                        completedAt = updatedAt,
                    ),
                )
            } else if (task.isRecurrent) {
                taskCompletionRecordDao.deleteRecord(taskId, occurrenceEpochDay)
            } else {
                taskCompletionRecordDao.deleteRecordsForTask(taskId)
            }
        }
    }

    override suspend fun updateTaskProgress(taskId: Long, progress: Int) {
        val task = taskDao.getTaskById(taskId) ?: return
        taskDao.updateProgress(
            taskId = taskId,
            progress = progress.coerceIn(0, 100),
            updatedAt = clock.millis(),
            stateDateEpochDay = task.stateDateForInteraction(),
        )
    }

    override suspend fun reorderTasks(taskIdsInOrder: List<Long>) {
        val updatedAt = clock.millis()
        database.withTransaction {
            taskIdsInOrder.forEachIndexed { index, taskId ->
                taskDao.updatePosition(
                    taskId = taskId,
                    position = index,
                    updatedAt = updatedAt,
                )
            }
        }
    }

    private fun TaskEntity.stateDateForInteraction(): Long? =
        if (isRecurrent) {
            LocalDate.now(clock).toEpochDay()
        } else {
            null
        }

    private fun TaskEntity.occurrenceEpochDay(clock: Clock): Long =
        if (isRecurrent) {
            LocalDate.now(clock).toEpochDay()
        } else {
            LocalDate.now(clock).toEpochDay()
        }
}
