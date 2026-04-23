package com.hisham.todolist.presentation.loader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisham.todolist.core.state.AppRuntimeState
import com.hisham.todolist.domain.usecase.InitializeAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LoaderStatus {
    LOADING,
    AUTHENTICATED,
    UNAUTHENTICATED,
}

data class LoaderUiState(
    val status: LoaderStatus = LoaderStatus.LOADING,
    val errorMessage: String? = null,
)

@HiltViewModel
class LoaderViewModel @Inject constructor(
    private val initializeAppUseCase: InitializeAppUseCase,
    private val appRuntimeState: AppRuntimeState,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoaderUiState())
    val uiState: StateFlow<LoaderUiState> = _uiState.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            try {
                val result = initializeAppUseCase()
                _uiState.value = LoaderUiState(
                    status = if (result.session != null) {
                        LoaderStatus.AUTHENTICATED
                    } else {
                        LoaderStatus.UNAUTHENTICATED
                    },
                )
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                _uiState.value = LoaderUiState(
                    status = LoaderStatus.UNAUTHENTICATED,
                    errorMessage = throwable.message ?: "No se pudo inicializar la aplicacion.",
                )
            } finally {
                appRuntimeState.markBootstrapped()
            }
        }
    }
}
