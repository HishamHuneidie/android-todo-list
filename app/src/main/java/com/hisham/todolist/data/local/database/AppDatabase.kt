package com.hisham.todolist.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hisham.todolist.data.local.converter.DayOfWeekSetConverter
import com.hisham.todolist.data.local.converter.TaskCategoryConverter
import com.hisham.todolist.data.local.dao.TaskDao
import com.hisham.todolist.data.local.entity.TaskEntity

@Database(
    entities = [TaskEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(
    DayOfWeekSetConverter::class,
    TaskCategoryConverter::class,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE tasks ADD COLUMN is_progress_enabled INTEGER NOT NULL DEFAULT 0",
                )
            }
        }
    }
}
