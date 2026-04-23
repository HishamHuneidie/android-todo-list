package com.hisham.todolist.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisham.todolist.domain.model.ThemeMode
import com.hisham.todolist.domain.model.UserSession
import com.hisham.todolist.domain.usecase.ChangeThemeUseCase
import com.hisham.todolist.domain.usecase.GetUserUseCase
import com.hisham.todolist.domain.usecase.ObserveThemeModeUseCase
import com.hisham.todolist.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val displayName: String = "Invitado",
    val email: String = "",
    val photoUrl: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isAuthenticated: Boolean = false,
    val isAuthResolved: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase,
    observeThemeModeUseCase: ObserveThemeModeUseCase,
    private val changeThemeUseCase: ChangeThemeUseCase,
    private val signOutUseCase: SignOutUseCase,
) : ViewModel() {

    private val userSession = MutableStateFlow<UserSession?>(null)
    private val isAuthResolved = MutableStateFlow(false)

    val uiState: StateFlow<ProfileUiState> = combine(
        observeThemeModeUseCase(),
        userSession,
        isAuthResolved,
    ) { themeMode, session, authResolved ->
        ProfileUiState(
            displayName = session?.displayName ?: "Invitado",
            email = session?.email.orEmpty(),
            photoUrl = session?.photoUrl,
            themeMode = themeMode,
            isAuthenticated = session != null,
            isAuthResolved = authResolved,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ProfileUiState(),
    )

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            userSession.value = runCatching { getUserUseCase() }.getOrNull()
            isAuthResolved.value = true
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            changeThemeUseCase(themeMode)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            userSession.value = null
            isAuthResolved.value = true
        }
    }
}
