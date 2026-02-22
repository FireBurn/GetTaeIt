package uk.co.fireburn.gettaeit

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import uk.co.fireburn.gettaeit.notifications.TaskNotificationManager

@HiltAndroidApp
class GetTaeItApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TaskNotificationManager.createChannel(this)
    }
}
