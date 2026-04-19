package com.hisham.todolist.domain.model

sealed interface AuthState {
    data object Unauthenticated : AuthState

    data class Authenticated(
        val session: UserSession,
    ) : AuthState
}
