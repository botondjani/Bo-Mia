package app.tend.ui

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class InstalledApp(val pkg: String, val label: String)

/** Launchable apps, split into well-known time sinks (suggested) and everything else. */
class AppCatalog(private val context: Context) {
    /** Loaded once, off the main thread when [preload] runs first (it takes a while with many apps). */
    val all: List<InstalledApp> by lazy {
        context.packageManager
        .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(context.packageManager).toString()) }
        .filter { it.pkg != context.packageName }
        .distinctBy { it.pkg }
        .sortedBy { it.label.lowercase() }
    }

    /**
     * Minutes a day each app was used over the last two weeks (empty without usage access).
     * The home screen and tend itself are left out.
     */
    val dailyMinutes: Map<String, Int> by lazy {
        runCatching {
            val usm = context.getSystemService(android.app.usage.UsageStatsManager::class.java)
            val now = System.currentTimeMillis()
            val days = 14
            val home = context.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName
            usm.queryAndAggregateUsageStats(now - days * 86_400_000L, now)
                .mapValues { (it.value.totalTimeInForeground / 60_000 / days).toInt() }
                .filter { (pkg, min) -> min >= 5 && pkg != home && pkg != context.packageName && pkg != "com.android.settings" }
        }.getOrDefault(emptyMap())
    }

    /**
     * The tiles shown first: the apps used most (when tend can see usage) and the social apps
     * installed, most used first, at most [MAX_TILES].
     */
    val suggested: List<InstalledApp> by lazy {
        val usage = dailyMinutes
        val used = all.filter { (usage[it.pkg] ?: 0) > 0 }.sortedByDescending { usage[it.pkg] }.take(MAX_TILES)
        val social = all.filter { it.pkg in SUGGESTED }.sortedBy { SUGGESTED.indexOf(it.pkg) }
        (used + social).distinctBy { it.pkg }.sortedByDescending { usage[it.pkg] ?: 0 }.take(MAX_TILES)
    }

    val others: List<InstalledApp> by lazy { all - suggested.toSet() }

    /** Reads the app list in the background so the first screens don't wait for it. */
    fun preload(then: (AppCatalog) -> Unit = {}) = Thread({ others; then(this) }, "tend-apps").start()

    fun label(pkg: String): String = all.firstOrNull { it.pkg == pkg }?.label ?: pkg

    private val icons = HashMap<String, ImageBitmap?>()

    fun icon(pkg: String): ImageBitmap? = icons.getOrPut(pkg) {
        runCatching { context.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() }.getOrNull()
    }

    companion object {
        const val MAX_TILES = 9

        /** Ordered roughly by how often they come up as "mindless scrolling". */
        val SUGGESTED = listOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", // TikTok
            "com.google.android.youtube",
            "com.facebook.katana",
            "com.twitter.android",
            "com.instagram.barcelona", // Threads
            "com.snapchat.android",
            "com.reddit.frontpage",
            "com.pinterest",
            "tv.twitch.android.app",
            "com.netflix.mediaclient",
            "com.linkedin.android",
            "com.tinder", "com.bumble.app",
            "com.ninegag.android.app",
        )

        /** Pre-selected on first start: social media only (where installed). Video, dating and the rest are one tap away. */
        val SOCIAL = setOf(
            "com.instagram.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.facebook.katana",
            "com.twitter.android", "com.instagram.barcelona", "com.snapchat.android", "com.reddit.frontpage",
            "com.pinterest", "com.bereal.ft", "com.tumblr",
        )
    }
}
