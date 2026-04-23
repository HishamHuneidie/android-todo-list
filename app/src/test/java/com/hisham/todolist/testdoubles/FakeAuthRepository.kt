package com.hisham.todolist.testdoubles

import com.hisham.todolist.domain.model.AuthState
import com.hisham.todolist.domain.model.UserSession
import com.hisham.todolist.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository(
    initialSession: UserSession? = null,
    initialAuthState: AuthState = initialSession?.let(AuthState::Authenticated)
        ?: AuthState.Unauthenticated,
) : AuthRepository {

    private val authState = MutableStateFlow(initialAuthState)

    var session: UserSession? =
        (initialAuthState as? AuthState.Authenticated)?.session ?: initialSession
        private set

    var signInResult: Result<UserSession> = Result.failure(
        IllegalStateException("signInWithGoogle not configured."),
    )
    var signInAction: (suspend () -> Result<UserSession>)? = null
    var getCurrentSessionAction: (suspend () -> UserSession?)? = null
    var getCurrentSessionError: Throwable? = null
    var signOutError: Throwable? = null
    var signOutAction: (suspend () -> Unit)? = null

    var signInCalls: Int = 0
        private set
    var signOutCalls: Int = 0
        private set
    var getCurrentSessionCalls: Int = 0
        private set

    override fun observeAuthState(): Flow<AuthState> = authState

    override suspend fun getCurrentSession(): UserSession? {
        getCurrentSessionCalls += 1
        getCurrentSessionError?.let { throw it }
        getCurrentSessionAction?.let { return it() }
        return session
    }

    override suspend fun signInWithGoogle(): Result<UserSession> {
        signInCalls += 1
        val result = signInAction?.invoke() ?: signInResult
        result.onSuccess(::emitSession)
        return result
    }

    override suspend fun signOut() {
        signOutCalls += 1
        signOutError?.let { throw it }
        signOutAction?.invoke()
        emitSession(null)
    }

    fun emitSession(session: UserSession?) {
        this.session = session
        authState.value = session?.let(AuthState::Authenticated) ?: AuthState.Unauthenticated
    }

    fun emitAuthState(state: AuthState) {
        authState.value = state
        session = (state as? AuthState.Authenticated)?.session
    }
}
