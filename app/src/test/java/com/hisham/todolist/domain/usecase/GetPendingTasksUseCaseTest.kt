package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class GetPendingTasksUseCaseTest {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2026-04-21T08:00:00Z"),
        ZoneOffset.UTC,
    )

    private val currentEpochDay = LocalDate.now(fixedClock).toEpochDay()

    @Test
    fun `returns unified pending list ordered by position`() = runTest {
        val tasks = listOf(
            Task(id = 3L, title = "Done", isCompleted = true, position = 3),
            Task(
                id = 4L,
                title = "Weekly review",
                isRecurrent = true,
                recurrenceDays = setOf(DayOfWeek.TUESDAY),
                position = 0,
            ),
            Task(id = 2L, title = "Inbox zero", position = 2),
            Task(id = 1L, title = "Focus block", position = 1),
        )

        val result = useCaseFor(tasks).invoke().first()

        assertEquals(listOf(4L, 1L, 2L), result.map(Task::id))
    }

    @Test
    fun `resets stale recurrent tasks for the current day`() = runTest {
        val staleTask = Task(
            id = 7L,
            title = "Read 20 pages",
            isCompleted = true,
            progress = 70,
            isRecurrent = true,
            recurrenceDays = setOf(DayOfWeek.TUESDAY),
            stateDateEpochDay = currentEpochDay - 1,
        )

        val result = useCaseFor(listOf(staleTask)).invoke().first().single()

        assertFalse(result.isCompleted)
        assertEquals(0, result.progress)
        assertEquals(currentEpochDay, result.stateDateEpochDay)
    }

    private fun useCaseFor(tasks: List<Task>): GetPendingTasksUseCase =
        GetPendingTasksUseCase(
            taskRepository = FakeTaskRepository(flowOf(tasks)),
            clock = fixedClock,
        )

    private class FakeTaskRepository(
        private val tasksFlow: Flow<List<Task>>,
    ) : TaskRepository {
        override fun observeTasks(): Flow<List<Task>> = tasksFlow

        override suspend fun getTask(taskId: Long): Task? = null

        override suspend fun upsertTask(task: Task) = Unit

        override suspend fun deleteTask(taskId: Long) = Unit

        override suspend fun updateTaskCompletion(taskId: Long, isCompleted: Boolean) = Unit

        override suspend fun updateTaskProgress(taskId: Long, progress: Int) = Unit

        override suspend fun reorderTasks(taskIdsInOrder: List<Long>) = Unit
    }
}
