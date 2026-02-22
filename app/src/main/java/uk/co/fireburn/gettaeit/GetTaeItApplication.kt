package uk.co.fireburn.gettaeit

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import uk.co.fireburn.gettaeit.notifications.TaskNotificationManager
import uk.co.fireburn.gettaeit.shared.domain.scheduling.RecurrenceResetWorker
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class GetTaeItApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TaskNotificationManager.createChannel(this)
        scheduleRecurrenceWorker()
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
}
