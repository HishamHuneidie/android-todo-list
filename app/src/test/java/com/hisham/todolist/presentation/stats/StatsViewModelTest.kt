package com.hisham.todolist.presentation.stats

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.model.TaskCompletionRecord
import com.hisham.todolist.domain.usecase.CalculateDailyAverageUseCase
import com.hisham.todolist.domain.usecase.CalculateWeeklyEffectivenessUseCase
import com.hisham.todolist.domain.usecase.GetMonthlyCompletedTasksUseCase
import com.hisham.todolist.testdoubles.FakeTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2026-04-22T10:00:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun `starts from a consistent default state`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = viewModelFor(FakeTaskRepository())
            advanceUntilIdle()

            assertEquals(0, viewModel.uiState.value.weeklyEffectiveness)
            assertEquals(0, viewModel.uiState.value.monthlyCompletedTasks)
            assertEquals(0.0, viewModel.uiState.value.dailyAverage, 0.0001)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `combines weekly monthly and average metrics into ui state`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeTaskRepository(
                initialTasks = listOf(
                    Task(
                        id = 1L,
                        title = "Workout",
                        isRecurrent = true,
                        recurrenceDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                        createdAt = Instant.parse("2026-04-01T00:00:00Z").toEpochMilli(),
                    ),
                    Task(
                        id = 2L,
                        title = "Read",
                        createdAt = Instant.parse("2026-04-21T00:00:00Z").toEpochMilli(),
                        isCompleted = true,
                    ),
                ),
                initialRecords = listOf(
                    TaskCompletionRecord(
                        taskId = 1L,
                        occurrenceEpochDay = LocalDate.of(2026, 4, 20).toEpochDay(),
                        completedAt = Instant.parse("2026-04-20T09:00:00Z").toEpochMilli(),
                    ),
                    TaskCompletionRecord(
                        taskId = 2L,
                        occurrenceEpochDay = LocalDate.of(2026, 4, 21).toEpochDay(),
                        completedAt = Instant.parse("2026-04-21T11:00:00Z").toEpochMilli(),
                    ),
                    TaskCompletionRecord(
                        taskId = 3L,
                        occurrenceEpochDay = LocalDate.of(2026, 4, 1).toEpochDay(),
                        completedAt = Instant.parse("2026-04-01T08:00:00Z").toEpochMilli(),
                    ),
                ),
            )

            val viewModel = StatsViewModel(
                calculateWeeklyEffectivenessUseCase = CalculateWeeklyEffectivenessUseCase(
                    repository,
                    fixedClock
                ),
                getMonthlyCompletedTasksUseCase = GetMonthlyCompletedTasksUseCase(
                    repository,
                    fixedClock
                ),
                calculateDailyAverageUseCase = CalculateDailyAverageUseCase(repository, fixedClock),
            )
            advanceUntilIdle()

            assertEquals(67, viewModel.uiState.value.weeklyEffectiveness)
            assertEquals(3, viewModel.uiState.value.monthlyCompletedTasks)
            assertEquals(3.0 / 22.0, viewModel.uiState.value.dailyAverage, 0.0001)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `reacts when only weekly effectiveness changes`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeTaskRepository(
                initialRecords = listOf(
                    TaskCompletionRecord(
                        taskId = 1L,
                        occurrenceEpochDay = LocalDate.of(2026, 4, 21).toEpochDay(),
                        completedAt = Instant.parse("2026-04-21T10:00:00Z").toEpochMilli(),
                    ),
                ),
            )
            val viewModel = viewModelFor(repository)
            advanceUntilIdle()

            assertEquals(0, viewModel.uiState.value.weeklyEffectiveness)
            assertEquals(1, viewModel.uiState.value.monthlyCompletedTasks)
            assertEquals(1.0 / 22.0, viewModel.uiState.value.dailyAverage, 0.0001)

            repository.emitTasks(
                listOf(
                    Task(
                        id = 1L,
                        title = "Workout",
                        isRecurrent = true,
                        recurrenceDays = setOf(DayOfWeek.TUESDAY),
                        createdAt = Instant.parse("2026-04-01T00:00:00Z").toEpochMilli(),
                    ),
                ),
            )
            advanceUntilIdle()

            assertEquals(100, viewModel.uiState.value.weeklyEffectiveness)
            assertEquals(1, viewModel.uiState.value.monthlyCompletedTasks)
            assertEquals(1.0 / 22.0, viewModel.uiState.value.dailyAverage, 0.0001)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun viewModelFor(repository: FakeTaskRepository): StatsViewModel =
        StatsViewModel(
            calculateWeeklyEffectivenessUseCase = CalculateWeeklyEffectivenessUseCase(
                repository,
                fixedClock,
            ),
            getMonthlyCompletedTasksUseCase = GetMonthlyCompletedTasksUseCase(
                repository,
                fixedClock,
            ),
            calculateDailyAverageUseCase = CalculateDailyAverageUseCase(repository, fixedClock),
        )
}
