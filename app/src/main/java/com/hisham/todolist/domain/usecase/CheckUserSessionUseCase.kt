package com.hisham.todolist.domain.usecase

import com.hisham.todolist.domain.model.UserSession
import com.hisham.todolist.domain.repository.AuthRepository
import javax.inject.Inject

class CheckUserSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): UserSession? = authRepository.getCurrentSession()
}
