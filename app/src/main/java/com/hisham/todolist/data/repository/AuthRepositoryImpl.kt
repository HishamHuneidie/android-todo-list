package com.hisham.todolist.data.repository

import com.hisham.todolist.BuildConfig
import com.hisham.todolist.data.auth.AuthSessionLocalDataSource
import com.hisham.todolist.data.auth.GoogleCredentialAuthClient
import com.hisham.todolist.domain.model.AuthState
import com.hisham.todolist.domain.model.UserSession
import com.hisham.todolist.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepositoryImpl @Inject constructor(
    private val authSessionLocalDataSource: AuthSessionLocalDataSource,
    private val googleCredentialAuthClient: GoogleCredentialAuthClient,
) : AuthRepository {

    override fun observeAuthState(): Flow<AuthState> =
        authSessionLocalDataSource.observeSession().map { session ->
            session?.let(AuthState::Authenticated) ?: AuthState.Unauthenticated
        }

    override suspend fun getCurrentSession(): UserSession? = authSessionLocalDataSource.getSession()

    override suspend fun signInWithGoogle(): Result<UserSession> = runCatching {
        val session = googleCredentialAuthClient.signIn(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        authSessionLocalDataSource.saveSession(session)
        session
    }

    override suspend fun signOut() {
        authSessionLocalDataSource.clearSession()
        runCatching {
            googleCredentialAuthClient.clearCredentialState()
        }
    }
}
