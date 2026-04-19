package com.hisham.todolist.data.local.converter

import androidx.room.TypeConverter
import com.hisham.todolist.domain.model.TaskCategory

class TaskCategoryConverter {

    @TypeConverter
    fun fromTaskCategory(category: TaskCategory?): String? = category?.name

    @TypeConverter
    fun toTaskCategory(rawValue: String?): TaskCategory? = rawValue?.let(TaskCategory::valueOf)
}
