package com.hisham.todolist.presentation.profile

import com.hisham.todolist.domain.model.AuthState
import com.hisham.todolist.domain.model.ThemeMode
import com.hisham.todolist.domain.model.UserSession
import com.hisham.todolist.domain.repository.AuthRepository
import com.hisham.todolist.domain.repository.SettingsRepository
import com.hisham.todolist.domain.usecase.ChangeThemeUseCase
import com.hisham.todolist.domain.usecase.GetUserUseCase
import com.hisham.todolist.domain.usecase.ObserveThemeModeUseCase
import com.hisham.todolist.domain.usecase.SignOutUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @Test
    fun `loads authenticated user and current theme into ui state`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val authRepository = FakeAuthRepository(
                session = UserSession(
                    userId = "user-1",
                    displayName = "Forge Master",
                    email = "forge@example.com",
                    photoUrl = "https://example.com/avatar.png",
                ),
            )
            val settingsRepository = FakeSettingsRepository(ThemeMode.DARK)

            val viewModel = ProfileViewModel(
                getUserUseCase = GetUserUseCase(authRepository),
                observeThemeModeUseCase = ObserveThemeModeUseCase(settingsRepository),
                changeThemeUseCase = ChangeThemeUseCase(settingsRepository),
                signOutUseCase = SignOutUseCase(authRepository),
            )
            advanceUntilIdle()

            assertEquals("Forge Master", viewModel.uiState.value.displayName)
            assertEquals("forge@example.com", viewModel.uiState.value.email)
            assertEquals("https://example.com/avatar.png", viewModel.uiState.value.photoUrl)
            assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
            assertTrue(viewModel.uiState.value.isAuthenticated)
            assertTrue(viewModel.uiState.value.isAuthResolved)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `emits unauthenticated state when no user is available`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val authRepository = FakeAuthRepository(session = null)
            val settingsRepository = FakeSettingsRepository(ThemeMode.SYSTEM)

            val viewModel = ProfileViewModel(
                getUserUseCase = GetUserUseCase(authRepository),
                observeThemeModeUseCase = ObserveThemeModeUseCase(settingsRepository),
                changeThemeUseCase = ChangeThemeUseCase(settingsRepository),
                signOutUseCase = SignOutUseCase(authRepository),
            )
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isAuthenticated)
            assertTrue(viewModel.uiState.value.isAuthResolved)
            assertEquals("Invitado", viewModel.uiState.value.displayName)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `updates theme through change theme use case`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val authRepository = FakeAuthRepository(session = null)
            val settingsRepository = FakeSettingsRepository(ThemeMode.SYSTEM)
            val viewModel = ProfileViewModel(
                getUserUseCase = GetUserUseCase(authRepository),
                observeThemeModeUseCase = ObserveThemeModeUseCase(settingsRepository),
                changeThemeUseCase = ChangeThemeUseCase(settingsRepository),
                signOutUseCase = SignOutUseCase(authRepository),
            )
            advanceUntilIdle()

            viewModel.updateThemeMode(ThemeMode.LIGHT)
            advanceUntilIdle()

            assertEquals(ThemeMode.LIGHT, settingsRepository.themeMode.value)
            assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `sign out clears authenticated state`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val authRepository = FakeAuthRepository(
                session = UserSession(
                    userId = "user-1",
                    displayName = "Forge Master",
                    email = "forge@example.com",
                ),
            )
            val settingsRepository = FakeSettingsRepository(ThemeMode.DARK)
            val viewModel = ProfileViewModel(
                getUserUseCase = GetUserUseCase(authRepository),
                observeThemeModeUseCase = ObserveThemeModeUseCase(settingsRepository),
                changeThemeUseCase = ChangeThemeUseCase(settingsRepository),
                signOutUseCase = SignOutUseCase(authRepository),
            )
            advanceUntilIdle()

            viewModel.signOut()
            advanceUntilIdle()

            assertTrue(authRepository.signOutCalled)
            assertFalse(viewModel.uiState.value.isAuthenticated)
            assertTrue(viewModel.uiState.value.isAuthResolved)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeAuthRepository(
        private val session: UserSession?,
    ) : AuthRepository {
        var signOutCalled: Boolean = false
            private set

        override fun observeAuthState(): Flow<AuthState> = MutableStateFlow(
            session?.let(AuthState::Authenticated) ?: AuthState.Unauthenticated,
        )

        override suspend fun getCurrentSession(): UserSession? = session

        override suspend fun signInWithGoogle(): Result<UserSession> {
            error("Not used in this test")
        }

        override suspend fun signOut() {
            signOutCalled = true
        }
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
