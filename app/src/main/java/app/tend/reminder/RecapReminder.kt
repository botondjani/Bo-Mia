package app.tend.reminder

import app.tend.core.tr

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import app.tend.MainActivity
import app.tend.R
import app.tend.core.Recap
import app.tend.data.TendStore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * A gentle evening note pointing to tonight's recap. Inexact daily alarm (no exact-alarm
 * permission needed), rescheduled after every firing, on app start and after reboot.
 */
object RecapReminder {
    private const val CHANNEL = "recap"
    private const val WINDOW_MS = 10 * 60_000L
    const val EXTRA_OPEN = "open"
    /** Opened from a pause: home, with the activities in view. */
    const val OPEN_ACTIVITIES = "activities"
    const val OPEN_RECAP = "recap"

    fun schedule(context: Context) {
        val store = TendStore(context)
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(context)
        alarms.cancel(pi)
        if (!store.reminderEnabled) return
        var at = LocalDate.now().atTime(store.eveningFrom, 0)
        if (!at.isAfter(LocalDateTime.now())) at = at.plusDays(1)
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarms.setWindow(AlarmManager.RTC_WAKEUP, millis, WINDOW_MS, pi)
    }

    fun canNotify(context: Context) = Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    internal fun post(context: Context) {
        val store = TendStore(context)
        if (!store.reminderEnabled || !canNotify(context)) return
        if (store.recapSeenOn == LocalDate.now().toString()) return // already looked tonight
        val recap = Recap.of(store.config, store.today(), store.worthItToday(), store.normalMinutes)
        val name = store.avatar.displayName
        val text = when {
            recap.gotBackMinutes > 0 && recap.tonightAcorns > 0 -> tr("You got back ${recap.gotBackMinutes} min today: +${recap.tonightAcorns} acorns for the camp at midnight.", "Ma ${recap.gotBackMinutes} percet nyertél vissza: éjfélkor +${recap.tonightAcorns} makk jár érte a tábornak.")
            recap.gotBackMinutes > 0 -> tr("You got back ${recap.gotBackMinutes} min today. $name is by the fire.", "Ma ${recap.gotBackMinutes} percet nyertél vissza. $name a tűznél ül.")
            else -> tr("Today used it all, and that's okay. $name saved you a seat by the fire.", "A mai nap mindet elvitte, és ez rendben van. $name foglalt neked helyet a tűznél.")
        }

        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, tr("Evening recap", "Esti összefoglaló"), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(
            context, 1,
            Intent(context, MainActivity::class.java).putExtra(EXTRA_OPEN, OPEN_RECAP)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        store.countNote()
        nm.notify(
            2,
            Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(tr("Tonight's recap is ready", "Kész a ma esti összefoglaló"))
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun alarmIntent(context: Context) = PendingIntent.getBroadcast(
        context, 0, Intent(context, RecapReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

class RecapReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RecapReminder.post(context)
        RecapReminder.schedule(context)
    }
}
