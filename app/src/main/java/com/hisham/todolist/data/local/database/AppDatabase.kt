package com.hisham.todolist.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hisham.todolist.data.local.converter.DayOfWeekSetConverter
import com.hisham.todolist.data.local.converter.TaskCategoryConverter
import com.hisham.todolist.data.local.dao.TaskCompletionRecordDao
import com.hisham.todolist.data.local.dao.TaskDao
import com.hisham.todolist.data.local.entity.TaskCompletionRecordEntity
import com.hisham.todolist.data.local.entity.TaskEntity

@Database(
    entities = [TaskEntity::class, TaskCompletionRecordEntity::class],
    version = 4,
    exportSchema = false,
)
@TypeConverters(
    DayOfWeekSetConverter::class,
    TaskCategoryConverter::class,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskCompletionRecordDao(): TaskCompletionRecordDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE tasks ADD COLUMN is_progress_enabled INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_completion_records (
                        task_id INTEGER NOT NULL,
                        occurrence_epoch_day INTEGER NOT NULL,
                        completed_at INTEGER NOT NULL,
                        PRIMARY KEY(task_id, occurrence_epoch_day),
                        FOREIGN KEY(task_id) REFERENCES tasks(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_task_completion_records_task_id
                    ON task_completion_records(task_id)
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO task_completion_records(task_id, occurrence_epoch_day, completed_at)
                    SELECT
                        id,
                        CASE
                            WHEN state_date_epoch_day IS NOT NULL THEN state_date_epoch_day
                            ELSE CAST(updated_at / 86400000 AS INTEGER)
                        END,
                        updated_at
                    FROM tasks
                    WHERE is_completed = 1
                    """.trimIndent(),
                )
            }
        }
    }
}
