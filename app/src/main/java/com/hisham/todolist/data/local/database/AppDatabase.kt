package com.hisham.todolist.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hisham.todolist.data.local.converter.DayOfWeekSetConverter
import com.hisham.todolist.data.local.converter.TaskCategoryConverter
import com.hisham.todolist.data.local.dao.TaskDao
import com.hisham.todolist.data.local.entity.TaskEntity

@Database(
    entities = [TaskEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(
    DayOfWeekSetConverter::class,
    TaskCategoryConverter::class,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
