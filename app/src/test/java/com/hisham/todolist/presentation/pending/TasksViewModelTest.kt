package com.hisham.todolist.presentation.pending

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.repository.TaskRepository
import com.hisham.todolist.domain.usecase.CreateQuickTaskUseCase
import com.hisham.todolist.domain.usecase.GetPendingTasksUseCase
import com.hisham.todolist.domain.usecase.ReorderTasksUseCase
import com.hisham.todolist.domain.usecase.ToggleTaskCompletionUseCase
import com.hisham.todolist.domain.usecase.UpdateTaskProgressUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2026-04-21T08:00:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun `exposes the filtered pending task list`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeTaskRepository(
                initialTasks = listOf(
                    Task(id = 1L, title = "Hidden done", isCompleted = true, position = 1),
                    Task(id = 2L, title = "Visible", position = 0),
                    Task(
                        id = 3L,
                        title = "Recurring today",
                        isRecurrent = true,
                        recurrenceDays = setOf(DayOfWeek.TUESDAY),
                        position = 2,
                    ),
                ),
            )

            val viewModel = viewModelFor(repository)

            advanceUntilIdle()

            assertEquals(
                listOf("Visible", "Recurring today"),
                viewModel.uiState.value.tasks.map(TaskListItemUiModel::title)
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `creates quick tasks and clears the composer`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeTaskRepository(
                initialTasks = listOf(Task(id = 1L, title = "Existing", position = 0)),
            )

            val viewModel = viewModelFor(repository)

            viewModel.onQuickAddTextChange("  New item  ")
            viewModel.onQuickAddSubmit()
            advanceUntilIdle()

            assertEquals("", viewModel.uiState.value.quickAddText)
            assertEquals(listOf("Existing", "New item"), repository.tasks.value.map(Task::title))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `removes a task from the pending list when it is completed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeTaskRepository(
                initialTasks = listOf(
                    Task(id = 1L, title = "First", position = 0),
                    Task(id = 2L, title = "Second", position = 1),
                ),
            )

            val viewModel = viewModelFor(repository)
            advanceUntilIdle()

            viewModel.onToggleTask(taskId = 1L, isCompleted = true)
            advanceUntilIdle()

            assertEquals(
                listOf("Second"),
                viewModel.uiState.value.tasks.map(TaskListItemUiModel::title)
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `preview reorder updates the visible order and commit persists it`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeTaskRepository(
                initialTasks = listOf(
                    Task(id = 1L, title = "First", position = 0),
                    Task(id = 2L, title = "Second", position = 1),
                    Task(id = 3L, title = "Third", position = 2),
                ),
            )

            val viewModel = viewModelFor(repository)
            advanceUntilIdle()

            viewModel.onDragStart(taskId = 1L)
            viewModel.onReorderPreview(fromIndex = 0, toIndex = 2)
            advanceUntilIdle()

            assertEquals(
                listOf("Second", "Third", "First"),
                viewModel.uiState.value.tasks.map(TaskListItemUiModel::title)
            )

            viewModel.onReorderCommit(viewModel.uiState.value.tasks.map(TaskListItemUiModel::id))
            advanceUntilIdle()

            assertEquals(
                listOf(2L, 3L, 1L),
                repository.tasks.value.sortedBy(Task::position).map(Task::id)
            )
            assertTrue(viewModel.uiState.value.draggingTaskId == null)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun viewModelFor(repository: FakeTaskRepository): TasksViewModel =
        TasksViewModel(
            getPendingTasksUseCase = GetPendingTasksUseCase(
                taskRepository = repository,
                clock = fixedClock,
            ),
            toggleTaskCompletionUseCase = ToggleTaskCompletionUseCase(repository),
            updateTaskProgressUseCase = UpdateTaskProgressUseCase(repository),
            reorderTasksUseCase = ReorderTasksUseCase(repository),
            createQuickTaskUseCase = CreateQuickTaskUseCase(
                taskRepository = repository,
                clock = fixedClock,
            ),
        )

    private class FakeTaskRepository(
        initialTasks: List<Task>,
    ) : TaskRepository {
        val tasks =
            MutableStateFlow(initialTasks.sortedWith(compareBy<Task> { it.position }.thenBy { it.id }))

        override fun observeTasks(): Flow<List<Task>> = tasks

        override suspend fun getTask(taskId: Long): Task? =
            tasks.value.firstOrNull { it.id == taskId }

        override suspend fun upsertTask(task: Task) {
            val nextId = if (task.id == 0L) {
                (tasks.value.maxOfOrNull(Task::id) ?: 0L) + 1
            } else {
                task.id
            }
            val updated = tasks.value.filterNot { it.id == nextId } + task.copy(id = nextId)
            tasks.value = updated.sortedWith(compareBy<Task> { it.position }.thenBy { it.id })
        }

        override suspend fun deleteTask(taskId: Long) {
            tasks.value = tasks.value.filterNot { it.id == taskId }
        }

        override suspend fun updateTaskCompletion(taskId: Long, isCompleted: Boolean) {
            tasks.value = tasks.value.map { task ->
                if (task.id == taskId) {
                    task.copy(isCompleted = isCompleted)
                } else {
                    task
                }
            }.sortedWith(compareBy<Task> { it.position }.thenBy { it.id })
        }

        override suspend fun updateTaskProgress(taskId: Long, progress: Int) {
            tasks.value = tasks.value.map { task ->
                if (task.id == taskId) {
                    task.copy(progress = progress.coerceIn(0, 100))
                } else {
                    task
                }
            }.sortedWith(compareBy<Task> { it.position }.thenBy { it.id })
        }

        override suspend fun reorderTasks(taskIdsInOrder: List<Long>) {
            val currentById = tasks.value.associateBy(Task::id)
            tasks.value = taskIdsInOrder.mapIndexedNotNull { index, taskId ->
                currentById[taskId]?.copy(position = index)
            }
        }
    }
}
