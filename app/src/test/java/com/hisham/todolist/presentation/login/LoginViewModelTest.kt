package com.hisham.todolist.presentation.login

import com.hisham.todolist.core.state.AppRuntimeState
import com.hisham.todolist.domain.model.UserSession
import com.hisham.todolist.domain.usecase.SignInWithGoogleUseCase
import com.hisham.todolist.testdoubles.FakeAuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @Test
    fun `starts in idle state`() {
        val repository = FakeAuthRepository().apply {
            signInResult = Result.success(
                UserSession(
                    userId = "default-user",
                    displayName = "Default",
                    email = "default@example.com",
                ),
            )
        }
        val viewModel = LoginViewModel(
            signInWithGoogleUseCase = SignInWithGoogleUseCase(
                authRepository = repository,
            ),
            appRuntimeState = AppRuntimeState(),
        )

        assertEquals(LoginStatus.IDLE, viewModel.uiState.value.status)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `emits success state and navigation event when sign in succeeds`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeAuthRepository().apply {
                signInResult = Result.success(
                    UserSession(
                        userId = "user-1",
                        displayName = "Hisham",
                        email = "hisham@example.com",
                        photoUrl = null,
                    ),
                )
            }
            val viewModel = LoginViewModel(
                signInWithGoogleUseCase = SignInWithGoogleUseCase(
                    authRepository = repository,
                ),
                appRuntimeState = AppRuntimeState(),
            )

            val event = async { viewModel.events.first() }

            viewModel.onSignInClicked()
            advanceUntilIdle()

            assertEquals(LoginStatus.SUCCESS, viewModel.uiState.value.status)
            assertNull(viewModel.uiState.value.errorMessage)
            assertEquals(LoginEvent.NavigateToPending, event.await())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `emits error state when sign in fails`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeAuthRepository().apply {
                signInResult = Result.failure(
                    IllegalStateException("No se pudo completar la autenticacion."),
                )
            }
            val viewModel = LoginViewModel(
                signInWithGoogleUseCase = SignInWithGoogleUseCase(
                    authRepository = repository,
                ),
                appRuntimeState = AppRuntimeState(),
            )

            viewModel.onSignInClicked()
            advanceUntilIdle()

            assertEquals(LoginStatus.ERROR, viewModel.uiState.value.status)
            assertEquals(
                "No se pudo completar la autenticacion.",
                viewModel.uiState.value.errorMessage,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `shows Google config guidance when web client id is missing`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeAuthRepository().apply {
                signInResult = Result.failure(
                    IllegalStateException("Falta GOOGLE_WEB_CLIENT_ID."),
                )
            }
            val viewModel = LoginViewModel(
                signInWithGoogleUseCase = SignInWithGoogleUseCase(
                    authRepository = repository,
                ),
                appRuntimeState = AppRuntimeState(),
            )

            viewModel.onSignInClicked()
            advanceUntilIdle()

            assertEquals(LoginStatus.ERROR, viewModel.uiState.value.status)
            assertEquals(
                "Configura GOOGLE_WEB_CLIENT_ID con tu OAuth Web Client ID real en gradle.properties o local.properties.",
                viewModel.uiState.value.errorMessage,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `ignores repeated taps while sign in is already running`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val gate = CompletableDeferred<Unit>()
        val repository = FakeAuthRepository().apply {
            signInAction = {
                gate.await()
                Result.success(
                    UserSession(
                        userId = "user-1",
                        displayName = "Hisham",
                        email = "hisham@example.com",
                        photoUrl = null,
                    ),
                )
            }
        }

        try {
            val viewModel = LoginViewModel(
                signInWithGoogleUseCase = SignInWithGoogleUseCase(authRepository = repository),
                appRuntimeState = AppRuntimeState(),
            )

            viewModel.onSignInClicked()
            viewModel.onSignInClicked()
            advanceUntilIdle()

            assertEquals(LoginStatus.LOADING, viewModel.uiState.value.status)
            assertEquals(1, repository.signInCalls)

            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(LoginStatus.SUCCESS, viewModel.uiState.value.status)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `recovers from error on a later successful sign in`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeAuthRepository().apply {
                signInResult = Result.failure(
                    IllegalStateException("No se pudo completar la autenticacion."),
                )
            }
            val viewModel = LoginViewModel(
                signInWithGoogleUseCase = SignInWithGoogleUseCase(repository),
                appRuntimeState = AppRuntimeState(),
            )

            viewModel.onSignInClicked()
            advanceUntilIdle()

            assertEquals(LoginStatus.ERROR, viewModel.uiState.value.status)

            repository.signInResult = Result.success(
                UserSession(
                    userId = "user-1",
                    displayName = "Hisham",
                    email = "hisham@example.com",
                ),
            )

            viewModel.onSignInClicked()
            advanceUntilIdle()

            assertEquals(LoginStatus.SUCCESS, viewModel.uiState.value.status)
            assertNull(viewModel.uiState.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
