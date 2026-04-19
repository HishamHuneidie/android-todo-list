package com.hisham.todolist.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisham.todolist.domain.model.AuthState
import com.hisham.todolist.domain.model.ThemeMode
import com.hisham.todolist.domain.usecase.ObserveAuthStateUseCase
import com.hisham.todolist.domain.usecase.ObserveThemeModeUseCase
import com.hisham.todolist.domain.usecase.SetThemeModeUseCase
import com.hisham.todolist.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val displayName: String = "Invitado",
    val email: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isAuthenticated: Boolean = false,
    val isAuthResolved: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeThemeModeUseCase: ObserveThemeModeUseCase,
    observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val signOutUseCase: SignOutUseCase,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        observeThemeModeUseCase(),
        observeAuthStateUseCase(),
    ) { themeMode, authState ->
        when (authState) {
            is AuthState.Authenticated -> ProfileUiState(
                displayName = authState.session.displayName,
                email = authState.session.email,
                themeMode = themeMode,
                isAuthenticated = true,
                isAuthResolved = true,
            )
            AuthState.Unauthenticated -> ProfileUiState(
                themeMode = themeMode,
                isAuthenticated = false,
                isAuthResolved = true,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(),
    )

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            setThemeModeUseCase(themeMode)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
        }
    }
}
