package uk.co.fireburn.gettaeit.shared.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DataLayerEntryPoint {
    fun taskRepository(): TaskRepository
}
