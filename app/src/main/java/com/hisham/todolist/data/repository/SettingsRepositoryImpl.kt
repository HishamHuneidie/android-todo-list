package com.hisham.todolist.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.hisham.todolist.data.local.preferences.PreferenceKeys
import com.hisham.todolist.domain.model.ThemeMode
import com.hisham.todolist.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun observeThemeMode(): Flow<ThemeMode> =
        dataStore.data.map { preferences ->
            val persistedMode = preferences[PreferenceKeys.themeMode]
            persistedMode
                ?.let { rawMode -> runCatching { ThemeMode.valueOf(rawMode) }.getOrNull() }
                ?: ThemeMode.SYSTEM
        }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.themeMode] = themeMode.name
        }
    }
}
