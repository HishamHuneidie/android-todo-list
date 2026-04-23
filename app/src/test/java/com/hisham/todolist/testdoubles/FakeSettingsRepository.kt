package com.hisham.todolist.testdoubles

import com.hisham.todolist.domain.model.ThemeMode
import com.hisham.todolist.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository(
    initialThemeMode: ThemeMode = ThemeMode.SYSTEM,
) : SettingsRepository {

    val themeMode = MutableStateFlow(initialThemeMode)
    val setThemeModeCalls = mutableListOf<ThemeMode>()

    var setThemeModeError: Throwable? = null
    var setThemeModeAction: (suspend (ThemeMode) -> Unit)? = null

    override fun observeThemeMode(): Flow<ThemeMode> = themeMode

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        setThemeModeCalls += themeMode
        setThemeModeError?.let { throw it }
        setThemeModeAction?.invoke(themeMode) ?: run {
            this.themeMode.value = themeMode
        }
    }
}
