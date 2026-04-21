package com.hisham.todolist.presentation.login

import com.hisham.todolist.domain.model.AuthState
import com.hisham.todolist.domain.model.UserSession
import com.hisham.todolist.domain.repository.AuthRepository
import com.hisham.todolist.domain.usecase.SignInWithGoogleUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
        val viewModel = LoginViewModel(
            signInWithGoogleUseCase = SignInWithGoogleUseCase(
                authRepository = FakeAuthRepository(),
            ),
        )

        assertEquals(LoginStatus.IDLE, viewModel.uiState.value.status)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `emits success state and navigation event when sign in succeeds`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = LoginViewModel(
                signInWithGoogleUseCase = SignInWithGoogleUseCase(
                    authRepository = FakeAuthRepository(
                        signInResult = Result.success(
                            UserSession(
                                userId = "user-1",
                                displayName = "Hisham",
                                email = "hisham@example.com",
                                photoUrl = null,
                            ),
                        ),
                    ),
                ),
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
            val viewModel = LoginViewModel(
                signInWithGoogleUseCase = SignInWithGoogleUseCase(
                    authRepository = FakeAuthRepository(
                        signInResult = Result.failure(
                            IllegalStateException("No se pudo completar la autenticacion."),
                        ),
                    ),
                ),
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
            val viewModel = LoginViewModel(
                signInWithGoogleUseCase = SignInWithGoogleUseCase(
                    authRepository = FakeAuthRepository(
                        signInResult = Result.failure(
                            IllegalStateException("Falta GOOGLE_WEB_CLIENT_ID."),
                        ),
                    ),
                ),
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
        val repository = FakeAuthRepository(
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
            },
        )

        try {
            val viewModel = LoginViewModel(
                signInWithGoogleUseCase = SignInWithGoogleUseCase(authRepository = repository),
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

    private class FakeAuthRepository(
        private val signInResult: Result<UserSession> = Result.success(
            UserSession(
                userId = "default-user",
                displayName = "Default",
                email = "default@example.com",
                photoUrl = null,
            ),
        ),
        private val signInAction: (suspend () -> Result<UserSession>)? = null,
    ) : AuthRepository {
        var signInCalls: Int = 0
            private set

        override fun observeAuthState(): Flow<AuthState> = emptyFlow()

        override suspend fun getCurrentSession(): UserSession? = null

        override suspend fun signInWithGoogle(): Result<UserSession> {
            signInCalls += 1
            return signInAction?.invoke() ?: signInResult
        }

        override suspend fun signOut() = Unit
    }
}
