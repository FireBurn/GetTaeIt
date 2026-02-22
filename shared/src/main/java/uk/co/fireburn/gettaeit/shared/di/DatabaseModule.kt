package uk.co.fireburn.gettaeit.shared.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uk.co.fireburn.gettaeit.shared.data.AppDatabase
import uk.co.fireburn.gettaeit.shared.data.TaskDao
import uk.co.fireburn.gettaeit.shared.data.TaskRepositoryImpl
import uk.co.fireburn.gettaeit.shared.data.UserPreferencesDao
import uk.co.fireburn.gettaeit.shared.data.UserPreferencesRepositoryImpl
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import uk.co.fireburn.gettaeit.shared.domain.UserPreferencesRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        impl: TaskRepositoryImpl
    ): TaskRepository

    @Binds
    abstract fun bindUserPreferencesRepository(
        impl: UserPreferencesRepositoryImpl
    ): UserPreferencesRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
            AppDatabase.getDatabase(context)

        @Provides
        fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

        @Provides
        fun provideUserPreferencesDao(db: AppDatabase): UserPreferencesDao = db.userPreferencesDao()
    }
}
