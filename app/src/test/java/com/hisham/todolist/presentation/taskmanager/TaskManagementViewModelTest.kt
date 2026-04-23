package com.hisham.todolist.presentation.taskmanager

import com.hisham.todolist.core.state.AppRuntimeState
import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.model.TaskCategory
import com.hisham.todolist.domain.repository.TaskRepository
import com.hisham.todolist.domain.usecase.CreateQuickTaskUseCase
import com.hisham.todolist.domain.usecase.CreateTaskUseCase
import com.hisham.todolist.domain.usecase.DeleteTaskUseCase
import com.hisham.todolist.domain.usecase.GetTaskByIdUseCase
import com.hisham.todolist.domain.usecase.ObserveTasksUseCase
import com.hisham.todolist.domain.usecase.UpdateTaskUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TaskManagementViewModelTest {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2026-04-21T08:00:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun `opens create sheet from plus button`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = viewModelFor(FakeTaskRepository())

            viewModel.onCreateTaskClick()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isSheetVisible)
            assertTrue(viewModel.uiState.value.sheetMode is TaskSheetMode.Create)
            assertEquals(TaskCategory.PERSONAL, viewModel.uiState.value.formState.category)
            assertFalse(viewModel.uiState.value.formState.isProgressEnabled)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `loads selected task into edit sheet`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeTaskRepository(
                initialTasks = listOf(
                    Task(
                        id = 5L,
                        title = "Review metrics",
                        isProgressEnabled = true,
                        progress = 40,
                        category = TaskCategory.WORK,
                        isRecurrent = true,
                        recurrenceDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                        iconName = "mail_outline",
                    ),
                ),
            )
            val viewModel = viewModelFor(repository)

            viewModel.onTaskClick(5L)
            advanceUntilIdle()

            val uiState = viewModel.uiState.value
            assertTrue(uiState.sheetMode is TaskSheetMode.Edit)
            assertEquals("Review metrics", uiState.formState.title)
            assertTrue(uiState.formState.isProgressEnabled)
            assertEquals(40, uiState.formState.progressValue)
            assertTrue(DayOfWeek.MONDAY in uiState.formState.recurrenceDays)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `validates form before saving only for enabled sections`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = viewModelFor(FakeTaskRepository())

            viewModel.onCreateTaskClick()
            viewModel.onTitleChange("   ")
            viewModel.onRecurrenceToggle(true)
            viewModel.onSaveClick()
            advanceUntilIdle()

            val form = viewModel.uiState.value.formState
            assertEquals("El titulo es obligatorio.", form.titleError)
            assertEquals(null, form.progressError)
            assertEquals("Selecciona al menos un dia.", form.recurrenceError)
            assertTrue(viewModel.uiState.value.isSheetVisible)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `syncs progress text and slider when progress is enabled`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = viewModelFor(FakeTaskRepository())

            viewModel.onCreateTaskClick()
            viewModel.onProgressToggle(true)
            viewModel.onProgressTextChange("65")
            advanceUntilIdle()
            assertEquals(65, viewModel.uiState.value.formState.progressValue)

            viewModel.onProgressValueChange(82)
            advanceUntilIdle()
            assertEquals("82", viewModel.uiState.value.formState.progressText)
            assertEquals(82, viewModel.uiState.value.formState.progressValue)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `creates a task and closes the sheet`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeTaskRepository()
            val viewModel = viewModelFor(repository)

            viewModel.onCreateTaskClick()
            viewModel.onTitleChange("New managed task")
            viewModel.onCategorySelected(TaskCategory.HOME)
            viewModel.onIconSelected(TaskIconOption.HOME)
            viewModel.onProgressToggle(true)
            viewModel.onProgressTextChange("20")
            viewModel.onSaveClick()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSheetVisible)
            assertEquals("New managed task", repository.tasks.value.single().title)
            assertEquals(TaskCategory.HOME, repository.tasks.value.single().category)
            assertEquals("home", repository.tasks.value.single().iconName)
            assertTrue(repository.tasks.value.single().isProgressEnabled)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `creates quick task from management and clears the composer`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeTaskRepository()
            val viewModel = viewModelFor(repository)

            viewModel.onQuickCreateTextChange("  Quick task  ")
            viewModel.onQuickCreateSubmit()
            advanceUntilIdle()

            assertEquals("", viewModel.uiState.value.quickCreateText)
            assertEquals("Quick task", repository.tasks.value.single().title)
            assertFalse(repository.tasks.value.single().isProgressEnabled)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `separates completed tasks into the secondary list`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeTaskRepository(
                initialTasks = listOf(
                    Task(id = 1L, title = "Active"),
                    Task(id = 2L, title = "Done", isCompleted = true),
                ),
            )
            val viewModel = viewModelFor(repository)
            advanceUntilIdle()

            assertEquals(
                listOf("Active"),
                viewModel.uiState.value.tasks.map(TaskManagementListItemUiModel::title)
            )
            assertEquals(
                listOf("Done"),
                viewModel.uiState.value.completedTasks.map(TaskManagementListItemUiModel::title),
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `deletes selected task and closes the sheet`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeTaskRepository(
                initialTasks = listOf(Task(id = 3L, title = "Disposable")),
            )
            val viewModel = viewModelFor(repository)

            viewModel.onTaskClick(3L)
            advanceUntilIdle()
            viewModel.onDeleteClick()
            advanceUntilIdle()

            assertTrue(repository.tasks.value.isEmpty())
            assertFalse(viewModel.uiState.value.isSheetVisible)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `dismissing the sheet discards unsaved changes`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = viewModelFor(FakeTaskRepository())

            viewModel.onCreateTaskClick()
            viewModel.onTitleChange("Transient")
            viewModel.onProgressToggle(true)
            viewModel.onSheetDismiss()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSheetVisible)
            assertEquals("", viewModel.uiState.value.formState.title)
            assertFalse(viewModel.uiState.value.formState.isDirty)
            assertFalse(viewModel.uiState.value.formState.isProgressEnabled)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun viewModelFor(repository: FakeTaskRepository): TaskManagementViewModel =
        TaskManagementViewModel(
            observeTasksUseCase = ObserveTasksUseCase(repository),
            getTaskByIdUseCase = GetTaskByIdUseCase(repository),
            createTaskUseCase = CreateTaskUseCase(repository, fixedClock),
            updateTaskUseCase = UpdateTaskUseCase(repository, fixedClock),
            deleteTaskUseCase = DeleteTaskUseCase(repository),
            createQuickTaskUseCase = CreateQuickTaskUseCase(repository, fixedClock),
            appRuntimeState = AppRuntimeState(),
        )

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

        override suspend fun deleteTask(taskId: Long) {
            tasks.value = tasks.value.filterNot { it.id == taskId }
        }

        override suspend fun updateTaskCompletion(taskId: Long, isCompleted: Boolean) = Unit

        override suspend fun updateTaskProgress(taskId: Long, progress: Int) = Unit

        override suspend fun reorderTasks(taskIdsInOrder: List<Long>) = Unit
    }
}
