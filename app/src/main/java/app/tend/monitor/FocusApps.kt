package app.tend.monitor

import android.content.Context
import android.content.Intent
import app.tend.core.TaskDef
import app.tend.core.Tasks

/** What stays open while an activity runs on its timer; everything else waits (and counts as using the phone). */
object FocusApps {
    /** Screens apps open on top of themselves for a moment, and the system's own. */
    val HELPER_PACKAGES = setOf(
        "android", "com.android.systemui", "com.android.intentresolver", "com.android.permissioncontroller",
        "com.google.android.permissioncontroller", "com.google.android.gms", "com.android.packageinstaller",
        "com.google.android.packageinstaller", "com.google.android.documentsui", "com.android.documentsui",
    )

    /** Workout trackers: a glance at the run is part of the workout. */
    val FITNESS = setOf(
        "com.strava", "com.google.android.apps.fitness", "com.garmin.android.apps.connectmobile", "com.sec.android.app.shealth",
        "com.nike.plusgps", "com.runtastic.android", "com.fitbit.FitbitMobile", "fi.polar.polarflow", "com.huawei.health",
        "com.mi.health", "com.komoot.android", "com.suunto.android",
    )

    fun homes(context: Context) = context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
        .map { it.activityInfo.packageName }.toSet()

    /** Calls, the clock, settings, the keyboard and the system's own screens. */
    fun essentials(context: Context): Set<String> {
        val dialer = runCatching { context.getSystemService(android.telecom.TelecomManager::class.java).defaultDialerPackage }.getOrNull()
        val clock = context.packageManager.queryIntentActivities(Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS), 0).map { it.activityInfo.packageName }
        val keyboard = runCatching {
            android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.DEFAULT_INPUT_METHOD)?.substringBefore('/')
        }.getOrNull()
        return HELPER_PACKAGES + listOfNotNull(dialer, keyboard) + clock +
            setOf("com.android.settings", "com.android.emergency", "com.google.android.dialer", "com.android.server.telecom")
    }

    /** Open during [task] on top of [essentials]: trackers during a workout. */
    fun extra(task: TaskDef): Set<String> = if (task.id == Tasks.MOVE.id) FITNESS else emptySet()

    /** Everything that doesn't count as using the phone during [task]: tend, the home screen, the essentials, the extras. */
    fun open(context: Context, task: TaskDef) = homes(context) + essentials(context) + extra(task) + context.packageName
}
