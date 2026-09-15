package uk.co.fireburn.gettaeit.shared.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uk.co.fireburn.gettaeit.shared.DataLayerSync
import uk.co.fireburn.gettaeit.shared.WearTaskSync
import uk.co.fireburn.gettaeit.shared.domain.GeofenceManager
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import uk.co.fireburn.gettaeit.shared.domain.ai.HybridTaskService

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DataLayerEntryPoint {
    fun taskRepository(): TaskRepository
    fun dataLayerSync(): DataLayerSync
    fun wearTaskSync(): WearTaskSync
    fun geofenceManager(): GeofenceManager
    fun hybridTaskService(): HybridTaskService
    fun userPreferencesRepository(): uk.co.fireburn.gettaeit.shared.domain.UserPreferencesRepository
}
