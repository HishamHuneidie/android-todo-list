package com.hisham.todolist.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hisham.todolist.data.local.dao.TaskCompletionRecordDao
import com.hisham.todolist.data.local.dao.TaskDao
import com.hisham.todolist.data.local.database.AppDatabase
import com.hisham.todolist.data.local.entity.TaskEntity
import com.hisham.todolist.domain.model.TaskCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class TaskRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var taskCompletionRecordDao: TaskCompletionRecordDao
    private lateinit var clock: MutableClock
    private lateinit var repository: TaskRepositoryImpl

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
    fun `update task completion creates and removes record for non recurrent tasks`() = runTest {
        taskDao.upsertTask(taskEntity(id = 1L, title = "Invoice"))

        repository.updateTaskCompletion(taskId = 1L, isCompleted = true)

        var records = repository.observeTaskCompletionRecords().first()
        assertEquals(1, records.size)
        assertEquals(LocalDate.now(clock).toEpochDay(), records.single().occurrenceEpochDay)

        repository.updateTaskCompletion(taskId = 1L, isCompleted = false)

        records = repository.observeTaskCompletionRecords().first()
        assertEquals(emptyList<Long>(), records.map { it.taskId })
    }

    @Test
    fun `uncompleting a recurrent task removes only the current occurrence`() = runTest {
        taskDao.upsertTask(
            taskEntity(
                id = 2L,
                title = "Workout",
                isRecurrent = true,
                recurrenceDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
            ),
        )

        repository.updateTaskCompletion(taskId = 2L, isCompleted = true)
        clock.instant = Instant.parse("2026-04-22T08:00:00Z")
        repository.updateTaskCompletion(taskId = 2L, isCompleted = true)

        var records = repository.observeTaskCompletionRecords().first()
        assertEquals(
            listOf(
                LocalDate.of(2026, 4, 21).toEpochDay(),
                LocalDate.of(2026, 4, 22).toEpochDay(),
            ),
            records.map { it.occurrenceEpochDay },
        )

        repository.updateTaskCompletion(taskId = 2L, isCompleted = false)

        records = repository.observeTaskCompletionRecords().first()
        assertEquals(
            listOf(LocalDate.of(2026, 4, 21).toEpochDay()),
            records.map { it.occurrenceEpochDay },
        )
    }

    private fun taskEntity(
        id: Long,
        title: String,
        isRecurrent: Boolean = false,
        recurrenceDays: Set<DayOfWeek> = emptySet(),
    ): TaskEntity = TaskEntity(
        id = id,
        title = title,
        isCompleted = false,
        isProgressEnabled = false,
        progress = 0,
        isRecurrent = isRecurrent,
        recurrenceDays = recurrenceDays,
        stateDateEpochDay = null,
        category = TaskCategory.PERSONAL,
        iconName = null,
        position = 0,
        createdAt = 10L,
        updatedAt = 10L,
    )

    private class MutableClock(
        var instant: Instant,
        private val zone: ZoneId,
    ) : Clock() {
        override fun getZone(): ZoneId = zone

        override fun withZone(zone: ZoneId): Clock = MutableClock(instant, zone)

        override fun instant(): Instant = instant
    }
}
