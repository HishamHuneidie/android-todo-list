package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.model.UserSession
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

data class AppInitializationResult(
    val session: UserSession?,
    val preparedTasks: List<Task>,
    val completedWithWarnings: Boolean,
)

class InitializeAppUseCase @Inject constructor(
    private val checkUserSessionUseCase: CheckUserSessionUseCase,
    private val prepareTasksForCurrentDayUseCase: PrepareTasksForCurrentDayUseCase,
) {
    suspend operator fun invoke(): AppInitializationResult {
        var completedWithWarnings = false

        val session = try {
            checkUserSessionUseCase()
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Throwable) {
            completedWithWarnings = true
            null
        }

        val preparedTasks = try {
            prepareTasksForCurrentDayUseCase()
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Throwable) {
            completedWithWarnings = true
            emptyList()
        }

        return AppInitializationResult(
            session = session,
            preparedTasks = preparedTasks,
            completedWithWarnings = completedWithWarnings,
        )
    }
}
