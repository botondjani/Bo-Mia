package app.tend.monitor

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.tend.MainActivity
import app.tend.core.Verification
import app.tend.data.TendStore
import app.tend.health.TaskSync

/**
 * "Phone down" in the quick settings: one tap starts the next timed or phone-free activity (the
 * phone goes down, tend counts it), without opening anything first. While one runs, it shows the
 * minutes left and opens tend.
 */
class PhoneDownTile : TileService() {
    override fun onStartListening() = update()

    override fun onClick() {
        val store = TendStore(this)
        if (store.focusStartedAt <= 0) next(store)?.let { TaskSync.startTimer(this, it) }
        update()
        open()
    }

    /** The next activity of the day that runs on a timer or with the phone down, not done yet. */
    private fun next(store: TendStore) = store.today().let { day ->
        store.selectedTasks.firstOrNull { it.id !in day.completedTasks && (it.verification.timed || it.verification == Verification.PHONE_FREE) }
    }

    private fun update() {
        val tile = qsTile ?: return
        val store = TendStore(this)
        val left = TaskSync.focusSecondsLeft(store)
        tile.state = if (left != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Phone down"
        if (Build.VERSION.SDK_INT >= 29) tile.subtitle = when {
            left != null -> "${(left + 59) / 60} min left"
            else -> next(store)?.let { "${it.title} · ${it.target} min" } ?: "Open tend"
        }
        tile.updateTile()
    }

    /** tend, where the running activity shows. */
    @Suppress("DEPRECATION")
    private fun open() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(PendingIntent.getActivity(this, 31, intent, PendingIntent.FLAG_IMMUTABLE))
        else startActivityAndCollapse(intent)
    }
}
