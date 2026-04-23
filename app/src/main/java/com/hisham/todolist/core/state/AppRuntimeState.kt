package com.hisham.todolist.core.state

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class GlobalLoadingState(
    val isAuthenticating: Boolean = false,
    val isMutatingTasks: Boolean = false,
    val showBlockingOverlay: Boolean = false,
)

@Singleton
class AppRuntimeState @Inject constructor() {

    private val isBootstrapped = MutableStateFlow(false)
    private val authOperationCount = MutableStateFlow(0)
    private val taskOperationCount = MutableStateFlow(0)

    val bootstrapState = isBootstrapped.asStateFlow()

    val loadingState: Flow<GlobalLoadingState> = combine(
        authOperationCount,
        taskOperationCount,
    ) { authCount, taskCount ->
        val isAuthenticating = authCount > 0
        val isMutatingTasks = taskCount > 0
        GlobalLoadingState(
            isAuthenticating = isAuthenticating,
            isMutatingTasks = isMutatingTasks,
            showBlockingOverlay = isAuthenticating,
        )
    }

    fun markBootstrapped() {
        isBootstrapped.value = true
    }

    suspend fun <T> trackAuthOperation(block: suspend () -> T): T {
        authOperationCount.update { it + 1 }
        return try {
            block()
        } finally {
            authOperationCount.update { count -> (count - 1).coerceAtLeast(0) }
        }
    }

    suspend fun <T> trackTaskOperation(block: suspend () -> T): T {
        taskOperationCount.update { it + 1 }
        return try {
            block()
        } finally {
            taskOperationCount.update { count -> (count - 1).coerceAtLeast(0) }
        }
    }
}
