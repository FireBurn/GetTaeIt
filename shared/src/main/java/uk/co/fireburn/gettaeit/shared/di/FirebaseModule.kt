package uk.co.fireburn.gettaeit.shared.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uk.co.fireburn.gettaeit.shared.data.AuthRepositoryImpl
import uk.co.fireburn.gettaeit.shared.domain.AuthRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
