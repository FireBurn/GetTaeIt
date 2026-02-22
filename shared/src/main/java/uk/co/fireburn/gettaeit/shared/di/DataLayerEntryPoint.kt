package uk.co.fireburn.gettaeit.shared.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uk.co.fireburn.gettaeit.shared.domain.GeofenceManager
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import uk.co.fireburn.gettaeit.shared.domain.ai.HybridTaskService

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DataLayerEntryPoint {
    fun taskRepository(): TaskRepository
    fun geofenceManager(): GeofenceManager
    fun hybridTaskService(): HybridTaskService
}
