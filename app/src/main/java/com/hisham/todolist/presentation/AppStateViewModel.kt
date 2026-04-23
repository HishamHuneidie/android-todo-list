package com.hisham.todolist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisham.todolist.core.state.AppRuntimeState
import com.hisham.todolist.domain.model.AuthState
import com.hisham.todolist.domain.model.ThemeMode
import com.hisham.todolist.domain.usecase.ObserveAuthStateUseCase
import com.hisham.todolist.domain.usecase.ObserveThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppStateViewModel @Inject constructor(
    observeThemeModeUseCase: ObserveThemeModeUseCase,
    observeAuthStateUseCase: ObserveAuthStateUseCase,
    appRuntimeState: AppRuntimeState,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = observeThemeModeUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM,
        )

    val uiState: StateFlow<AppUiState> = combine(
        observeAuthStateUseCase(),
        appRuntimeState.bootstrapState,
        appRuntimeState.loadingState,
    ) { authState, isBootstrapped, loadingState ->
        AppUiState(
            authState = authState,
            isBootstrapped = isBootstrapped,
            loadingState = loadingState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppUiState(authState = AuthState.Unauthenticated),
    )
}
