package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.repository.TaskRepository
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObservePendingTaskSectionsUseCaseTest {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2026-04-21T08:00:00Z"),
        ZoneOffset.UTC,
    )
    private val currentEpochDay = LocalDate.now(fixedClock).toEpochDay()

    @Test
    fun `returns active and completed today sections`() = runTest {
        val useCase = ObservePendingTaskSectionsUseCase(
            taskRepository = FakeTaskRepository(
                listOf(
                    Task(id = 1L, title = "Active", position = 0),
                    Task(
                        id = 2L,
                        title = "Completed today",
                        isCompleted = true,
                        updatedAt = fixedClock.millis(),
                        position = 1,
                    ),
                    Task(
                        id = 3L,
                        title = "Recurring completed today",
                        isCompleted = true,
                        isRecurrent = true,
                        recurrenceDays = setOf(DayOfWeek.TUESDAY),
                        stateDateEpochDay = currentEpochDay,
                        updatedAt = fixedClock.millis(),
                        position = 2,
                    ),
                ),
            ),
            clock = fixedClock,
        )

        val result = useCase().first()

        assertEquals(listOf("Active"), result.activeTasks.map(Task::title))
        assertEquals(
            listOf("Completed today", "Recurring completed today"),
            result.completedTasks.map(Task::title),
        )
    }

    private class FakeTaskRepository(
        private val tasks: List<Task>,
    ) : TaskRepository {
        override fun observeTasks(): Flow<List<Task>> = flowOf(tasks)

        override suspend fun getTask(taskId: Long): Task? = null

        override suspend fun upsertTask(task: Task) = Unit

        override suspend fun deleteTask(taskId: Long) = Unit

        override suspend fun updateTaskCompletion(taskId: Long, isCompleted: Boolean) = Unit

        override suspend fun updateTaskProgress(taskId: Long, progress: Int) = Unit

        override suspend fun reorderTasks(taskIdsInOrder: List<Long>) = Unit
    }
}
