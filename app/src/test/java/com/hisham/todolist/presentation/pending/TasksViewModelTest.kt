package com.hisham.todolist.presentation.pending

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.repository.TaskRepository
import com.hisham.todolist.domain.usecase.ObservePendingTaskSectionsUseCase
import com.hisham.todolist.domain.usecase.ReorderTasksUseCase
import com.hisham.todolist.domain.usecase.ToggleTaskCompletionUseCase
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
import java.time.LocalDate
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
                    Task(
                        id = 1L,
                        title = "Hidden done",
                        isCompleted = true,
                        position = 1,
                        updatedAt = 0L
                    ),
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
                viewModel.uiState.value.tasks.map(TaskListItemUiModel::title),
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `keeps recurring tasks hidden when the weekday does not match`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeTaskRepository(
                initialTasks = listOf(
                    Task(
                        id = 1L,
                        title = "Recurring later",
                        isRecurrent = true,
                        recurrenceDays = setOf(DayOfWeek.WEDNESDAY),
                    ),
                ),
            )

            val viewModel = viewModelFor(repository)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.tasks.isEmpty())
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
                clock = fixedClock,
            )

            val viewModel = viewModelFor(repository)
            advanceUntilIdle()

            viewModel.onToggleTask(taskId = 1L, isCompleted = true)
            advanceUntilIdle()

            assertEquals(
                listOf("Second"),
                viewModel.uiState.value.tasks.map(TaskListItemUiModel::title),
            )
            assertEquals(
                listOf("First"),
                viewModel.uiState.value.completedTasks.map(TaskListItemUiModel::title),
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `shows tasks completed today in the collapsed completed list`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeTaskRepository(
                initialTasks = listOf(
                    Task(
                        id = 10L,
                        title = "Completed today",
                        isCompleted = true,
                        updatedAt = fixedClock.millis(),
                    ),
                    Task(
                        id = 11L,
                        title = "Completed earlier",
                        isCompleted = true,
                        updatedAt = fixedClock.millis() - 86_400_000L,
                    ),
                ),
                clock = fixedClock,
            )

            val viewModel = viewModelFor(repository)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.tasks.isEmpty())
            assertEquals(
                listOf("Completed today"),
                viewModel.uiState.value.completedTasks.map(TaskListItemUiModel::title),
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `exposes progress enabled state for pending tasks`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeTaskRepository(
                initialTasks = listOf(
                    Task(
                        id = 4L,
                        title = "Progress task",
                        isProgressEnabled = true,
                        progress = 25,
                    ),
                ),
            )

            val viewModel = viewModelFor(repository)
            advanceUntilIdle()

            val task = viewModel.uiState.value.tasks.single()
            assertTrue(task.isProgressEnabled)
            assertEquals(25, task.progress)
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
                viewModel.uiState.value.tasks.map(TaskListItemUiModel::title),
            )

            viewModel.onReorderCommit(viewModel.uiState.value.tasks.map(TaskListItemUiModel::id))
            advanceUntilIdle()

            assertEquals(
                listOf(2L, 3L, 1L),
                repository.tasks.value.sortedBy(Task::position).map(Task::id),
            )
            assertTrue(viewModel.uiState.value.draggingTaskId == null)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun viewModelFor(repository: FakeTaskRepository): TasksViewModel =
        TasksViewModel(
            observePendingTaskSectionsUseCase = ObservePendingTaskSectionsUseCase(
                repository,
                fixedClock
            ),
            toggleTaskCompletionUseCase = ToggleTaskCompletionUseCase(repository),
            reorderTasksUseCase = ReorderTasksUseCase(repository),
        )

    private class FakeTaskRepository(
        initialTasks: List<Task>,
        private val clock: Clock = Clock.systemUTC(),
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
            val currentEpochDay = LocalDate.now(clock).toEpochDay()
            tasks.value = tasks.value.map { task ->
                if (task.id == taskId) {
                    task.copy(
                        isCompleted = isCompleted,
                        updatedAt = clock.millis(),
                        stateDateEpochDay = if (task.isRecurrent) currentEpochDay else task.stateDateEpochDay,
                    )
                } else {
                    task
                }
            }.sortedWith(compareBy<Task> { it.position }.thenBy { it.id })
        }

        override suspend fun updateTaskProgress(taskId: Long, progress: Int) = Unit

        override suspend fun reorderTasks(taskIdsInOrder: List<Long>) {
            val currentById = tasks.value.associateBy(Task::id)
            tasks.value = taskIdsInOrder.mapIndexedNotNull { index, taskId ->
                currentById[taskId]?.copy(position = index)
            }
        }
    }
}
