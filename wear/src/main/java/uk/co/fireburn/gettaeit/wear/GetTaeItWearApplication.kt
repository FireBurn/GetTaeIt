package uk.co.fireburn.gettaeit.wear

import android.app.Application
import android.content.ComponentName
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.WearTaskSync
import uk.co.fireburn.gettaeit.shared.data.TaskSyncStore
import uk.co.fireburn.gettaeit.wear.tiles.TasksTileService
import uk.co.fireburn.gettaeit.wear.widgets.NextTaskComplicationService
import javax.inject.Inject

@HiltAndroidApp
class GetTaeItWearApplication : Application() {

    @Inject
    lateinit var wearTaskSync: WearTaskSync

    @Inject
    lateinit var taskSyncStore: TaskSyncStore

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        wearTaskSync.start()
        refreshGlanceablesOnChange()
    }

    /** The tile and complication read Room when asked, so ask again whenever tasks change. */
    @OptIn(FlowPreview::class)
    private fun refreshGlanceablesOnChange() {
        applicationScope.launch {
            taskSyncStore.changes().debounce(500L).collect {
                TileService.getUpdater(this@GetTaeItWearApplication)
                    .requestUpdate(TasksTileService::class.java)
                ComplicationDataSourceUpdateRequester.create(
                    this@GetTaeItWearApplication,
                    ComponentName(this@GetTaeItWearApplication, NextTaskComplicationService::class.java)
                ).requestUpdateAll()
            }
        }
    }
}
