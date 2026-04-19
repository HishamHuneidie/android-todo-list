package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.ThemeMode
import com.hisham.todolist.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveThemeModeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<ThemeMode> = settingsRepository.observeThemeMode()
}
