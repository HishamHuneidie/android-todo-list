package com.hisham.todolist.presentation.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisham.todolist.domain.usecase.ObserveTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PendingUiState(
    val taskCount: Int = 0,
)

@HiltViewModel
class PendingViewModel @Inject constructor(
    observeTasksUseCase: ObserveTasksUseCase,
) : ViewModel() {

    val uiState: StateFlow<PendingUiState> = observeTasksUseCase()
        .map { tasks -> PendingUiState(taskCount = tasks.size) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PendingUiState(),
        )
}
