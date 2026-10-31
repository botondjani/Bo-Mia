package app.tend.reminder

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.tend.MainActivity
import app.tend.R
import app.tend.core.Patterns
import app.tend.data.TendStore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** A short note every Sunday evening: what the week gave back. Opens the garden & week page. */
object WeeklyLetter {
    private const val CHANNEL = "weekly"
    const val OPEN_GARDEN = "garden"

    fun schedule(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(context)
        alarms.cancel(pi)
        if (!TendStore(context).weeklyLetter) return
        var at = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(18, 30)
        if (!at.isAfter(LocalDateTime.now())) at = at.plusWeeks(1)
        alarms.setWindow(AlarmManager.RTC_WAKEUP, at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 20 * 60_000L, pi)
    }

    /** The week in two sentences; also used for the text of the notification. */
    fun text(store: TendStore): Pair<String, String> {
        val week = Patterns.week(LocalDate.now(), store.daySummaries(), store.answers())
        val h = week.gotBackMinutes / 60
        val m = week.gotBackMinutes % 60
        val gotBack = if (h > 0) "$h h $m min" else "$m min"
        val plants = week.days.count { it.unusedMinutes > 0 }
        val title = "Your week with ${store.avatar.displayName}"
        val body = buildString {
            append("You got back $gotBack")
            if (plants > 0) append(" and $plants ${if (plants == 1) "plant" else "plants"} grew")
            append(".")
            if (week.pauses > 0) append(" You let the scroll go ${week.letGo} of ${week.pauses} times.")
        }
        return title to body
    }

    internal fun post(context: Context) {
        val store = TendStore(context)
        if (!store.weeklyLetter || !store.onboarded || !RecapReminder.canNotify(context)) return
        val (title, body) = text(store)
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Weekly letter", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(
            context, 7,
            Intent(context, MainActivity::class.java).putExtra(RecapReminder.EXTRA_OPEN, OPEN_GARDEN)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        nm.notify(
            8,
            Notification.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title).setContentText(body).setStyle(Notification.BigTextStyle().bigText(body))
                .setContentIntent(open).setAutoCancel(true).build(),
        )
    }

    private fun alarmIntent(context: Context) = PendingIntent.getBroadcast(
        context, 9, Intent(context, WeeklyLetterReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

class WeeklyLetterReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WeeklyLetter.post(context)
        WeeklyLetter.schedule(context)
    }
}
