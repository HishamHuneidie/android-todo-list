package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.repository.TaskRepository
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrepareTasksForCurrentDayUseCaseTest {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2026-04-21T08:00:00Z"),
        ZoneOffset.UTC,
    )

    private val currentEpochDay = LocalDate.now(fixedClock).toEpochDay()

    @Test
    fun `filters out completed non recurrent tasks`() = runTest {
        val pendingTask = Task(id = 1L, title = "Inbox zero", position = 0)
        val completedTask = Task(id = 2L, title = "Workout", isCompleted = true, position = 1)

        val result = useCaseFor(listOf(pendingTask, completedTask)).invoke()

        assertEquals(listOf(pendingTask), result)
    }

    @Test
    fun `includes recurrent tasks only on matching weekday`() = runTest {
        val matchingTask = Task(
            id = 1L,
            title = "Review backlog",
            isRecurrent = true,
            recurrenceDays = setOf(DayOfWeek.TUESDAY),
        )
        val nonMatchingTask = Task(
            id = 2L,
            title = "Weekly retro",
            isRecurrent = true,
            recurrenceDays = setOf(DayOfWeek.WEDNESDAY),
        )

        val result = useCaseFor(listOf(matchingTask, nonMatchingTask)).invoke()

        assertEquals(1, result.size)
        assertEquals(matchingTask.id, result.single().id)
    }

    @Test
    fun `resets stale recurrent task state for a new day`() = runTest {
        val staleTask = Task(
            id = 7L,
            title = "Practice guitar",
            isCompleted = true,
            progress = 80,
            isRecurrent = true,
            recurrenceDays = setOf(DayOfWeek.TUESDAY),
            stateDateEpochDay = currentEpochDay - 1,
        )

        val result = useCaseFor(listOf(staleTask)).invoke().single()

        assertFalse(result.isCompleted)
        assertEquals(0, result.progress)
        assertEquals(currentEpochDay, result.stateDateEpochDay)
    }

    @Test
    fun `excludes recurrent task already completed today`() = runTest {
        val completedTodayTask = Task(
            id = 3L,
            title = "Morning planning",
            isCompleted = true,
            progress = 100,
            isRecurrent = true,
            recurrenceDays = setOf(DayOfWeek.TUESDAY),
            stateDateEpochDay = currentEpochDay,
        )

        val result = useCaseFor(listOf(completedTodayTask)).invoke()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `keeps partial progress for recurrent task already started today`() = runTest {
        val partialTask = Task(
            id = 4L,
            title = "Read 20 pages",
            progress = 40,
            isRecurrent = true,
            recurrenceDays = setOf(DayOfWeek.TUESDAY),
            stateDateEpochDay = currentEpochDay,
        )

        val result = useCaseFor(listOf(partialTask)).invoke().single()

        assertEquals(40, result.progress)
        assertFalse(result.isCompleted)
        assertEquals(currentEpochDay, result.stateDateEpochDay)
    }

    private fun useCaseFor(tasks: List<Task>): PrepareTasksForCurrentDayUseCase =
        PrepareTasksForCurrentDayUseCase(
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
