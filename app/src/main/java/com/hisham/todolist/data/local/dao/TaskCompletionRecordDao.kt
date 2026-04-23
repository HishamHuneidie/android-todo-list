package com.hisham.todolist.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hisham.todolist.data.local.entity.TaskCompletionRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskCompletionRecordDao {

    @Query("SELECT * FROM task_completion_records ORDER BY completed_at ASC, task_id ASC")
    fun observeRecords(): Flow<List<TaskCompletionRecordEntity>>

    @Upsert
    suspend fun upsertRecord(record: TaskCompletionRecordEntity)

    @Query("DELETE FROM task_completion_records WHERE task_id = :taskId AND occurrence_epoch_day = :occurrenceEpochDay")
    suspend fun deleteRecord(taskId: Long, occurrenceEpochDay: Long)

    @Query("DELETE FROM task_completion_records WHERE task_id = :taskId")
    suspend fun deleteRecordsForTask(taskId: Long)
}
