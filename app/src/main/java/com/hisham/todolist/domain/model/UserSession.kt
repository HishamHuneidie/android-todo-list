package com.hisham.todolist.domain.model

data class UserSession(
    val userId: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
)
