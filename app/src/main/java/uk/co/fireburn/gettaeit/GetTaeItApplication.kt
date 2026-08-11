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
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.notifications.TaskNotificationManager
import uk.co.fireburn.gettaeit.shared.data.FirestoreTaskSync
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import uk.co.fireburn.gettaeit.shared.domain.scheduling.RecurrenceResetWorker
import uk.co.fireburn.gettaeit.widgets.refreshTaskWidgets
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class GetTaeItApplication : Application() {

    @Inject
    lateinit var firestoreTaskSync: FirestoreTaskSync

    @Inject
    lateinit var taskRepository: TaskRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        TaskNotificationManager.createChannel(this)
        scheduleRecurrenceWorker()
        firestoreTaskSync.start()
        observeTaskWidgetUpdates()
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
            taskRepository.getAllActiveToplevelTasks().collectLatest {
                refreshTaskWidgets(this@GetTaeItApplication)
            }
        }
    }
}
