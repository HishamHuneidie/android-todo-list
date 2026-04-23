package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.testdoubles.FakeTaskRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskMutationDelegationUseCasesTest {

    @Test
    fun `toggle task completion delegates task id and completion state`() = runTest {
        val repository = FakeTaskRepository(
            initialTasks = listOf(Task(id = 9L, title = "Workout")),
        )

        ToggleTaskCompletionUseCase(repository)(
            taskId = 9L,
            isCompleted = true,
        )

        assertEquals(
            listOf(FakeTaskRepository.CompletionUpdate(taskId = 9L, isCompleted = true)),
            repository.completionUpdates,
        )
        assertEquals(true, repository.tasks.value.single().isCompleted)
    }

    @Test
    fun `update task progress clamps value before delegating`() = runTest {
        val repository = FakeTaskRepository(
            initialTasks = listOf(Task(id = 7L, title = "Read", isProgressEnabled = true)),
        )

        UpdateTaskProgressUseCase(repository)(
            taskId = 7L,
            progress = 150,
        )

        assertEquals(
            listOf(FakeTaskRepository.ProgressUpdate(taskId = 7L, progress = 100)),
            repository.progressUpdates,
        )
        assertEquals(100, repository.tasks.value.single().progress)
    }

    @Test
    fun `reorder tasks delegates full ordered list`() = runTest {
        val repository = FakeTaskRepository(
            initialTasks = listOf(
                Task(id = 1L, title = "First", position = 0),
                Task(id = 2L, title = "Second", position = 1),
                Task(id = 3L, title = "Third", position = 2),
            ),
        )

        ReorderTasksUseCase(repository)(listOf(3L, 1L, 2L))

        assertEquals(listOf(listOf(3L, 1L, 2L)), repository.reorderCalls)
        assertEquals(listOf(3L, 1L, 2L), repository.tasks.value.map(Task::id))
    }
}
