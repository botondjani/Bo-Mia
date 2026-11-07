package app.tend.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.tend.data.TendStore
import app.tend.widget.Widgets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The day's turning points, even when nobody touches the phone: just after midnight the day is
 * closed (acorns are counted, a fresh budget starts), and at the evening hour and at 5:00
 * the widgets switch between day and night. Inexact alarms, a minute either way is fine.
 */
object DayClock {
    private const val WINDOW_MS = 60_000L

    fun schedule(context: Context) {
        val store = TendStore(context)
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val next = listOf(
            today.plusDays(1).atTime(0, 0, 30), // midnight: close yesterday, start today
            today.atTime(store.eveningFrom, 0), // evening: sleepy widget, the camp at night
            today.atTime(5, 0), // morning: day again
            today.plusDays(1).atTime(5, 0),
        ).filter { it.isAfter(now) }.min()
        val pi = PendingIntent.getBroadcast(context, 8, Intent(context, DayClockReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        context.getSystemService(AlarmManager::class.java)
            .setWindow(AlarmManager.RTC_WAKEUP, next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), WINDOW_MS, pi)
    }
}

class DayClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TendStore(context).today() // rolls the day over when it's past midnight
        Widgets.refresh(context, force = true)
        DayClock.schedule(context)
    }
}
