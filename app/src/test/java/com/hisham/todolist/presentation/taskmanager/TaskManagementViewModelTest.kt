package com.hisham.todolist.presentation.taskmanager

import com.hisham.todolist.core.state.AppRuntimeState
import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.model.TaskCategory
import com.hisham.todolist.domain.usecase.CreateQuickTaskUseCase
import com.hisham.todolist.domain.usecase.CreateTaskUseCase
import com.hisham.todolist.domain.usecase.DeleteTaskUseCase
import com.hisham.todolist.domain.usecase.GetTaskByIdUseCase
import com.hisham.todolist.domain.usecase.ObserveTasksUseCase
import com.hisham.todolist.domain.usecase.UpdateTaskUseCase
import com.hisham.todolist.testdoubles.FakeTaskRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
                    Task(
                        id = 2L,
                        title = "Done recently",
                        isCompleted = true,
                        updatedAt = fixedClock.millis(),
                    ),
                    Task(
                        id = 3L,
                        title = "Done earlier",
                        isCompleted = true,
                        updatedAt = fixedClock.millis() - (8 * 86_400_000L),
                    ),
                ),
            )
            val viewModel = viewModelFor(repository)
            advanceUntilIdle()

            assertEquals(
                listOf("Active"),
                viewModel.uiState.value.tasks.map(TaskManagementListItemUiModel::title)
            )
            assertEquals(
                listOf("Done recently"),
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

    @Test
    fun `shows error and closes sheet when selected task no longer exists`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = viewModelFor(FakeTaskRepository())

            viewModel.onTaskClick(999L)
            advanceUntilIdle()

            assertEquals(
                "La tarea seleccionada ya no existe.",
                viewModel.uiState.value.errorMessage
            )
            assertFalse(viewModel.uiState.value.isSheetVisible)
            assertTrue(viewModel.uiState.value.sheetMode is TaskSheetMode.Create)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `exposes saving state while persisting a task`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val gate = CompletableDeferred<Unit>()
        try {
            val repository = FakeTaskRepository().apply {
                upsertAction = { gate.await() }
            }
            val viewModel = viewModelFor(repository)

            viewModel.onCreateTaskClick()
            viewModel.onTitleChange("Long running save")
            viewModel.onSaveClick()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isSaving)

            gate.complete(Unit)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSaving)
            assertFalse(viewModel.uiState.value.isSheetVisible)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `exposes deleting state while removing a task`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val gate = CompletableDeferred<Unit>()
        try {
            val repository = FakeTaskRepository(
                initialTasks = listOf(Task(id = 4L, title = "Disposable")),
            ).apply {
                deleteAction = { gate.await() }
            }
            val viewModel = viewModelFor(repository)

            viewModel.onTaskClick(4L)
            advanceUntilIdle()
            viewModel.onDeleteClick()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isDeleting)

            gate.complete(Unit)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isDeleting)
            assertTrue(repository.tasks.value.isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `exposes quick create submission state while request is running`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val gate = CompletableDeferred<Unit>()
        try {
            val repository = FakeTaskRepository().apply {
                upsertAction = { gate.await() }
            }
            val viewModel = viewModelFor(repository)

            viewModel.onQuickCreateTextChange("Queued task")
            viewModel.onQuickCreateSubmit()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isSubmittingQuickCreate)

            gate.complete(Unit)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSubmittingQuickCreate)
            assertEquals("", viewModel.uiState.value.quickCreateText)
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
            clock = fixedClock,
        )
}
