package app.tend.ui

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class InstalledApp(val pkg: String, val label: String)

/** Launchable apps, split into well-known time sinks (suggested) and everything else. */
class AppCatalog(private val context: Context) {
    val all: List<InstalledApp> = context.packageManager
        .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(context.packageManager).toString()) }
        .filter { it.pkg != context.packageName }
        .distinctBy { it.pkg }
        .sortedBy { it.label.lowercase() }

    val suggested: List<InstalledApp> = all.filter { it.pkg in SUGGESTED }
        .sortedBy { SUGGESTED.indexOf(it.pkg) }

    val others: List<InstalledApp> = all - suggested.toSet()

    fun label(pkg: String): String = all.firstOrNull { it.pkg == pkg }?.label ?: pkg

    private val icons = HashMap<String, ImageBitmap?>()

    fun icon(pkg: String): ImageBitmap? = icons.getOrPut(pkg) {
        runCatching { context.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() }.getOrNull()
    }

    companion object {
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
    }
}
