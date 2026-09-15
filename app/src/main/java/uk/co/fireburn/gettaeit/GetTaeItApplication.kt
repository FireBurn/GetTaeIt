package uk.co.fireburn.gettaeit

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.notifications.TaskNotificationManager
import uk.co.fireburn.gettaeit.shared.WearTaskSync
import uk.co.fireburn.gettaeit.shared.data.FirestoreTaskSync
import uk.co.fireburn.gettaeit.shared.domain.GeofenceManager
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import uk.co.fireburn.gettaeit.shared.domain.UserPreferencesRepository
import uk.co.fireburn.gettaeit.shared.domain.scheduling.RecurrenceResetWorker
import uk.co.fireburn.gettaeit.widgets.refreshTaskWidgets
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class GetTaeItApplication : Application() {

    @Inject
    lateinit var firestoreTaskSync: FirestoreTaskSync

    @Inject
    lateinit var wearTaskSync: WearTaskSync

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var geofenceManager: GeofenceManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        TaskNotificationManager.createChannel(this)
        scheduleRecurrenceWorker()
        firestoreTaskSync.start()
        wearTaskSync.start()
        observeTaskWidgetUpdates()
        restoreWorkGeofence()
    }

    /**
     * Geofences don't survive a reboot, location being switched off, or Play services data
     * being cleared. Registering again is harmless, and its initial trigger also tells us
     * straight away if we're already at work.
     */
    private fun restoreWorkGeofence() {
        applicationScope.launch {
            val prefs = userPreferencesRepository.getUserPreferences().first()
            prefs.workLocationLatLng?.let { (lat, lng) ->
                geofenceManager.addWorkGeofence(lat, lng, prefs.workLocationRadius)
            }
        }
    }

    private fun scheduleRecurrenceWorker() {
        val constraints = Constraints.Builder().build()
        val resetWork = PeriodicWorkRequestBuilder<RecurrenceResetWorker>(2, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "RecurrenceResetWork",
            ExistingPeriodicWorkPolicy.KEEP,
            resetWork
        )
    }

    private fun observeTaskWidgetUpdates() {
        applicationScope.launch {
            // Every change, completions included, so the widgets never show a finished task.
            taskRepository.getAllTasksFlow().collectLatest {
                refreshTaskWidgets(this@GetTaeItApplication)
            }
        }
    }
}
