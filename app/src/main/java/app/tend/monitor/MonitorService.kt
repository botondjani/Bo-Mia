package app.tend.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import app.tend.MainActivity
import app.tend.R
import app.tend.data.TendStore

/**
 * Watches the foreground app via UsageStats (polled every second):
 * - a blocked app coming to the front without a pass -> delay screen ([InterstitialActivity]),
 * - time in a blocked app counts against today's budget; at zero the app is locked,
 * - leaving a blocked app after a real session -> "Worth it?".
 * Every gap or uncaught open is written to the miss log.
 */
class MonitorService : Service() {
    private val store by lazy { TendStore(this) }
    private val handler = Handler(Looper.getMainLooper())
    private val usm by lazy { getSystemService(UsageStatsManager::class.java) }

    private var lastQuery = 0L
    private var lastTick = 0L
    private var foreground: String? = null
    private var sessionPkg: String? = null
    private var sessionStart = 0L
    private var interceptedPkg: String? = null
    private var interceptedAt = 0L

    private val tick = object : Runnable {
        override fun run() {
            poll()
            handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("tend_monitor", MODE_PRIVATE)
        val lastBeat = prefs.getLong("heartbeat", 0)
        val gap = (System.currentTimeMillis() - lastBeat) / 1000
        if (lastBeat > 0 && gap > GAP_LOG_SECONDS) store.logMiss("monitor_not_running_${gap}s")
        startInForeground()
        lastQuery = System.currentTimeMillis() - 5_000
        lastTick = System.currentTimeMillis()
        handler.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        store.logMiss("monitor_destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun poll() {
        val now = System.currentTimeMillis()
        getSharedPreferences("tend_monitor", MODE_PRIVATE).edit().putLong("heartbeat", now).apply()

        val events = usm.queryEvents(lastQuery, now)
        val e = UsageEvents.Event()
        var latest: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            @Suppress("DEPRECATION")
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) latest = e.packageName
        }
        lastQuery = now
        if (latest != null && latest != foreground) onForegroundChanged(foreground, latest, now)

        val blocked = store.blockedPackages
        val fg = foreground
        if (fg != null && fg == sessionPkg) {
            store.update { it.copy(usedSeconds = it.usedSeconds + (now - lastTick) / 1000) }
            if (store.isLocked()) {
                endSession(now, askWorthIt = false)
                intercept(fg, now)
            }
        } else if (fg != null && fg in blocked && fg == interceptedPkg &&
            now - interceptedAt > MISS_AFTER_MS
        ) {
            // The delay screen should have covered the app by now.
            store.logMiss("open_not_caught", fg)
            interceptedPkg = null
        }
        lastTick = now - (now - lastTick) % 1000
    }

    private fun onForegroundChanged(old: String?, new: String, now: Long) {
        foreground = new
        if (new == packageName) {
            interceptedPkg = null // our delay screen is showing
            return
        }
        if (old != null && old == sessionPkg) endSession(now, askWorthIt = true)
        if (new in store.blockedPackages) {
            if (!store.isLocked() && store.consumePass(new)) {
                sessionPkg = new
                sessionStart = now
            } else {
                intercept(new, now)
            }
        }
    }

    private fun intercept(pkg: String, now: Long) {
        if (!android.provider.Settings.canDrawOverlays(this)) return nudge(pkg)
        interceptedPkg = pkg
        interceptedAt = now
        startActivity(InterstitialActivity.intent(this, pkg))
    }

    /**
     * Limited mode (no "display over other apps"): Android won't let us show the pause, so a
     * notification nudges instead; tapping it opens the pause. Minutes are still counted.
     */
    private fun nudge(pkg: String) {
        val cfg0 = store.config
        if (!app.tend.reminder.RecapReminder.canNotify(this)) {
            if (!app.tend.core.Budget.isLocked(cfg0, store.today())) { sessionPkg = pkg; sessionStart = System.currentTimeMillis() }
            return
        }
        val label = runCatching { packageManager.getApplicationInfo(pkg, 0).loadLabel(packageManager).toString() }.getOrDefault(pkg)
        val cfg = store.config
        val day = store.today()
        val text = if (app.tend.core.Budget.isLocked(cfg, day)) "Out of minutes for today. ${store.avatar.displayName} is here if you want to earn some."
            else "${app.tend.core.Budget.remainingMinutes(cfg, day)} min left today. Worth it right now?"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(NUDGE_CHANNEL, "Pause nudges", NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(this, 3, InterstitialActivity.intent(this, pkg), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        nm.notify(
            3,
            Notification.Builder(this, NUDGE_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Opening $label")
                .setContentText(text)
                .setContentIntent(open)
                .setAutoCancel(true)
                .setTimeoutAfter(60_000)
                .build(),
        )
        // Without the pause, the open itself is the session start (not when locked: one nudge, no loop).
        if (!app.tend.core.Budget.isLocked(cfg, day)) {
            sessionPkg = pkg
            sessionStart = System.currentTimeMillis()
        }
    }

    private fun endSession(now: Long, askWorthIt: Boolean) {
        val pkg = sessionPkg ?: return
        val seconds = (now - sessionStart) / 1000
        sessionPkg = null
        if (askWorthIt && seconds >= WORTH_IT_MIN_SECONDS) {
            // A moment later, so the closing swipes and back presses don't land on the question;
            // skipped if a paused app is open again by then.
            handler.postDelayed({
                if (sessionPkg == null && foreground !in store.blockedPackages) startActivity(WorthItActivity.intent(this, pkg, seconds))
            }, WORTH_IT_DELAY_MS)
        }
    }

    private fun TendStore.isLocked() = app.tend.core.Budget.isLocked(config, today())

    private fun startInForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Companion", NotificationManager.IMPORTANCE_MIN))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${store.avatar.displayName} is keeping you company")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    companion object {
        private const val CHANNEL = "monitor"
        private const val NUDGE_CHANNEL = "nudge"
        /** How often the foreground app is checked: fast enough that the pause beats the app's first scroll. */
        private const val POLL_MS = 300L
        private const val MISS_AFTER_MS = 3000L
        private const val GAP_LOG_SECONDS = 120
        private const val WORTH_IT_DELAY_MS = 1500L
        const val WORTH_IT_MIN_SECONDS = 30

        fun start(context: Context) {
            context.startForegroundService(Intent(context, MonitorService::class.java))
        }
    }
}

/** Restarts monitoring after a reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        app.tend.reminder.RecapReminder.schedule(context) // alarms don't survive a reboot
        app.tend.reminder.WeeklyLetter.schedule(context)
        if (TendStore(context).blockedPackages.isNotEmpty()) MonitorService.start(context)
    }
}
