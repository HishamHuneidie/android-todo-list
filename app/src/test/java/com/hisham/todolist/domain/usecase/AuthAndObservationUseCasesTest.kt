package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.AuthState
import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.model.ThemeMode
import com.hisham.todolist.domain.model.UserSession
import com.hisham.todolist.testdoubles.FakeAuthRepository
import com.hisham.todolist.testdoubles.FakeSettingsRepository
import com.hisham.todolist.testdoubles.FakeTaskRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AuthAndObservationUseCasesTest {

    @Test
    fun `sign in with Google delegates to repository and returns its result`() = runTest {
        val session = UserSession(
            userId = "user-1",
            displayName = "Hisham",
            email = "hisham@example.com",
        )
        val repository = FakeAuthRepository().apply {
            signInResult = Result.success(session)
        }

        val result = SignInWithGoogleUseCase(repository)()

        assertEquals(1, repository.signInCalls)
        assertEquals(Result.success(session), result)
        assertEquals(session, repository.session)
    }

    @Test
    fun `sign out delegates to repository`() = runTest {
        val repository = FakeAuthRepository(
            initialSession = UserSession(
                userId = "user-1",
                displayName = "Hisham",
                email = "hisham@example.com",
            ),
        )

        SignOutUseCase(repository)()

        assertEquals(1, repository.signOutCalls)
        assertEquals(null, repository.session)
    }

    @Test
    fun `check user session returns repository session`() = runTest {
        val session = UserSession(
            userId = "user-1",
            displayName = "Hisham",
            email = "hisham@example.com",
        )
        val repository = FakeAuthRepository(initialSession = session)

        val result = CheckUserSessionUseCase(repository)()

        assertEquals(1, repository.getCurrentSessionCalls)
        assertSame(session, result)
    }

    @Test
    fun `observe auth state re emits repository changes`() = runTest {
        val session = UserSession(
            userId = "user-1",
            displayName = "Hisham",
            email = "hisham@example.com",
        )
        val repository = FakeAuthRepository()
        val emissions = async {
            ObserveAuthStateUseCase(repository)().take(2).toList()
        }
        runCurrent()

        repository.emitSession(session)

        assertEquals(
            listOf(
                AuthState.Unauthenticated,
                AuthState.Authenticated(session),
            ),
            emissions.await(),
        )
    }

    @Test
    fun `observe tasks re emits repository changes`() = runTest {
        val task = Task(id = 1L, title = "Task")
        val repository = FakeTaskRepository()
        val emissions = async {
            ObserveTasksUseCase(repository)().take(2).toList()
        }
        runCurrent()

        repository.emitTasks(listOf(task))

        assertEquals(
            listOf(
                emptyList(),
                listOf(task),
            ),
            emissions.await(),
        )
    }

    @Test
    fun `observe theme mode re emits repository changes`() = runTest {
        val repository = FakeSettingsRepository()
        val emissions = async {
            ObserveThemeModeUseCase(repository)().take(2).toList()
        }
        runCurrent()

        repository.themeMode.value = ThemeMode.DARK

        assertEquals(
            listOf(ThemeMode.SYSTEM, ThemeMode.DARK),
            emissions.await(),
        )
    }
}
