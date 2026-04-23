package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.model.TaskCompletionRecord
import com.hisham.todolist.testdoubles.FakeTaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class StatsUseCasesTest {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2026-04-22T10:00:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    fun `calculate weekly effectiveness uses scheduled occurrences and completion records`() =
        runTest {
            val repository = FakeTaskRepository(
                initialTasks = listOf(
                    Task(
                        id = 1L,
                        title = "Recurring",
                        isRecurrent = true,
                        recurrenceDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                        createdAt = Instant.parse("2026-04-01T00:00:00Z").toEpochMilli(),
                    ),
                    Task(
                        id = 2L,
                        title = "One off done",
                        createdAt = Instant.parse("2026-04-21T00:00:00Z").toEpochMilli(),
                        isCompleted = true,
                    ),
                    Task(
                        id = 3L,
                        title = "One off pending",
                        createdAt = Instant.parse("2026-04-18T00:00:00Z").toEpochMilli(),
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
                ),
            )

            val result =
                CalculateWeeklyEffectivenessUseCase(repository, fixedClock).invoke().first()

            assertEquals(50, result)
        }

    @Test
    fun `get monthly completed tasks counts current month records only`() = runTest {
        val repository = FakeTaskRepository(
            initialRecords = listOf(
                TaskCompletionRecord(
                    taskId = 1L,
                    occurrenceEpochDay = LocalDate.of(2026, 4, 10).toEpochDay(),
                    completedAt = Instant.parse("2026-04-10T08:00:00Z").toEpochMilli(),
                ),
                TaskCompletionRecord(
                    taskId = 2L,
                    occurrenceEpochDay = LocalDate.of(2026, 4, 21).toEpochDay(),
                    completedAt = Instant.parse("2026-04-21T08:00:00Z").toEpochMilli(),
                ),
                TaskCompletionRecord(
                    taskId = 3L,
                    occurrenceEpochDay = LocalDate.of(2026, 3, 31).toEpochDay(),
                    completedAt = Instant.parse("2026-03-31T22:00:00Z").toEpochMilli(),
                ),
            ),
        )

        val result = GetMonthlyCompletedTasksUseCase(repository, fixedClock).invoke().first()

        assertEquals(2, result)
    }

    @Test
    fun `calculate daily average uses completed tasks of current month and elapsed days`() =
        runTest {
            val repository = FakeTaskRepository(
                initialRecords = List(11) { index ->
                    TaskCompletionRecord(
                        taskId = index.toLong() + 1L,
                        occurrenceEpochDay = LocalDate.of(2026, 4, 1).toEpochDay() + index,
                        completedAt = Instant.parse("2026-04-10T08:00:00Z").toEpochMilli() + index,
                    )
                },
            )

            val result = CalculateDailyAverageUseCase(repository, fixedClock).invoke().first()

            assertEquals(0.5, result, 0.0001)
        }
}
