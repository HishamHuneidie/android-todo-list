package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.ThemeMode
import com.hisham.todolist.domain.model.UserSession
import com.hisham.todolist.testdoubles.FakeAuthRepository
import com.hisham.todolist.testdoubles.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileUseCasesTest {

    @Test
    fun `get user use case returns current session when available`() = runTest {
        val session = UserSession(
            userId = "user-1",
            displayName = "Hisham",
            email = "hisham@example.com",
        )
        val useCase = GetUserUseCase(FakeAuthRepository(initialSession = session))

        assertEquals(session, useCase())
    }

    @Test
    fun `get user use case returns null when there is no session`() = runTest {
        val useCase = GetUserUseCase(FakeAuthRepository(initialSession = null))

        assertNull(useCase())
    }

    @Test
    fun `change theme use case persists selected mode`() = runTest {
        val repository = FakeSettingsRepository(initialThemeMode = ThemeMode.SYSTEM)
        val useCase = ChangeThemeUseCase(repository)

        useCase(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, repository.themeMode.value)
    }
}
