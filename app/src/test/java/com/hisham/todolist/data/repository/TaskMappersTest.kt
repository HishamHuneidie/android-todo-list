package com.hisham.todolist.data.repository

import com.hisham.todolist.domain.model.Task
import com.hisham.todolist.domain.model.TaskCategory
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskMappersTest {

    @Test
    fun `maps domain task to entity and back without losing supported fields`() {
        val task = Task(
            id = 9L,
            title = "Plan roadmap",
            isCompleted = true,
            progress = 75,
            isRecurrent = true,
            recurrenceDays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            stateDateEpochDay = 20_000L,
            category = TaskCategory.WORK,
            iconName = "briefcase",
            position = 2,
            createdAt = 100L,
            updatedAt = 200L,
        )

        val roundTrip = task.toEntity().toDomain()

        assertEquals(task, roundTrip)
    }

    @Test
    fun `normalizes invalid progress and recurrence data`() {
        val task = Task(
            title = "Deep clean",
            progress = 250,
            isRecurrent = false,
            recurrenceDays = setOf(DayOfWeek.SUNDAY),
            stateDateEpochDay = 123L,
        )

        val entity = task.toEntity()
        val mappedBack = entity.toDomain()

        assertEquals(100, entity.progress)
        assertEquals(emptySet<DayOfWeek>(), entity.recurrenceDays)
        assertNull(entity.stateDateEpochDay)

        assertEquals(100, mappedBack.progress)
        assertEquals(emptySet<DayOfWeek>(), mappedBack.recurrenceDays)
        assertNull(mappedBack.stateDateEpochDay)
    }
}
