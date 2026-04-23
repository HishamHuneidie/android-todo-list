package com.hisham.todolist.data.auth

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.hisham.todolist.domain.model.AuthState
import com.hisham.todolist.domain.model.UserSession
import com.hisham.todolist.domain.repository.AuthRepository
import com.hisham.todolist.domain.usecase.CheckUserSessionUseCase
import com.hisham.todolist.domain.usecase.ObserveAuthStateUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
class AuthSessionLocalIntegrationTest {

    @Test
    fun `saved session is readable and emitted through auth use cases`() = runTest {
        val dispatcher = newDataStoreDispatcher()
        val job = SupervisorJob()
        val scope = CoroutineScope(job + dispatcher)
        val repository = localOnlyAuthRepository(scope)
        val session = UserSession(
            userId = "user-1",
            displayName = "Hisham",
            email = "hisham@example.com",
            photoUrl = "https://example.com/avatar.png",
        )
        val observeAuthStateUseCase = ObserveAuthStateUseCase(repository)

        assertEquals(AuthState.Unauthenticated, observeAuthStateUseCase().first())

        repository.localDataSource.saveSession(session)

        assertEquals(session, CheckUserSessionUseCase(repository)())
        assertEquals(AuthState.Authenticated(session), observeAuthStateUseCase().first())
        scope.cancel()
        job.join()
        dispatcher.close()
    }

    private fun localOnlyAuthRepository(
        scope: CoroutineScope,
        file: File = newAuthFile(),
    ): LocalOnlyAuthRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file },
        )
        return LocalOnlyAuthRepository(
            localDataSource = AuthSessionLocalDataSource(dataStore),
        )
    }

    private fun newAuthFile(): File {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return context.filesDir.resolve("auth-${UUID.randomUUID()}.preferences_pb")
    }

    private fun newDataStoreDispatcher(): ExecutorCoroutineDispatcher =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private class LocalOnlyAuthRepository(
        val localDataSource: AuthSessionLocalDataSource,
    ) : AuthRepository {

        override fun observeAuthState(): Flow<AuthState> =
            localDataSource.observeSession().map { session ->
                session?.let(AuthState::Authenticated) ?: AuthState.Unauthenticated
            }

        override suspend fun getCurrentSession(): UserSession? = localDataSource.getSession()

        override suspend fun signInWithGoogle(): Result<UserSession> {
            error("Not used in integration test")
        }

        override suspend fun signOut() {
            localDataSource.clearSession()
        }
    }
}
