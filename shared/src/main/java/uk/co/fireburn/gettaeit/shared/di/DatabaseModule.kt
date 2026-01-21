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
    abstract fun bindUserPreferencesRepository(
        userPreferencesRepositoryImpl: UserPreferencesRepositoryImpl
    ): UserPreferencesRepository

    @Binds
    abstract fun bindTaskRepository(
        taskRepositoryImpl: TaskRepositoryImpl
    ): TaskRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
            return AppDatabase.getDatabase(context)
        }

        @Provides
        fun provideTaskDao(appDatabase: AppDatabase): TaskDao {
            return appDatabase.taskDao()
        }

        @Provides
        fun provideUserPreferencesDao(appDatabase: AppDatabase): UserPreferencesDao {
            return appDatabase.userPreferencesDao()
        }
    }
}
