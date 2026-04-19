package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.ThemeMode
import com.hisham.todolist.domain.repository.SettingsRepository
import javax.inject.Inject

class SetThemeModeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(themeMode: ThemeMode) = settingsRepository.setThemeMode(themeMode)
}
