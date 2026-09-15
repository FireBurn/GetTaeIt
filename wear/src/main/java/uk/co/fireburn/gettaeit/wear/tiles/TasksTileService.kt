package uk.co.fireburn.gettaeit.wear.tiles

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import uk.co.fireburn.gettaeit.wear.ui.MainActivity
import java.util.UUID
import javax.inject.Inject

/**
 * The next task for the current mode, with Done and Snooze.
 *
 * Delete deliberately stays in the app behind its cancel countdown: a stray tap on a
 * tile shouldn't lose a task for good.
 */
@AndroidEntryPoint
class TasksTileService : TileService() {

    @Inject
    lateinit var taskRepository: TaskRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
        serviceScope.future {
            handleClick(requestParams.currentState.lastClickableId)
            val nextTask = taskRepository.getTasksForCurrentMode().first().firstOrNull()
            TileBuilders.Tile.Builder()
                .setFreshnessIntervalMillis(FRESHNESS_MS)
                .setTileTimeline(
                    TimelineBuilders.Timeline.fromLayoutElement(layout(nextTask, System.currentTimeMillis()))
                )
                .build()
        }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Clickable ids look like `complete:<task id>:<layout time>`. A tile can hand the same id
     * back on later refreshes, so each one is acted on at most once.
     */
    private suspend fun handleClick(clickableId: String) {
        val parts = clickableId.split(':')
        if (parts.size != 3) return
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_CLICK, null) == clickableId) return
        prefs.edit().putString(KEY_LAST_CLICK, clickableId).apply()

        val task = runCatching { UUID.fromString(parts[1]) }.getOrNull()
            ?.let { taskRepository.getTaskById(it) }
            ?: return
        when (parts[0]) {
            ACTION_COMPLETE -> if (!task.isCompleted) taskRepository.completeTask(task)
            ACTION_SNOOZE -> taskRepository.snoozeTask(task, System.currentTimeMillis() + SNOOZE_MS)
        }
    }

    private fun layout(task: TaskEntity?, layoutTime: Long): LayoutElementBuilders.LayoutElement {
        val content = LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(header())
            .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(12f)).build())

        if (task == null) {
            content.addContent(text("Chill oot. Yer list is empty.", 15f, WHITE, maxLines = 2))
        } else {
            content.addContent(text(task.title, 16f, WHITE, maxLines = 2))
            content.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(12f)).build())
            content.addContent(
                LayoutElementBuilders.Row.Builder()
                    .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                    .addContent(actionButton("zZ", "Snooze for two hours", AMBER, "$ACTION_SNOOZE:${task.id}:$layoutTime"))
                    .addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(16f)).build())
                    .addContent(actionButton("✓", "Mark done", GREEN, "$ACTION_COMPLETE:${task.id}:$layoutTime"))
                    .build()
            )
        }

        val openApp = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(packageName)
                    .setClassName(MainActivity::class.java.name)
                    .build()
            )
            .build()

        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(ModifiersBuilders.Clickable.Builder().setId(OPEN_APP).setOnClick(openApp).build())
                    .build()
            )
            .addContent(content.build())
            .build()
    }

    // Text only: the tile carousel already shows the app icon beside it.
    private fun header(): LayoutElementBuilders.LayoutElement = text("Get Tae It", 14f, MUTED)

    /** 48dp buttons: the smallest target that's comfortable to hit on a wrist. */
    private fun actionButton(label: String, description: String, color: Int, clickableId: String) =
        LayoutElementBuilders.Box.Builder()
            .setWidth(dp(48f))
            .setHeight(dp(48f))
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(color))
                            .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(24f)).build())
                            .build()
                    )
                    .setClickable(
                        ModifiersBuilders.Clickable.Builder()
                            .setId(clickableId)
                            .setOnClick(ActionBuilders.LoadAction.Builder().build())
                            .build()
                    )
                    .setSemantics(
                        ModifiersBuilders.Semantics.Builder()
                            .setContentDescription(description)
                            .setRole(ModifiersBuilders.SEMANTICS_ROLE_BUTTON)
                            .build()
                    )
                    .build()
            )
            .addContent(text(label, 18f, WHITE))
            .build()

    private fun text(value: String, sizeSp: Float, color: Int, maxLines: Int = 1) =
        LayoutElementBuilders.Text.Builder()
            .setText(value)
            .setMaxLines(maxLines)
            .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE)
            .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setSize(sp(sizeSp))
                    .setColor(argb(color))
                    .build()
            )
            .build()

    private companion object {
        const val OPEN_APP = "open_app"
        const val ACTION_COMPLETE = "complete"
        const val ACTION_SNOOZE = "snooze"
        const val SNOOZE_MS = 2 * 60 * 60 * 1000L
        const val FRESHNESS_MS = 15 * 60 * 1000L
        const val PREFS = "tasks_tile"
        const val KEY_LAST_CLICK = "last_click"

        // White text keeps at least 4.5:1 contrast on both button colours.
        val WHITE = 0xFFFFFFFF.toInt()
        val MUTED = 0xFFCFC3D6.toInt()
        val GREEN = 0xFF2E7D32.toInt()
        val AMBER = 0xFFB45309.toInt()
    }
}
