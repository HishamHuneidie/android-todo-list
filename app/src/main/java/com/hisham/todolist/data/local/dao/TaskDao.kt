package com.hisham.todolist.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hisham.todolist.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY position ASC, id ASC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    @Upsert
    suspend fun upsertTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("UPDATE tasks SET is_completed = :isCompleted, updated_at = :updatedAt WHERE id = :taskId")
    suspend fun updateCompletion(taskId: Long, isCompleted: Boolean, updatedAt: Long)

    @Query("UPDATE tasks SET progress = :progress, updated_at = :updatedAt WHERE id = :taskId")
    suspend fun updateProgress(taskId: Long, progress: Int, updatedAt: Long)

    @Query("UPDATE tasks SET position = :position, updated_at = :updatedAt WHERE id = :taskId")
    suspend fun updatePosition(taskId: Long, position: Int, updatedAt: Long)
}
