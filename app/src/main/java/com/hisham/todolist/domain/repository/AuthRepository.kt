package com.hisham.todolist.domain.repository

import com.hisham.todolist.domain.model.AuthState
import com.hisham.todolist.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeAuthState(): Flow<AuthState>
    suspend fun getCurrentSession(): UserSession?
    suspend fun signInWithGoogle(): Result<UserSession>
    suspend fun signOut()
}
