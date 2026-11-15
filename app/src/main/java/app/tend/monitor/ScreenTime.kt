package app.tend.monitor

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.ZoneId

/** The phone's whole screen time (every app but the home screen), as Android's own events tell it. */
object ScreenTime {
    /** Minutes on [date] (up to now for today); null without usage access. */
    fun minutes(context: Context, date: LocalDate): Int? = runCatching {
        val zone = ZoneId.systemDefault()
        val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = minOf(System.currentTimeMillis(), date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
        val home = context.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName
        val events = context.getSystemService(UsageStatsManager::class.java).queryEvents(from, to)
        val e = UsageEvents.Event()
        var cur: String? = null
        var start = 0L
        var total = 0L
        var seen = false
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            seen = true
            @Suppress("DEPRECATION")
            when (e.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (cur != null) total += e.timeStamp - start
                    cur = e.packageName.takeIf { it != home }; start = e.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> if (e.packageName == cur) { total += e.timeStamp - start; cur = null }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.KEYGUARD_SHOWN -> { if (cur != null) total += e.timeStamp - start; cur = null }
            }
        }
        if (cur != null) total += to - start
        // No events at all: no usage access (a phone in use always has some).
        if (seen) (total / 60_000).toInt() else null
    }.getOrNull()
}
