package com.hisham.todolist.presentation

import com.hisham.todolist.core.state.AppRuntimeState
import com.hisham.todolist.domain.model.AuthState
import com.hisham.todolist.domain.model.ThemeMode
import com.hisham.todolist.domain.model.UserSession
import com.hisham.todolist.domain.repository.AuthRepository
import com.hisham.todolist.domain.repository.SettingsRepository
import com.hisham.todolist.domain.usecase.ObserveAuthStateUseCase
import com.hisham.todolist.domain.usecase.ObserveThemeModeUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppStateViewModelTest {

    @Test
    fun `combines bootstrap and auth state for guarded navigation`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val authRepository = FakeAuthRepository()
            val settingsRepository = FakeSettingsRepository()
            val appRuntimeState = AppRuntimeState()

            val viewModel = AppStateViewModel(
                observeThemeModeUseCase = ObserveThemeModeUseCase(settingsRepository),
                observeAuthStateUseCase = ObserveAuthStateUseCase(authRepository),
                appRuntimeState = appRuntimeState,
            )
            val collector = launch(dispatcher) {
                viewModel.uiState.collect { }
            }
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isBootstrapped)
            assertFalse(viewModel.uiState.value.isAuthenticated)

            appRuntimeState.markBootstrapped()
            authRepository.authState.value = AuthState.Authenticated(
                UserSession(
                    userId = "user-1",
                    displayName = "Hisham",
                    email = "hisham@example.com",
                ),
            )
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isBootstrapped)
            assertTrue(viewModel.uiState.value.isAuthenticated)
            collector.cancelAndJoin()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `reflects global loading from auth and task operations`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val appRuntimeState = AppRuntimeState()
            val viewModel = AppStateViewModel(
                observeThemeModeUseCase = ObserveThemeModeUseCase(FakeSettingsRepository()),
                observeAuthStateUseCase = ObserveAuthStateUseCase(FakeAuthRepository()),
                appRuntimeState = appRuntimeState,
            )
            val collector = launch(dispatcher) {
                viewModel.uiState.collect { }
            }
            val authGate = CompletableDeferred<Unit>()
            val taskGate = CompletableDeferred<Unit>()

            val authJob = launch(dispatcher) {
                appRuntimeState.trackAuthOperation {
                    authGate.await()
                }
            }
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.loadingState.isAuthenticating)
            assertTrue(viewModel.uiState.value.loadingState.showBlockingOverlay)

            authGate.complete(Unit)
            advanceUntilIdle()
            authJob.join()

            val taskJob = launch(dispatcher) {
                appRuntimeState.trackTaskOperation {
                    taskGate.await()
                }
            }
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.loadingState.isMutatingTasks)
            assertFalse(viewModel.uiState.value.loadingState.showBlockingOverlay)

            taskGate.complete(Unit)
            advanceUntilIdle()
            taskJob.join()

            assertFalse(viewModel.uiState.value.loadingState.isAuthenticating)
            assertFalse(viewModel.uiState.value.loadingState.isMutatingTasks)
            collector.cancelAndJoin()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeAuthRepository : AuthRepository {
        val authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)

        override fun observeAuthState(): Flow<AuthState> = authState

        override suspend fun getCurrentSession(): UserSession? = null

        override suspend fun signInWithGoogle(): Result<UserSession> = error("Not used")

        override suspend fun signOut() = Unit
    }

    private class FakeSettingsRepository : SettingsRepository {
        private val themeMode = MutableStateFlow(ThemeMode.SYSTEM)

        override fun observeThemeMode(): Flow<ThemeMode> = themeMode

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            this.themeMode.value = themeMode
        }
    }
}
