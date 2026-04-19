package com.hisham.todolist.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.hisham.todolist.data.local.preferences.PreferenceKeys
import com.hisham.todolist.domain.model.UserSession
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AuthSessionLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    fun observeSession(): Flow<UserSession?> = dataStore.data.map { preferences ->
        preferences.toUserSession()
    }

    suspend fun getSession(): UserSession? = dataStore.data.first().toUserSession()

    suspend fun saveSession(session: UserSession) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.isAuthenticated] = true
            preferences[PreferenceKeys.userId] = session.userId
            preferences[PreferenceKeys.displayName] = session.displayName
            preferences[PreferenceKeys.email] = session.email
            session.photoUrl?.let { photoUrl ->
                preferences[PreferenceKeys.photoUrl] = photoUrl
            } ?: preferences.remove(PreferenceKeys.photoUrl)
        }
    }

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.isAuthenticated] = false
            preferences.remove(PreferenceKeys.userId)
            preferences.remove(PreferenceKeys.displayName)
            preferences.remove(PreferenceKeys.email)
            preferences.remove(PreferenceKeys.photoUrl)
        }
    }

    private fun Preferences.toUserSession(): UserSession? {
        val isAuthenticated = this[PreferenceKeys.isAuthenticated] ?: false
        val email = this[PreferenceKeys.email].orEmpty()
        if (!isAuthenticated || email.isBlank()) {
            return null
        }

        return UserSession(
            userId = this[PreferenceKeys.userId] ?: email,
            displayName = this[PreferenceKeys.displayName] ?: email.substringBefore("@"),
            email = email,
            photoUrl = this[PreferenceKeys.photoUrl],
        )
    }
}
