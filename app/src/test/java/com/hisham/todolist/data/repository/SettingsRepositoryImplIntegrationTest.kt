package com.hisham.todolist.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.hisham.todolist.domain.model.ThemeMode
import com.hisham.todolist.domain.usecase.ChangeThemeUseCase
import com.hisham.todolist.domain.usecase.ObserveThemeModeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryImplIntegrationTest {

    @Test
    fun `change theme persists and re emits through datastore`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.filesDir.resolve("settings-${UUID.randomUUID()}.preferences_pb")
        val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = firstScope,
            produceFile = { file },
        )
        val repository = SettingsRepositoryImpl(dataStore)
        val observeThemeModeUseCase = ObserveThemeModeUseCase(repository)
        val emissions = async {
            observeThemeModeUseCase().take(2).toList()
        }
        runCurrent()

        ChangeThemeUseCase(repository)(ThemeMode.DARK)

        assertEquals(
            listOf(ThemeMode.SYSTEM, ThemeMode.DARK),
            emissions.await(),
        )
        assertEquals(ThemeMode.DARK, repository.observeThemeMode().first())
        firstScope.cancel()
        file.delete()
    }
}
