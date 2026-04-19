package com.hisham.todolist.presentation.loader

import com.hisham.todolist.domain.model.AuthState
import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.model.UserSession
import com.hisham.todolist.domain.repository.AuthRepository
import com.hisham.todolist.domain.repository.TaskRepository
import com.hisham.todolist.domain.usecase.CheckUserSessionUseCase
import com.hisham.todolist.domain.usecase.InitializeAppUseCase
import com.hisham.todolist.domain.usecase.PrepareTasksForCurrentDayUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoaderViewModelTest {
    @Test
    fun `emits authenticated when initialization returns a session`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val session = UserSession(
                userId = "user-1",
                displayName = "Hisham",
                email = "hisham@example.com",
                photoUrl = null,
            )

            val viewModel = LoaderViewModel(
                initializeAppUseCase = initializeAppUseCase(
                    session = session,
                    tasksFlow = flowOf(listOf(Task(id = 1L, title = "Daily review"))),
                ),
            )

            advanceUntilIdle()

            assertEquals(LoaderStatus.AUTHENTICATED, viewModel.uiState.value.status)
            assertNull(viewModel.uiState.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `emits unauthenticated when initialization returns no session`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = LoaderViewModel(
                initializeAppUseCase = initializeAppUseCase(
                    session = null,
                    tasksFlow = flowOf(listOf(Task(id = 1L, title = "Backlog cleanup"))),
                ),
            )

            advanceUntilIdle()

            assertEquals(LoaderStatus.UNAUTHENTICATED, viewModel.uiState.value.status)
            assertNull(viewModel.uiState.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `keeps authenticated destination when task preparation fails`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val session = UserSession(
                userId = "user-1",
                displayName = "Hisham",
                email = "hisham@example.com",
                photoUrl = null,
            )

            val viewModel = LoaderViewModel(
                initializeAppUseCase = initializeAppUseCase(
                    session = session,
                    tasksFlow = flow {
                        throw IllegalStateException("db unavailable")
                    },
                ),
            )

            advanceUntilIdle()

            assertEquals(LoaderStatus.AUTHENTICATED, viewModel.uiState.value.status)
            assertNull(viewModel.uiState.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun initializeAppUseCase(
        session: UserSession?,
        tasksFlow: Flow<List<Task>>,
    ): InitializeAppUseCase = InitializeAppUseCase(
        checkUserSessionUseCase = CheckUserSessionUseCase(FakeAuthRepository(session = session)),
        prepareTasksForCurrentDayUseCase = PrepareTasksForCurrentDayUseCase(
            taskRepository = FakeTaskRepository(tasksFlow = tasksFlow),
        ),
    )

    private class FakeAuthRepository(
        private val session: UserSession?,
    ) : AuthRepository {
        override fun observeAuthState(): Flow<AuthState> = emptyFlow()

        override suspend fun getCurrentSession(): UserSession? = session

        override suspend fun signInWithGoogle() = error("Not used in test")

        override suspend fun signOut() = Unit
    }

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
