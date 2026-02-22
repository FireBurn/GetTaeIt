package uk.co.fireburn.gettaeit.shared.domain.scheduling

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import uk.co.fireburn.gettaeit.shared.di.DataLayerEntryPoint

class RecurrenceResetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Retrieve the repository via Hilt EntryPoint since Workers can't easily
            // inject dependencies directly without a custom HiltWorkerFactory
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                DataLayerEntryPoint::class.java
            )
            val taskRepository = entryPoint.taskRepository()

            // Reset any recurring tasks whose nextOccurrenceAt has passed
            taskRepository.resetDueRecurrences()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
