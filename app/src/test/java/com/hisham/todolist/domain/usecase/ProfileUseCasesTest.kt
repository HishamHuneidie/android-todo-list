package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.AuthState
import com.hisham.todolist.domain.model.ThemeMode
import com.hisham.todolist.domain.model.UserSession
import com.hisham.todolist.domain.repository.AuthRepository
import com.hisham.todolist.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileUseCasesTest {

    @Test
    fun `get user use case returns current session when available`() = runTest {
        val session = UserSession(
            userId = "user-1",
            displayName = "Hisham",
            email = "hisham@example.com",
        )
        val useCase = GetUserUseCase(FakeAuthRepository(session = session))

        assertEquals(session, useCase())
    }

    @Test
    fun `get user use case returns null when there is no session`() = runTest {
        val useCase = GetUserUseCase(FakeAuthRepository(session = null))

        assertNull(useCase())
    }

    @Test
    fun `change theme use case persists selected mode`() = runTest {
        val repository = FakeSettingsRepository(initialThemeMode = ThemeMode.SYSTEM)
        val useCase = ChangeThemeUseCase(repository)

        useCase(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, repository.themeMode.value)
    }

    private class FakeAuthRepository(
        private val session: UserSession?,
    ) : AuthRepository {
        override fun observeAuthState(): Flow<AuthState> = MutableStateFlow(
            session?.let(AuthState::Authenticated) ?: AuthState.Unauthenticated,
        )

        override suspend fun getCurrentSession(): UserSession? = session

        override suspend fun signInWithGoogle(): Result<UserSession> {
            error("Not used in this test")
        }

        override suspend fun signOut() = Unit
    }

    private class FakeSettingsRepository(
        initialThemeMode: ThemeMode,
    ) : SettingsRepository {
        val themeMode = MutableStateFlow(initialThemeMode)

        override fun observeThemeMode(): Flow<ThemeMode> = themeMode

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            this.themeMode.value = themeMode
        }
    }
}
