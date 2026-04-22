package com.hisham.todolist.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hisham.todolist.data.local.database.AppDatabase
import com.hisham.todolist.data.local.entity.TaskEntity
import com.hisham.todolist.domain.model.TaskCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek

@RunWith(RobolectricTestRunner::class)
class TaskDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = database.taskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `observeTasks returns tasks ordered by position and id`() = runTest {
        taskDao.upsertTask(taskEntity(id = 2L, title = "Second", position = 1))
        taskDao.upsertTask(taskEntity(id = 1L, title = "First", position = 0))

        val tasks = taskDao.observeTasks().first()

        assertEquals(listOf(1L, 2L), tasks.map(TaskEntity::id))
    }

    @Test
    fun `update queries persist completion progress and state date`() = runTest {
        taskDao.upsertTask(
            taskEntity(
                id = 3L,
                title = "Recurring task",
                isRecurrent = true,
                recurrenceDays = setOf(DayOfWeek.TUESDAY),
            ),
        )

        taskDao.updateCompletion(
            taskId = 3L,
            isCompleted = true,
            updatedAt = 50L,
            stateDateEpochDay = 20_001L,
        )
        taskDao.updateProgress(
            taskId = 3L,
            progress = 65,
            updatedAt = 60L,
            stateDateEpochDay = 20_001L,
        )
        taskDao.updatePosition(
            taskId = 3L,
            position = 4,
            updatedAt = 70L,
        )

        val stored = taskDao.getTaskById(3L)

        requireNotNull(stored)
        assertEquals(true, stored.isCompleted)
        assertEquals(65, stored.progress)
        assertEquals(4, stored.position)
        assertEquals(20_001L, stored.stateDateEpochDay)
        assertEquals(70L, stored.updatedAt)
    }

    @Test
    fun `deleteTaskById removes persisted task`() = runTest {
        taskDao.upsertTask(taskEntity(id = 4L, title = "Delete me"))

        taskDao.deleteTaskById(4L)

        assertNull(taskDao.getTaskById(4L))
    }

    private fun taskEntity(
        id: Long,
        title: String,
        position: Int = 0,
        isRecurrent: Boolean = false,
        recurrenceDays: Set<DayOfWeek> = emptySet(),
        isProgressEnabled: Boolean = false,
    ): TaskEntity = TaskEntity(
        id = id,
        title = title,
        isCompleted = false,
        isProgressEnabled = isProgressEnabled,
        progress = 0,
        isRecurrent = isRecurrent,
        recurrenceDays = recurrenceDays,
        stateDateEpochDay = null,
        category = TaskCategory.PERSONAL,
        iconName = null,
        position = position,
        createdAt = 10L,
        updatedAt = 10L,
    )
}
