package uk.co.fireburn.gettaeit.auto.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uk.co.fireburn.gettaeit.auto.AutoViewModelFactory

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ViewModelFactoryProvider {
    fun autoViewModelFactory(): AutoViewModelFactory
}
