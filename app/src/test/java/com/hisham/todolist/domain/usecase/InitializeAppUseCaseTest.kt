package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.model.UserSession
import com.hisham.todolist.domain.repository.AuthRepository
import com.hisham.todolist.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InitializeAppUseCaseTest {

    @Test
    fun `returns session and prepared tasks when initialization succeeds`() = runTest {
        val session = UserSession(
            userId = "user-1",
            displayName = "Hisham",
            email = "hisham@example.com",
            photoUrl = null,
        )
        val task = Task(id = 1L, title = "Plan sprint")

        val useCase = InitializeAppUseCase(
            checkUserSessionUseCase = CheckUserSessionUseCase(FakeAuthRepository(session = session)),
            prepareTasksForCurrentDayUseCase = PrepareTasksForCurrentDayUseCase(
                taskRepository = FakeTaskRepository(tasksFlow = flowOf(listOf(task))),
            ),
        )

        val result = useCase()

        assertEquals(session, result.session)
        assertEquals(listOf(task), result.preparedTasks)
        assertFalse(result.completedWithWarnings)
    }

    @Test
    fun `returns unauthenticated state when session lookup fails`() = runTest {
        val task = Task(id = 1L, title = "Inbox zero")

        val useCase = InitializeAppUseCase(
            checkUserSessionUseCase = CheckUserSessionUseCase(FakeAuthRepository(shouldFailOnGetSession = true)),
            prepareTasksForCurrentDayUseCase = PrepareTasksForCurrentDayUseCase(
                taskRepository = FakeTaskRepository(tasksFlow = flowOf(listOf(task))),
            ),
        )

        val result = useCase()

        assertNull(result.session)
        assertEquals(listOf(task), result.preparedTasks)
        assertTrue(result.completedWithWarnings)
    }

    @Test
    fun `returns empty prepared tasks when task preparation fails`() = runTest {
        val session = UserSession(
            userId = "user-1",
            displayName = "Hisham",
            email = "hisham@example.com",
            photoUrl = null,
        )

        val useCase = InitializeAppUseCase(
            checkUserSessionUseCase = CheckUserSessionUseCase(FakeAuthRepository(session = session)),
            prepareTasksForCurrentDayUseCase = PrepareTasksForCurrentDayUseCase(
                taskRepository = FakeTaskRepository(
                    tasksFlow = flow {
                        throw IllegalStateException("db unavailable")
                    },
                ),
            ),
        )

        val result = useCase()

        assertEquals(session, result.session)
        assertTrue(result.preparedTasks.isEmpty())
        assertTrue(result.completedWithWarnings)
    }

    private class FakeAuthRepository(
        private val session: UserSession? = null,
        private val shouldFailOnGetSession: Boolean = false,
    ) : AuthRepository {
        override fun observeAuthState() = emptyFlow()

        override suspend fun getCurrentSession(): UserSession? {
            if (shouldFailOnGetSession) {
                throw IllegalStateException("session unavailable")
            }
            return session
        }

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
