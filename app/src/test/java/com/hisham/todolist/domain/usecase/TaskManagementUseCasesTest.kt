package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.model.TaskCategory
import com.hisham.todolist.testdoubles.FakeTaskRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class TaskManagementUseCasesTest {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2026-04-21T08:00:00Z"),
        ZoneOffset.UTC,
    )
    private val currentEpochDay = LocalDate.now(fixedClock).toEpochDay()

    @Test
    fun `create task appends it at the end of the list`() = runTest {
        val repository = FakeTaskRepository(
            initialTasks = listOf(
                Task(id = 1L, title = "Existing", position = 0),
                Task(id = 2L, title = "Later", position = 4),
            ),
        )

        val useCase = CreateTaskUseCase(repository, fixedClock)

        val created = useCase(
            Task(
                title = "Created",
                category = TaskCategory.PERSONAL,
                iconName = "task_alt",
            ),
        )

        assertEquals(5, created.position)
        assertEquals(
            listOf("Existing", "Later", "Created"),
            repository.tasks.value.map(Task::title)
        )
    }

    @Test
    fun `update task preserves createdAt and updates content`() = runTest {
        val repository = FakeTaskRepository(
            initialTasks = listOf(
                Task(
                    id = 8L,
                    title = "Original",
                    category = TaskCategory.WORK,
                    iconName = "work_outline",
                    progress = 10,
                    createdAt = 111L,
                    updatedAt = 111L,
                ),
            ),
        )

        UpdateTaskUseCase(repository, fixedClock).invoke(
            repository.tasks.value.single().copy(
                title = "Updated",
                progress = 55,
                isRecurrent = true,
                recurrenceDays = setOf(DayOfWeek.MONDAY),
            ),
        )

        val updated = repository.tasks.value.single()
        assertEquals("Updated", updated.title)
        assertEquals(55, updated.progress)
        assertEquals(111L, updated.createdAt)
        assertEquals(fixedClock.millis(), updated.updatedAt)
    }

    @Test
    fun `update task marks recurrent progress changes for the current day`() = runTest {
        val repository = FakeTaskRepository(
            initialTasks = listOf(
                Task(
                    id = 9L,
                    title = "Read",
                    isProgressEnabled = true,
                    progress = 20,
                    isRecurrent = true,
                    recurrenceDays = setOf(DayOfWeek.MONDAY),
                    stateDateEpochDay = currentEpochDay - 1,
                    createdAt = 55L,
                    updatedAt = 55L,
                ),
            ),
        )

        UpdateTaskUseCase(repository, fixedClock).invoke(
            repository.tasks.value.single().copy(progress = 65),
        )

        val updated = repository.tasks.value.single()
        assertEquals(65, updated.progress)
        assertEquals(currentEpochDay, updated.stateDateEpochDay)
    }

    @Test
    fun `delete task removes it from repository`() = runTest {
        val repository = FakeTaskRepository(
            initialTasks = listOf(Task(id = 4L, title = "Delete me")),
        )

        DeleteTaskUseCase(repository).invoke(4L)

        assertTrue(repository.tasks.value.isEmpty())
    }

    @Test
    fun `get task by id returns matching task or null`() = runTest {
        val repository = FakeTaskRepository(
            initialTasks = listOf(Task(id = 2L, title = "Target")),
        )

        val useCase = GetTaskByIdUseCase(repository)

        assertEquals("Target", useCase(2L)?.title)
        assertNull(useCase(99L))
    }
}
