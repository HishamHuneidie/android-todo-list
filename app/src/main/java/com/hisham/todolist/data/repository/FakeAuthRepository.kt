package com.hisham.todolist.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.hisham.todolist.data.local.preferences.PreferenceKeys
import com.hisham.todolist.domain.model.AuthState
import com.hisham.todolist.domain.model.UserSession
import com.hisham.todolist.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FakeAuthRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : AuthRepository {

    override fun observeAuthState(): Flow<AuthState> =
        dataStore.data.map { preferences ->
            val isAuthenticated = preferences[PreferenceKeys.isAuthenticated] ?: false
            if (!isAuthenticated) {
                AuthState.Unauthenticated
            } else {
                AuthState.Authenticated(
                    session = UserSession(
                        userId = preferences[PreferenceKeys.userId] ?: "demo-user",
                        displayName = preferences[PreferenceKeys.displayName] ?: "Demo User",
                        email = preferences[PreferenceKeys.email] ?: "demo@todo.local",
                        photoUrl = preferences[PreferenceKeys.photoUrl],
                    ),
                )
            }
        }

    override suspend fun signInWithGoogle(): Result<UserSession> {
        val session = UserSession(
            userId = "demo-user",
            displayName = "Demo User",
            email = "demo@todo.local",
        )

        dataStore.edit { preferences ->
            preferences[PreferenceKeys.isAuthenticated] = true
            preferences[PreferenceKeys.userId] = session.userId
            preferences[PreferenceKeys.displayName] = session.displayName
            preferences[PreferenceKeys.email] = session.email
        }

        return Result.success(session)
    }

    override suspend fun signOut() {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.isAuthenticated] = false
            preferences.remove(PreferenceKeys.userId)
            preferences.remove(PreferenceKeys.displayName)
            preferences.remove(PreferenceKeys.email)
            preferences.remove(PreferenceKeys.photoUrl)
        }
    }
}
