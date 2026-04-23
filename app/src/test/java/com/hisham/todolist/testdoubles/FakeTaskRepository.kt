package com.hisham.todolist.testdoubles

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.model.TaskCompletionRecord
import com.hisham.todolist.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Clock
import java.time.LocalDate

class FakeTaskRepository(
    initialTasks: List<Task> = emptyList(),
    initialRecords: List<TaskCompletionRecord> = emptyList(),
    private val clock: Clock = Clock.systemUTC(),
) : TaskRepository {

    data class CompletionUpdate(
        val taskId: Long,
        val isCompleted: Boolean,
    )

    data class ProgressUpdate(
        val taskId: Long,
        val progress: Int,
    )

    val tasks = MutableStateFlow(initialTasks.sortedTasks())
    val records = MutableStateFlow(initialRecords.sortedRecords())

    val getTaskCalls = mutableListOf<Long>()
    val upsertCalls = mutableListOf<Task>()
    val deleteCalls = mutableListOf<Long>()
    val completionUpdates = mutableListOf<CompletionUpdate>()
    val progressUpdates = mutableListOf<ProgressUpdate>()
    val reorderCalls = mutableListOf<List<Long>>()

    var getTaskError: Throwable? = null
    var upsertError: Throwable? = null
    var deleteError: Throwable? = null
    var updateCompletionError: Throwable? = null
    var updateProgressError: Throwable? = null
    var reorderError: Throwable? = null
    var getTaskAction: (suspend (Long) -> Task?)? = null
    var upsertAction: (suspend (Task) -> Unit)? = null
    var deleteAction: (suspend (Long) -> Unit)? = null
    var updateCompletionAction: (suspend (Long, Boolean) -> Unit)? = null
    var updateProgressAction: (suspend (Long, Int) -> Unit)? = null
    var reorderAction: (suspend (List<Long>) -> Unit)? = null

    override fun observeTasks(): Flow<List<Task>> = tasks

    override fun observeTaskCompletionRecords(): Flow<List<TaskCompletionRecord>> = records

    override suspend fun getTask(taskId: Long): Task? {
        getTaskCalls += taskId
        getTaskError?.let { throw it }
        getTaskAction?.let { return it(taskId) }
        return tasks.value.firstOrNull { it.id == taskId }
    }

    override suspend fun upsertTask(task: Task) {
        upsertCalls += task
        upsertError?.let { throw it }
        upsertAction?.invoke(task)

        val nextId = if (task.id == 0L) {
            (tasks.value.maxOfOrNull(Task::id) ?: 0L) + 1L
        } else {
            task.id
        }

        val updatedTask = task.copy(id = nextId)
        tasks.value = (tasks.value.filterNot { it.id == nextId } + updatedTask).sortedTasks()
    }

    override suspend fun deleteTask(taskId: Long) {
        deleteCalls += taskId
        deleteError?.let { throw it }
        deleteAction?.invoke(taskId)

        tasks.value = tasks.value.filterNot { it.id == taskId }.sortedTasks()
        records.value = records.value.filterNot { it.taskId == taskId }.sortedRecords()
    }

    override suspend fun updateTaskCompletion(
        taskId: Long,
        isCompleted: Boolean,
    ) {
        completionUpdates += CompletionUpdate(taskId, isCompleted)
        updateCompletionError?.let { throw it }
        updateCompletionAction?.invoke(taskId, isCompleted)

        val currentTask = tasks.value.firstOrNull { it.id == taskId } ?: return
        val currentEpochDay = LocalDate.now(clock).toEpochDay()
        val updatedAt = clock.millis()

        tasks.value = tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(
                    isCompleted = isCompleted,
                    updatedAt = updatedAt,
                    stateDateEpochDay = if (task.isRecurrent) currentEpochDay else task.stateDateEpochDay,
                )
            } else {
                task
            }
        }.sortedTasks()

        records.value = when {
            isCompleted -> {
                val newRecord = TaskCompletionRecord(
                    taskId = taskId,
                    occurrenceEpochDay = currentEpochDay,
                    completedAt = updatedAt,
                )
                (records.value.filterNot {
                    it.taskId == taskId && it.occurrenceEpochDay == currentEpochDay
                } + newRecord).sortedRecords()
            }

            currentTask.isRecurrent -> {
                records.value.filterNot {
                    it.taskId == taskId && it.occurrenceEpochDay == currentEpochDay
                }.sortedRecords()
            }

            else -> {
                records.value.filterNot { it.taskId == taskId }.sortedRecords()
            }
        }
    }

    override suspend fun updateTaskProgress(
        taskId: Long,
        progress: Int,
    ) {
        val normalizedProgress = progress.coerceIn(0, 100)
        progressUpdates += ProgressUpdate(taskId, normalizedProgress)
        updateProgressError?.let { throw it }
        updateProgressAction?.invoke(taskId, normalizedProgress)

        val currentEpochDay = LocalDate.now(clock).toEpochDay()
        val updatedAt = clock.millis()

        tasks.value = tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(
                    progress = normalizedProgress,
                    updatedAt = updatedAt,
                    stateDateEpochDay = if (task.isRecurrent) currentEpochDay else null,
                )
            } else {
                task
            }
        }.sortedTasks()
    }

    override suspend fun reorderTasks(taskIdsInOrder: List<Long>) {
        reorderCalls += taskIdsInOrder
        reorderError?.let { throw it }
        reorderAction?.invoke(taskIdsInOrder)

        val now = clock.millis()
        val positionsById = taskIdsInOrder.withIndex().associate { it.value to it.index }
        tasks.value = tasks.value.map { task ->
            positionsById[task.id]?.let { position ->
                task.copy(position = position, updatedAt = now)
            } ?: task
        }.sortedTasks()
    }

    fun emitTasks(updatedTasks: List<Task>) {
        tasks.value = updatedTasks.sortedTasks()
    }

    fun emitRecords(updatedRecords: List<TaskCompletionRecord>) {
        records.value = updatedRecords.sortedRecords()
    }

    private fun List<Task>.sortedTasks(): List<Task> =
        sortedWith(compareBy<Task> { it.position }.thenBy { it.id })

    private fun List<TaskCompletionRecord>.sortedRecords(): List<TaskCompletionRecord> =
        sortedWith(compareBy<TaskCompletionRecord> { it.completedAt }.thenBy { it.taskId })
}
