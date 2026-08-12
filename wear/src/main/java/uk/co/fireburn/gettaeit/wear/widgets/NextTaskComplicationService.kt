package uk.co.fireburn.gettaeit.wear.widgets

import android.content.ComponentName
import android.content.Context
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import javax.inject.Inject

@AndroidEntryPoint
class NextTaskComplicationService : SuspendingComplicationDataSourceService() {

    @Inject
    lateinit var taskRepository: TaskRepository

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return ShortTextComplicationData.Builder(
            PlainComplicationText.Builder("Task").build(),
            PlainComplicationText.Builder("Next Task").build()
        ).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return null

        val tasks = taskRepository.getTasksForCurrentMode().firstOrNull() ?: emptyList()
        val nextTask = tasks.firstOrNull()

        val text = nextTask?.title ?: "All done!"

        return ShortTextComplicationData.Builder(
            PlainComplicationText.Builder(text).build(),
            PlainComplicationText.Builder("Next Task").build()
        ).build()
    }
}
