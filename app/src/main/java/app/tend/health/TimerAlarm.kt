package app.tend.health

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
import app.tend.data.TendStore
import app.tend.reminder.RecapReminder

/** Finishes a running timer when its time is up, even if tend isn't open, and says so. */
object TimerAlarm {
    private const val CHANNEL = "timers"

    fun schedule(context: Context, at: Long) {
        // Inexact is fine: a minute either way doesn't matter, and it needs no exact-alarm permission.
        context.getSystemService(AlarmManager::class.java).setWindow(AlarmManager.RTC_WAKEUP, at, 60_000L, intent(context))
    }

    fun cancel(context: Context) = context.getSystemService(AlarmManager::class.java).cancel(intent(context))

    private fun intent(context: Context) = PendingIntent.getBroadcast(
        context, 5, Intent(context, TimerReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    internal fun notify(context: Context, result: TaskSync.TimerResult) {
        if (!RecapReminder.canNotify(context)) return
        val store = TendStore(context)
        val task = store.focusTask
        val name = store.avatar.displayName
        val (title, text) = when {
            result == TaskSync.TimerResult.BROKEN ->
                "The phone came back a little early" to "No worries. ${task.title} is there whenever you want another go."
            task.id == app.tend.core.Tasks.TIMER_ID -> "Focus time done" to "${task.target} minutes, all yours. $name kept time."
            app.tend.core.PhoneFree.runsPhoneFree(task) -> "${task.title} done, phone-free" to "+${task.rewardMinutes} min. $name is proud of you."
            else -> "${task.title} done" to "+${task.rewardMinutes} min. $name enjoyed that."
        }.let { (t, body) ->
            // A new accessory is the best news of all: say it.
            val fresh = if (result == TaskSync.TimerResult.DONE) store.takeNewAccessories() else emptySet()
            t to (body + fresh.joinToString("") { " And $name got a ${it.title.lowercase()}!" })
        }
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Timers", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 6, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        nm.notify(
            4,
            Notification.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title).setContentText(text).setContentIntent(open).setAutoCancel(true).build(),
        )
    }

    private const val RUNNING_CHANNEL = "timer_running"
    private const val RUNNING_ID = 8

    /** While a timer runs: a quiet notification that counts down to [end]. */
    fun showRunning(context: Context, task: app.tend.core.TaskDef, end: Long) {
        if (!RecapReminder.canNotify(context)) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(RUNNING_CHANNEL, "Running timer", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(context, 7, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        nm.notify(
            RUNNING_ID,
            Notification.Builder(context, RUNNING_CHANNEL).setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(task.title).setContentText("Every app waits until it's done.")
                .setWhen(end).setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true)
                .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(open)
                // Gone by itself at the end, even if the alarm comes a little late.
                .setTimeoutAfter((end - System.currentTimeMillis()).coerceAtLeast(1_000L))
                .build(),
        )
    }

    fun hideRunning(context: Context) = context.getSystemService(NotificationManager::class.java).cancel(RUNNING_ID)
}

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TaskSync.checkTimer(context)?.let { TimerAlarm.notify(context, it) }
    }
}
