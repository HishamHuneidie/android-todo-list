package com.hisham.todolist.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hisham.todolist.data.local.dao.TaskCompletionRecordDao
import com.hisham.todolist.data.local.dao.TaskDao
import com.hisham.todolist.data.local.database.AppDatabase
import com.hisham.todolist.data.local.entity.TaskEntity
import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.model.TaskCategory
import com.hisham.todolist.domain.usecase.CreateTaskUseCase
import com.hisham.todolist.domain.usecase.ReorderTasksUseCase
import com.hisham.todolist.domain.usecase.ToggleTaskCompletionUseCase
import com.hisham.todolist.domain.usecase.UpdateTaskProgressUseCase
import com.hisham.todolist.domain.usecase.UpdateTaskUseCase
import com.hisham.todolist.testdoubles.MutableClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class TaskUseCasesIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var taskCompletionRecordDao: TaskCompletionRecordDao
    private lateinit var repository: TaskRepositoryImpl
    private lateinit var clock: MutableClock

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = database.taskDao()
        taskCompletionRecordDao = database.taskCompletionRecordDao()
        clock = MutableClock(
            instant = Instant.parse("2026-04-21T08:00:00Z"),
            zone = ZoneOffset.UTC,
        )
        repository = TaskRepositoryImpl(
            database = database,
            taskDao = taskDao,
            taskCompletionRecordDao = taskCompletionRecordDao,
            clock = clock,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `create and update use cases persist changes through room`() = runTest {
        val createTaskUseCase = CreateTaskUseCase(repository, clock)
        val updateTaskUseCase = UpdateTaskUseCase(repository, clock)

        createTaskUseCase(
            Task(
                title = "Deep work",
                category = TaskCategory.WORK,
                iconName = "work_outline",
            ),
        )

        val created = repository.observeTasks().first().single()
        assertEquals("Deep work", created.title)
        assertEquals(0, created.position)
        assertEquals(TaskCategory.WORK, created.category)

        updateTaskUseCase(
            created.copy(
                title = "Deep work revised",
                isProgressEnabled = true,
                progress = 40,
            ),
        )

        val updated = repository.getTask(created.id)
        requireNotNull(updated)
        assertEquals("Deep work revised", updated.title)
        assertTrue(updated.isProgressEnabled)
        assertEquals(40, updated.progress)
        assertEquals(created.createdAt, updated.createdAt)
        assertEquals(clock.millis(), updated.updatedAt)
    }

    @Test
    fun `toggle completion and progress use cases persist completion record and state date`() =
        runTest {
            taskDao.upsertTask(
                taskEntity(
                    id = 1L,
                    title = "Workout",
                    isRecurrent = true,
                    recurrenceDays = setOf(DayOfWeek.MONDAY),
                    isProgressEnabled = true,
                ),
            )

            ToggleTaskCompletionUseCase(repository)(
                taskId = 1L,
                isCompleted = true,
            )
            UpdateTaskProgressUseCase(repository)(
                taskId = 1L,
                progress = 150,
            )

            val stored = repository.getTask(1L)
            val records = repository.observeTaskCompletionRecords().first()
            val currentEpochDay = LocalDate.now(clock).toEpochDay()

            requireNotNull(stored)
            assertTrue(stored.isCompleted)
            assertEquals(100, stored.progress)
            assertEquals(currentEpochDay, stored.stateDateEpochDay)
            assertEquals(
                listOf(currentEpochDay),
                records.map { it.occurrenceEpochDay },
            )
        }

    @Test
    fun `reorder use case persists order across repository reads`() = runTest {
        taskDao.upsertTask(taskEntity(id = 1L, title = "First", position = 0))
        taskDao.upsertTask(taskEntity(id = 2L, title = "Second", position = 1))
        taskDao.upsertTask(taskEntity(id = 3L, title = "Third", position = 2))

        ReorderTasksUseCase(repository)(listOf(3L, 1L, 2L))

        val reordered = repository.observeTasks().first()
        assertEquals(listOf(3L, 1L, 2L), reordered.map(Task::id))
        assertEquals(listOf(0, 1, 2), reordered.map(Task::position))
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
