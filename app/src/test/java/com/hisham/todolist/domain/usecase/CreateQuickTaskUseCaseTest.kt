package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CreateQuickTaskUseCaseTest {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2026-04-21T08:00:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun `ignores blank titles`() = runTest {
        val repository = FakeTaskRepository()

        val result = CreateQuickTaskUseCase(
            taskRepository = repository,
            clock = fixedClock,
        ).invoke("   ")

        assertNull(result)
        assertTrue(repository.tasks.value.isEmpty())
    }

    @Test
    fun `creates task at the end of the current order`() = runTest {
        val repository = FakeTaskRepository(
            initialTasks = listOf(
                Task(id = 1L, title = "First", position = 0),
                Task(id = 2L, title = "Second", position = 4),
            ),
        )

        val result = CreateQuickTaskUseCase(
            taskRepository = repository,
            clock = fixedClock,
        ).invoke("  New task  ")

        requireNotNull(result)
        assertEquals("New task", result.title)
        assertEquals(false, result.isProgressEnabled)
        assertEquals(5, result.position)
        assertEquals(fixedClock.millis(), result.createdAt)
        assertEquals(listOf("First", "Second", "New task"), repository.tasks.value.map(Task::title))
    }

    private class FakeTaskRepository(
        initialTasks: List<Task> = emptyList(),
    ) : TaskRepository {
        val tasks =
            MutableStateFlow(initialTasks.sortedWith(compareBy<Task> { it.position }.thenBy { it.id }))

        override fun observeTasks(): Flow<List<Task>> = tasks

        override suspend fun getTask(taskId: Long): Task? =
            tasks.value.firstOrNull { it.id == taskId }

        override suspend fun upsertTask(task: Task) {
            val nextId = if (task.id == 0L) {
                (tasks.value.maxOfOrNull(Task::id) ?: 0L) + 1L
            } else {
                task.id
            }
            val updated = tasks.value.filterNot { it.id == nextId } + task.copy(id = nextId)
            tasks.value = updated.sortedWith(compareBy<Task> { it.position }.thenBy { it.id })
        }

        override suspend fun deleteTask(taskId: Long) = Unit

        override suspend fun updateTaskCompletion(taskId: Long, isCompleted: Boolean) = Unit

        override suspend fun updateTaskProgress(taskId: Long, progress: Int) = Unit

        override suspend fun reorderTasks(taskIdsInOrder: List<Long>) = Unit
    }
}
