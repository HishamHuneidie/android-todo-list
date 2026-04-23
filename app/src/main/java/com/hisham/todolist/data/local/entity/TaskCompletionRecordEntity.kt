package com.hisham.todolist.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "task_completion_records",
    primaryKeys = ["task_id", "occurrence_epoch_day"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["task_id"]),
    ],
)
data class TaskCompletionRecordEntity(
    @ColumnInfo(name = "task_id")
    val taskId: Long,
    @ColumnInfo(name = "occurrence_epoch_day")
    val occurrenceEpochDay: Long,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long,
)
