package com.hisham.todolist.di

import com.hisham.todolist.data.repository.AuthRepositoryImpl
import com.hisham.todolist.data.repository.SettingsRepositoryImpl
import com.hisham.todolist.data.repository.TaskRepositoryImpl
import com.hisham.todolist.domain.repository.AuthRepository
import com.hisham.todolist.domain.repository.SettingsRepository
import com.hisham.todolist.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        taskRepositoryImpl: TaskRepositoryImpl,
    ): TaskRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl,
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl,
    ): SettingsRepository
}
