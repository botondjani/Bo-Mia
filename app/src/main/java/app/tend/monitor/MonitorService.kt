package app.tend.monitor

import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
    /** When a time-boxed open ("Open for 5 min") runs out. */
    private var sessionEnds = Long.MAX_VALUE
    /**
     * Left the paused app, but maybe only for a moment: a link opened in the browser, the share
     * sheet, a permission dialog. The session ends (and "Worth it?" asks) only on the home screen,
     * or after a while in a real other app; coming back in time just carries on.
     */
    private var awaySince = 0L
    private val homes by lazy {
        packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }.toSet()
    }
    /** Screens apps open on top of themselves for a moment: browsers (custom tabs), share sheet, dialogs. */
    private val helpers by lazy {
        packageManager.queryIntentActivities(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://example.com")), 0)
            .map { it.activityInfo.packageName }.toSet() + HELPER_PACKAGES
    }
    /**
     * What stays open while an activity runs on its timer (everything else waits): calls, the clock,
     * settings, the keyboard and the system's own screens.
     */
    private val essentials by lazy {
        val dialer = runCatching { getSystemService(android.telecom.TelecomManager::class.java).defaultDialerPackage }.getOrNull()
        val clock = packageManager.queryIntentActivities(Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS), 0).map { it.activityInfo.packageName }
        val keyboard = runCatching { android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.DEFAULT_INPUT_METHOD)?.substringBefore('/') }.getOrNull()
        HELPER_PACKAGES + listOfNotNull(dialer, keyboard) + clock + setOf("com.android.settings", "com.android.emergency", "com.google.android.dialer", "com.android.server.telecom")
    }
    /** Last background Health Connect read. */
    private var lastSync = 0L
    private var lastInbox = 0L
    /** Time in paused apps in a row (breaks under [STREAK_GAP_MS] don't count): for the companion's peeks. */
    private var streakStart = 0L
    private var lastLeft = 0L
    private var nextPeekAt = Long.MAX_VALUE
    private val peek by lazy { Peek(this) }
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main) // store writes stay on the main thread, like the poll
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
        // Stopped for over 10 minutes while the phone was on: likely a battery saver. Home offers a fix.
        if (lastBeat > 0 && gap > 600 && store.onboarded) store.monitorSlept = true
        startInForeground()
        lastQuery = System.currentTimeMillis() - 5_000
        lastTick = System.currentTimeMillis()
        handler.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        peek.hide()
        scope.cancel()
        handler.removeCallbacks(tick)
        store.logMiss("monitor_destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun poll() {
        val now = System.currentTimeMillis()
        if (now - lastSync > SYNC_EVERY_MS) {
            lastSync = now
            scope.launch { backgroundSync() }
        }
        // Friends more often: a wave or a prank should reach the widget while it's still news.
        if (now - lastInbox > INBOX_EVERY_MS) {
            lastInbox = now
            scope.launch {
                if (runCatching { app.tend.data.Friends.syncInbox(this@MonitorService) }.getOrDefault(false)) app.tend.widget.Widgets.refresh(this@MonitorService)
                // Who's at the fire: the widget shows them, and a new one gets a note.
                if (app.tend.data.Account.user(this@MonitorService) != null) {
                    val before = app.tend.data.Friends.atFire(this@MonitorService)
                    runCatching { app.tend.data.Friends.list(this@MonitorService) }
                    if (app.tend.data.Friends.atFire(this@MonitorService) != before) app.tend.widget.Widgets.refresh(this@MonitorService)
                    app.tend.data.Friends.notifyAtFire(this@MonitorService)
                }
            }
        }
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
        // On a break: nothing is paused, counted or closed.
        if (store.isPaused) {
            if (sessionPkg != null) endSession(now, askWorthIt = false)
            lastTick = now
            return
        }
        if (sessionPkg != null && awaySince > 0 && fg != null) {
            val waited = now - awaySince
            when {
                fg in helpers -> if (waited > HELPER_MAX_MS) endSession(now, askWorthIt = false)
                waited > AWAY_CONFIRM_MS -> endSession(now, askWorthIt = true) // really in another app
            }
        }
        if (fg != null && fg == sessionPkg && (now > sessionEnds || scheduledLock() != null)) {
            // The chosen minutes are up, or a scheduled lock just began: check in again, kindly.
            val timeUp = now > sessionEnds
            endSession(now, askWorthIt = false)
            intercept(fg, now, timeUp = timeUp)
        } else if (fg != null && fg == sessionPkg && store.borrowing(fg)) {
            // Borrowed minutes: not counted, and the session's own end checks in again.
        } else if (fg != null && fg == sessionPkg) {
            val seconds = (now - lastTick) / 1000
            store.update { it.copy(usedSeconds = it.usedSeconds + seconds) }
            store.addAppSeconds(fg, seconds)
            app.tend.reminder.Notes.checkLowMinutes(this)
            maybePeek(fg, now)
            // The day's minutes are used up, or this app's own limit is.
            if (store.isLocked() || store.appLocked(fg)) {
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
        if (sessionPkg != null && new == sessionPkg && awaySince > 0) {
            awaySince = 0 // back from the browser or the share sheet: same session, no new pause
            return
        }
        if (old != null && old == sessionPkg) {
            if (new in homes) endSession(now, askWorthIt = true) else awaySince = now
        } else if (awaySince > 0 && new in homes) {
            endSession(now, askWorthIt = true) // from the helper screen straight home
        }
        // An activity on its timer (reading, phone-free, a workout): every app waits until it's done, not only the paused ones.
        if (store.onboarded && !store.isPaused && (app.tend.health.TaskSync.focusSecondsLeft(store) ?: 0L) > 0L && new !in homes && new !in essentials) {
            if (sessionPkg != null) endSession(now, askWorthIt = false)
            if (!android.provider.Settings.canDrawOverlays(this)) return
            peek.hide()
            interceptedPkg = new
            interceptedAt = now
            startActivity(InterstitialActivity.intent(this, new, focus = true))
            return
        }
        if (store.onboarded && new in store.blockedPackages && !store.isPaused) {
            if (sessionPkg != null) endSession(now, askWorthIt = false) // another paused app: the pause takes over
            val pass = if (((store.isLocked() || store.appLocked(new)) && !store.borrowing(new)) || scheduledLock() != null) null else store.consumePass(new)
            if (pass != null) {
                sessionPkg = new
                sessionStart = now
                sessionEnds = app.tend.core.TimeBox.endsAt(now, pass)
                // Carrying on after a short break (another "15 more", a quick look elsewhere): the same stretch.
                if (now - lastLeft > STREAK_GAP_MS || streakStart == 0L) { streakStart = now; nextPeekAt = now + Peek.EVERY_MS }
            } else {
                intercept(new, now)
            }
        }
    }

    private fun scheduledLock() = store.activeLock()

    /**
     * Every ten minutes in a row in paused apps, the companion peeks in from the edge: not right
     * after a pause screen, and not when one is about to come anyway.
     */
    private fun maybePeek(pkg: String, now: Long) {
        if (streakStart == 0L) { streakStart = sessionStart; nextPeekAt = sessionStart + Peek.EVERY_MS }
        if (now < nextPeekAt || peek.showing) return
        nextPeekAt = now + Peek.EVERY_MS
        if (!store.peekOn || !android.provider.Settings.canDrawOverlays(this)) return
        if (now - sessionStart < 90_000L || sessionEnds - now < 60_000L) return
        val minutes = ((now - streakStart) / 60_000L).toInt()
        val label = runCatching { packageManager.getApplicationInfo(pkg, 0).loadLabel(packageManager).toString() }.getOrDefault("this app")
        app.tend.data.Stats.log(this, "peek", mapOf("minutes" to minutes))
        peek.show(
            store.avatar, store.accessories().first, minutes, label,
            onCome = {
                app.tend.data.Stats.log(this, "peek_come", mapOf("minutes" to minutes))
                startActivity(Intent(this, app.tend.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
            },
            onMore = { app.tend.data.Stats.log(this, "peek_more", mapOf("minutes" to minutes)) },
        )
    }

    /** Three days before the Plus trial ends: one note, with what it has given so far. */
    private fun trialNote() {
        if (!store.onboarded || store.plusPaid || store.trialNoteSent || !store.isPlus || store.trialDaysLeft > 3) return
        if (java.time.LocalTime.now().hour !in 10..20 || !app.tend.reminder.RecapReminder.canNotify(this)) return
        store.trialNoteSent = true
        val won = store.normalMinutes?.let { app.tend.core.InsightsOf.of(store.daySummaries(), it, 0, java.time.LocalDate.now()).wonBack } ?: 0
        val days = store.trialDaysLeft
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(PLUS_CHANNEL, "tend Plus", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(this, 31, Intent(this, app.tend.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE)
        nm.notify(
            31,
            Notification.Builder(this, PLUS_CHANNEL).setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("$days more day${if (days == 1) "" else "s"} of tend Plus")
                .setContentText(if (won >= 60) "You've won back ${app.tend.core.InsightsOf.hours(won)} so far. Your time stays yours either way." else "Your weeks, each app's limit and strict mode are yours until then.")
                .setContentIntent(open).setAutoCancel(true).build(),
        )
        app.tend.data.Stats.log(this, "trial_note")
    }

    private fun intercept(pkg: String, now: Long, timeUp: Boolean = false) {
        if (!android.provider.Settings.canDrawOverlays(this)) return nudge(pkg)
        peek.hide()
        interceptedPkg = pkg
        interceptedAt = now
        startActivity(InterstitialActivity.intent(this, pkg, timeUp))
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

    /**
     * Every 15 minutes: Health Connect totals, so a walk or a workout counts without opening tend,
     * with a note when one completes (and when it brings a new accessory).
     */
    private suspend fun backgroundSync() {
        trialNote()
        // The town's weather for the widgets (at most hourly; nothing without a town).
        if (runCatching { app.tend.data.WeatherService.refresh(this) }.getOrDefault(false)) app.tend.widget.Widgets.refresh(this)
        if (!store.onboarded || store.selectedTasks.none { it.verification == app.tend.core.Verification.AUTOMATIC }) return
        val done = runCatching { app.tend.health.TaskSync.sync(this) }.getOrNull() ?: return
        if (done.tasks.isEmpty() || !app.tend.reminder.RecapReminder.canNotify(this)) return
        val name = store.avatar.displayName
        val fresh = store.takeNewAccessories()
        val text = "+${done.minutes} min. $name is proud of you." + fresh.joinToString("") { " And $name got a ${it.title.lowercase()}!" }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(DONE_CHANNEL, "Activities done", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(this, 9, Intent(this, app.tend.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE)
        nm.notify(
            13,
            Notification.Builder(this, DONE_CHANNEL).setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(done.tasks.joinToString(" and ") { it.title } + " done!")
                .setContentText(text).setStyle(Notification.BigTextStyle().bigText(text))
                .setContentIntent(open).setAutoCancel(true).build(),
        )
        app.tend.widget.Widgets.refresh(this)
    }

    private fun endSession(now: Long, askWorthIt: Boolean) {
        val pkg = sessionPkg ?: return
        lastLeft = now
        peek.hide()
        val seconds = (now - sessionStart) / 1000
        sessionPkg = null
        sessionEnds = Long.MAX_VALUE
        awaySince = 0
        // Asked rarely: after a long stretch only, and at most twice a day.
        if (askWorthIt && seconds >= WORTH_IT_MIN_SECONDS && store.worthItAskedToday() < WORTH_IT_PER_DAY) {
            // A moment later, so the closing swipes and back presses don't land on the question;
            // skipped if a paused app is open again by then.
            handler.postDelayed({
                // Only on the home screen or in a real other app, never over a screen inside the app.
                val fg = foreground
                if (sessionPkg == null && fg !in store.blockedPackages && fg !in helpers && fg != packageName) {
                    store.markWorthItAsked()
                    startActivity(WorthItActivity.intent(this, pkg, seconds))
                }
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
        private const val SYNC_EVERY_MS = 15 * 60_000L
        private const val INBOX_EVERY_MS = 5 * 60_000L
        private const val STREAK_GAP_MS = 3 * 60_000L
        private const val PLUS_CHANNEL = "plus"
        private const val AWAY_CONFIRM_MS = 8_000L
        private const val HELPER_MAX_MS = 10 * 60_000L
        private val HELPER_PACKAGES = setOf(
            "android", "com.android.systemui", "com.android.intentresolver", "com.android.permissioncontroller",
            "com.google.android.permissioncontroller", "com.google.android.gms", "com.android.packageinstaller",
            "com.google.android.packageinstaller", "com.google.android.documentsui", "com.android.documentsui",
        )
        private const val DONE_CHANNEL = "done"
        const val WORTH_IT_MIN_SECONDS = 5 * 60
        const val WORTH_IT_PER_DAY = 2

        fun start(context: Context) {
            context.startForegroundService(Intent(context, MonitorService::class.java))
        }
    }
}

/** Restarts monitoring after a reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        app.tend.reminder.RecapReminder.schedule(context) // alarms don't survive a reboot or an update
        app.tend.reminder.WeeklyLetter.schedule(context)
        app.tend.reminder.MorningNote.schedule(context)
        app.tend.reminder.DayClock.schedule(context)
        val store = TendStore(context)
        // A timer that was running keeps its end alarm.
        if (store.focusStartedAt > 0) {
            app.tend.health.TimerAlarm.schedule(context, store.focusStartedAt + store.focusTask.target * 60_000L + 2_000L)
        }
        // Only once setup is done: the apps are pre-selected from the first start, the pause isn't.
        if (store.onboarded && store.blockedPackages.isNotEmpty()) MonitorService.start(context)
    }
}
