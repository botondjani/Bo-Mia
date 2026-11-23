package app.tend.monitor

import app.tend.core.tr

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.tend.core.Verification
import app.tend.data.TendStore
import app.tend.health.TaskSync

/**
 * "Phone down" in the quick settings: a tap opens a small picker (how many minutes, or the day's
 * next timed activity), a second one starts it. While one runs, it shows the minutes left and opens tend.
 */
class PhoneDownTile : TileService() {
    override fun onStartListening() = update()

    override fun onClick() {
        update()
        open()
    }

    private fun update() {
        val tile = qsTile ?: return
        val store = TendStore(this)
        val left = TaskSync.focusSecondsLeft(store)
        tile.state = if (left != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = tr("Phone down", "Telefon le")
        if (Build.VERSION.SDK_INT >= 29) tile.subtitle = when {
            left != null -> tr("${(left + 59) / 60} min left", "még ${(left + 59) / 60} perc")
            else -> tr("Pick how long", "Válassz időt")
        }
        tile.updateTile()
    }

    /** The picker, or tend itself while a timer runs (the picker passes straight on then). */
    // Below Android 14 only the Intent form exists; from 14 on, the PendingIntent one is used.
    @Suppress("DEPRECATION")
    @android.annotation.SuppressLint("StartActivityAndCollapseDeprecated")
    private fun open() {
        val intent = Intent(this, TimerPickActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(PendingIntent.getActivity(this, 31, intent, PendingIntent.FLAG_IMMUTABLE))
        else startActivityAndCollapse(intent)
    }

    companion object {
        /** The next activity of the day that runs on a timer or with the phone down, not done yet. */
        fun next(store: TendStore) = store.today().let { day ->
            store.selectedTasks.firstOrNull { it.id !in day.completedTasks && (it.verification.timed || it.verification == Verification.PHONE_FREE) }
        }
    }
}
