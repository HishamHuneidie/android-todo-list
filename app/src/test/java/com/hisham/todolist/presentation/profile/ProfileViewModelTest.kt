package com.hisham.todolist.presentation.profile

import com.hisham.todolist.core.state.AppRuntimeState
import com.hisham.todolist.domain.model.ThemeMode
import com.hisham.todolist.domain.model.UserSession
import com.hisham.todolist.domain.usecase.ChangeThemeUseCase
import com.hisham.todolist.domain.usecase.GetUserUseCase
import com.hisham.todolist.domain.usecase.ObserveThemeModeUseCase
import com.hisham.todolist.domain.usecase.SignOutUseCase
import com.hisham.todolist.testdoubles.FakeAuthRepository
import com.hisham.todolist.testdoubles.FakeSettingsRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @Test
    fun `loads authenticated user and current theme into ui state`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val authRepository = FakeAuthRepository(
                initialSession = UserSession(
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
                appRuntimeState = AppRuntimeState(),
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
            val authRepository = FakeAuthRepository(initialSession = null)
            val settingsRepository = FakeSettingsRepository(ThemeMode.SYSTEM)

            val viewModel = ProfileViewModel(
                getUserUseCase = GetUserUseCase(authRepository),
                observeThemeModeUseCase = ObserveThemeModeUseCase(settingsRepository),
                changeThemeUseCase = ChangeThemeUseCase(settingsRepository),
                signOutUseCase = SignOutUseCase(authRepository),
                appRuntimeState = AppRuntimeState(),
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
            val authRepository = FakeAuthRepository(initialSession = null)
            val settingsRepository = FakeSettingsRepository(ThemeMode.SYSTEM)
            val viewModel = ProfileViewModel(
                getUserUseCase = GetUserUseCase(authRepository),
                observeThemeModeUseCase = ObserveThemeModeUseCase(settingsRepository),
                changeThemeUseCase = ChangeThemeUseCase(settingsRepository),
                signOutUseCase = SignOutUseCase(authRepository),
                appRuntimeState = AppRuntimeState(),
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
                initialSession = UserSession(
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
                appRuntimeState = AppRuntimeState(),
            )
            advanceUntilIdle()

            viewModel.signOut()
            advanceUntilIdle()

            assertEquals(1, authRepository.signOutCalls)
            assertFalse(viewModel.uiState.value.isAuthenticated)
            assertTrue(viewModel.uiState.value.isAuthResolved)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `theme updates are not blocked while sign out is still running`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val signOutGate = CompletableDeferred<Unit>()
        try {
            val authRepository = FakeAuthRepository(
                initialSession = UserSession(
                    userId = "user-1",
                    displayName = "Forge Master",
                    email = "forge@example.com",
                ),
            ).apply {
                signOutAction = { signOutGate.await() }
            }
            val settingsRepository = FakeSettingsRepository(ThemeMode.SYSTEM)
            val viewModel = ProfileViewModel(
                getUserUseCase = GetUserUseCase(authRepository),
                observeThemeModeUseCase = ObserveThemeModeUseCase(settingsRepository),
                changeThemeUseCase = ChangeThemeUseCase(settingsRepository),
                signOutUseCase = SignOutUseCase(authRepository),
                appRuntimeState = AppRuntimeState(),
            )
            advanceUntilIdle()

            viewModel.signOut()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isAuthenticated)

            viewModel.updateThemeMode(ThemeMode.DARK)
            advanceUntilIdle()

            assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)

            signOutGate.complete(Unit)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isAuthenticated)
            assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
