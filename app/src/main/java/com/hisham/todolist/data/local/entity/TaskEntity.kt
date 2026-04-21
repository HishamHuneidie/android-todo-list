package com.hisham.todolist.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hisham.todolist.domain.model.TaskCategory
import java.time.DayOfWeek

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,
    val progress: Int,
    @ColumnInfo(name = "is_recurrent")
    val isRecurrent: Boolean,
    @ColumnInfo(name = "recurrence_days")
    val recurrenceDays: Set<DayOfWeek>,
    @ColumnInfo(name = "state_date_epoch_day")
    val stateDateEpochDay: Long?,
    val category: TaskCategory?,
    @ColumnInfo(name = "icon_name")
    val iconName: String?,
    val position: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
