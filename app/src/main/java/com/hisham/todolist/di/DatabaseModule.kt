package com.hisham.todolist.di

import android.content.Context
import androidx.room.Room
import com.hisham.todolist.data.local.dao.TaskDao
import com.hisham.todolist.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "todo-list.db",
    ).addMigrations(AppDatabase.MIGRATION_2_3)
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideTaskDao(
        appDatabase: AppDatabase,
    ): TaskDao = appDatabase.taskDao()
}
