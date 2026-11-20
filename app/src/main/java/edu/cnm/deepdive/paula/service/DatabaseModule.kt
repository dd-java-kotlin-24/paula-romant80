package edu.cnm.deepdive.paula.service

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import edu.cnm.deepdive.paula.model.dao.CategoryDao
import edu.cnm.deepdive.paula.model.dao.RoutineDao
import edu.cnm.deepdive.paula.model.dao.RoutineLogDao
import edu.cnm.deepdive.paula.model.dao.TaskDao
import edu.cnm.deepdive.paula.model.dao.UserDao
import edu.cnm.deepdive.paula.model.dao.VoicePromptDao
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PaulaDatabase =
        Room.databaseBuilder(
            context,
            PaulaDatabase::class.java,
            PaulaDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideUserDao(database: PaulaDatabase): UserDao =
        database.getUserDao()

    @Provides
    @Singleton
    fun provideCategoryDao(database: PaulaDatabase): CategoryDao =
        database.getCategoryDao()

    @Provides
    @Singleton
    fun provideVoicePromptDao(database: PaulaDatabase): VoicePromptDao =
        database.getVoicePromptDao()

    @Provides
    @Singleton
    fun provideTaskDao(database: PaulaDatabase): TaskDao =
        database.getTaskDao()

    @Provides
    @Singleton
    fun provideRoutineDao(database: PaulaDatabase): RoutineDao =
        database.getRoutineDao()

    @Provides
    @Singleton
    fun provideRoutineLogDao(database: PaulaDatabase): RoutineLogDao =
        database.getRoutineLogDao()

}
