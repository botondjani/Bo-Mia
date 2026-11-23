package app.tend.monitor

import app.tend.core.tr

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.tend.core.TaskDef
import app.tend.core.Tasks
import app.tend.data.TendStore
import app.tend.health.TaskSync
import app.tend.ui.Brand
import app.tend.ui.PopupSheet
import app.tend.ui.TendTheme

/**
 * From the "Phone down" tile: how long, in one tap (the last length picked stands out), or the
 * day's next timed activity under it. Then tend, where the timer runs.
 */
class TimerPickActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = TendStore(this)
        if (store.focusStartedAt > 0) { open(); return }
        val last = store.focusTimerMinutes
        val next = PhoneDownTile.next(store)
        val start = { task: TaskDef -> TaskSync.startTimer(this, task); open() }
        setContent {
            TendTheme {
                PopupSheet(onDismiss = ::finish, centered = true) {
                    Text(tr("Phone down for…", "Telefon le, mennyi időre?"), style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        lengths(last).forEach { m ->
                            val picked = m == last
                            val ink = if (picked) androidx.compose.ui.graphics.Color.White else Brand.Moss
                            androidx.compose.foundation.layout.Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                    .background(if (picked) Brand.Moss else Brand.Moss.copy(alpha = 0.12f))
                                    .clickable { start(Tasks.timer(m)) }.padding(vertical = 12.dp),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            ) {
                                Text("$m", style = MaterialTheme.typography.titleLarge, color = ink, maxLines = 1)
                                Text(tr("min", "perc"), style = MaterialTheme.typography.labelMedium, color = ink, maxLines = 1)
                            }
                        }
                    }
                    next?.let { task ->
                        Text(
                            tr("Or: ${task.title} · ${task.target} min", "Vagy: ${task.title} · ${task.target} perc"),
                            style = MaterialTheme.typography.labelLarge,
                            color = Brand.Moss,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { start(task) }.padding(vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }

    /** tend, where the running timer shows. */
    private fun open() {
        startActivity(Intent(this, app.tend.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    companion object {
        /** 15, 25, 45 and 60 minutes; a last length that isn't one of them takes the nearest one's place. */
        fun lengths(last: Int): List<Int> = listOf(15, 25, 45, 60).let { l ->
            if (last in l) l else (l - l.minBy { kotlin.math.abs(it - last) } + last).sorted()
        }
    }
}
