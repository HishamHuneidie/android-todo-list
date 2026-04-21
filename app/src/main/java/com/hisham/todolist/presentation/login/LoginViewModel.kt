package com.hisham.todolist.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisham.todolist.domain.usecase.SignInWithGoogleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LoginStatus {
    IDLE,
    LOADING,
    SUCCESS,
    ERROR,
}

data class LoginUiState(
    val status: LoginStatus = LoginStatus.IDLE,
    val errorMessage: String? = null,
) {
    val isLoading: Boolean
        get() = status == LoginStatus.LOADING
}

sealed interface LoginEvent {
    data object NavigateToPending : LoginEvent
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(capacity = Channel.BUFFERED)
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    fun onSignInClicked() {
        if (_uiState.value.status == LoginStatus.LOADING) return

        _uiState.value = LoginUiState(status = LoginStatus.LOADING)

        viewModelScope.launch {
            signInWithGoogleUseCase().fold(
                onSuccess = {
                    _uiState.value = LoginUiState(status = LoginStatus.SUCCESS)
                    _events.send(LoginEvent.NavigateToPending)
                },
                onFailure = { throwable ->
                    if (throwable is CancellationException) {
                        throw throwable
                    }

                    _uiState.value = LoginUiState(
                        status = LoginStatus.ERROR,
                        errorMessage = throwable.toUserMessage(),
                    )
                },
            )
        }
    }

    private fun Throwable.toUserMessage(): String {
        val rawMessage = message?.trim().orEmpty()
        return when {
            rawMessage.contains("GOOGLE_WEB_CLIENT_ID", ignoreCase = true) ->
                "Configura GOOGLE_WEB_CLIENT_ID con tu OAuth Web Client ID real en gradle.properties o local.properties."

            rawMessage.contains("Activity activa", ignoreCase = true) ->
                "No se pudo abrir el acceso con Google. Intentalo de nuevo."

            rawMessage.isNotEmpty() -> rawMessage
            else -> "No se pudo iniciar sesion."
        }
    }
}
