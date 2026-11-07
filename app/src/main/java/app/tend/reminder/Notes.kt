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
import app.tend.core.Budget
import app.tend.core.Tasks
import app.tend.data.TendStore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * tend's notification rules, in one place:
 * - few and useful: at most [DAILY_CAP] a day across all kinds (timer results don't count, the user started them);
 * - never at night: morning notes and nudges only from 8:00 until an hour after the evening starts;
 * - each kind at most once a day, each switchable in settings, each on its own channel;
 * - kind words only: what's possible today, never what went wrong.
 */
object Notes {
    const val DAILY_CAP = 3

    enum class Kind(val channel: String, val channelName: String, val importance: Int, val notificationId: Int) {
        /** 8:30: today's minutes and the activities worth the most. Silent. */
        MORNING("morning", "Morning plan", NotificationManager.IMPORTANCE_LOW, 10),
        /** Once a day when 10 minutes are left. */
        LOW_MINUTES("nudges", "Gentle nudges", NotificationManager.IMPORTANCE_DEFAULT, 11),
        /** Instead of the morning note after two days without opening tend. */
        MISSED("nudges", "Gentle nudges", NotificationManager.IMPORTANCE_DEFAULT, 12),
    }

    /** Called by every kind of note (also the recap and the Sunday letter) to keep the daily cap. */
    fun underCap(store: TendStore) = store.notesSentToday() < DAILY_CAP

    private fun quietNow(store: TendStore): Boolean {
        val hour = LocalTime.now().hour
        return hour < 8 || hour > store.eveningFrom
    }

    private fun enabled(store: TendStore, kind: Kind) = when (kind) {
        Kind.MORNING, Kind.MISSED -> store.notesMorning
        Kind.LOW_MINUTES -> store.notesNudges
    }

    /** Posts [kind] if the rules allow it; true when it went out. */
    fun post(context: Context, kind: Kind, title: String, text: String): Boolean {
        val store = TendStore(context)
        if (!store.onboarded || !RecapReminder.canNotify(context) || !enabled(store, kind)) return false
        if (quietNow(store) || !underCap(store)) return false
        val today = LocalDate.now().toString()
        if (store.noteSentOn(kind.name) == today) return false
        store.markNoteSent(kind.name, today)

        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(kind.channel, kind.channelName, kind.importance))
        val open = PendingIntent.getActivity(
            context, 20 + kind.ordinal,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        nm.notify(
            kind.notificationId,
            Notification.Builder(context, kind.channel).setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title).setContentText(text).setStyle(Notification.BigTextStyle().bigText(text))
                .setContentIntent(open).setAutoCancel(true).build(),
        )
        return true
    }

    /** From the monitor while a paused app is in use: once a day when 10 minutes or fewer are left. */
    fun checkLowMinutes(context: Context) {
        val store = TendStore(context)
        val remaining = Budget.remainingMinutes(store.config, store.today())
        if (remaining !in 1..10 || store.noteSentOn(Kind.LOW_MINUTES.name) == LocalDate.now().toString()) return
        val next = Tasks.next(store.today(), store.selectedTasks)
        post(
            context, Kind.LOW_MINUTES, "$remaining minutes left today",
            next?.let { "${it.title} brings back +${it.rewardMinutes} min. ${store.avatar.displayName} is in." }
                ?: "Make them count. ${store.avatar.displayName} is here.",
        )
    }

    /** The 8:30 note: today's plan, or a hello after two quiet days. */
    fun morning(context: Context) {
        val store = TendStore(context)
        val name = store.avatar.displayName
        val away = System.currentTimeMillis() - store.lastOpenedAt > 2 * 24 * 3_600_000L && store.lastOpenedAt > 0
        if (away) {
            post(context, Kind.MISSED, "$name misses you", "The ${store.world.place} is still here. One small thing today is enough.")
            return
        }
        val cfg = store.config
        val best = store.selectedTasks.sortedByDescending { it.rewardMinutes }.take(2)
        val text = if (best.isEmpty()) "${cfg.freeMinutes} free minutes today. Pick something you'd rather do, and earn more."
        else "${cfg.freeMinutes} free minutes today. " + best.joinToString(" and ") { "${it.title} (+${it.rewardMinutes})" } + " would add ${best.sumOf { it.rewardMinutes }}."
        post(context, Kind.MORNING, "Good morning from $name", text)
    }
}

/** Schedules the 8:30 note (inexact, no exact-alarm permission needed). */
object MorningNote {
    private const val WINDOW_MS = 20 * 60_000L

    fun schedule(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 7, Intent(context, MorningNoteReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        alarms.cancel(pi)
        var at = LocalDate.now().atTime(8, 30)
        if (!at.isAfter(LocalDateTime.now())) at = at.plusDays(1)
        alarms.setWindow(AlarmManager.RTC_WAKEUP, at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), WINDOW_MS, pi)
    }
}

class MorningNoteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Notes.morning(context)
        MorningNote.schedule(context)
    }
}
