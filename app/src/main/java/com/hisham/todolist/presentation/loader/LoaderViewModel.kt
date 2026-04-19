package com.hisham.todolist.presentation.loader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisham.todolist.domain.model.AuthState
import com.hisham.todolist.domain.usecase.ObserveAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class LoaderDestination {
    AUTHENTICATED,
    UNAUTHENTICATED,
}

data class LoaderUiState(
    val isLoading: Boolean = true,
    val destination: LoaderDestination? = null,
)

@HiltViewModel
class LoaderViewModel @Inject constructor(
    observeAuthStateUseCase: ObserveAuthStateUseCase,
) : ViewModel() {

    val uiState: StateFlow<LoaderUiState> = observeAuthStateUseCase()
        .map { authState ->
            LoaderUiState(
                isLoading = false,
                destination = when (authState) {
                    is AuthState.Authenticated -> LoaderDestination.AUTHENTICATED
                    AuthState.Unauthenticated -> LoaderDestination.UNAUTHENTICATED
                },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LoaderUiState(),
        )
}
