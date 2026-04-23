package com.hisham.todolist.presentation

import com.hisham.todolist.core.state.GlobalLoadingState
import com.hisham.todolist.domain.model.AuthState

data class AppUiState(
    val authState: AuthState = AuthState.Unauthenticated,
    val isBootstrapped: Boolean = false,
    val loadingState: GlobalLoadingState = GlobalLoadingState(),
) {
    val isAuthenticated: Boolean
        get() = authState is AuthState.Authenticated
}
