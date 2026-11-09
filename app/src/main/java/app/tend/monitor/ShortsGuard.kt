package app.tend.monitor

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import app.tend.core.Mood
import app.tend.data.TendStore
import app.tend.ui.CharacterPainter

/**
 * Optional (Plus): Reels and Shorts closed, the rest of Instagram, YouTube and Facebook open.
 * When the short-video player shows (known by its view ids, never by what's on it), back it goes,
 * and the companion says so in a small bubble for a moment.
 */
class ShortsGuard : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var lastBlock = 0L
    private var bubble: LinearLayout? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        val ids = PLAYERS[pkg] ?: return
        val now = System.currentTimeMillis()
        if (now - lastBlock < 1_500) return
        val store = TendStore(this)
        if (!store.shortsGuardOn || !store.isPlus) return
        val root = rootInActiveWindow ?: return
        val showing = ids.any { id -> runCatching { root.findAccessibilityNodeInfosByViewId("$pkg:id/$id").any { it.isVisibleToUser } }.getOrDefault(false) }
        if (!showing) return
        lastBlock = now
        performGlobalAction(GLOBAL_ACTION_BACK)
        store.countShortsClosed()
        app.tend.data.Stats.log(this, "shorts_closed", mapOf("app" to APP_NAMES[pkg].orEmpty()))
        say(store, if (pkg == "com.google.android.youtube") "No Shorts today. The rest of YouTube is all yours." else "No Reels today. Your messages are still here.")
    }

    override fun onInterrupt() {}

    /** The companion's face and a line, by the top of the screen, gone after a few seconds. */
    private fun say(store: TendStore, line: String) {
        if (bubble != null) return
        val wm = getSystemService(WindowManager::class.java)
        fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply { setColor(0xFFFFFFFF.toInt()); cornerRadius = dp(26f).toFloat() }
            elevation = dp(8f).toFloat()
            setPadding(dp(8f), dp(6f), dp(16f), dp(6f))
            addView(ImageView(context).apply {
                setImageBitmap(CharacterPainter.bitmap(store.avatar, Mood.HAPPY, dp(40f), store.accessories().first))
                layoutParams = LinearLayout.LayoutParams(dp(40f), dp(40f))
            })
            addView(TextView(context).apply {
                text = line; textSize = 14f; setTextColor(0xFF1D2A23.toInt()); setPadding(dp(8f), 0, 0, 0)
            })
            alpha = 0f
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = dp(64f) }
        runCatching { wm.addView(view, params) }.onFailure { return }
        bubble = view
        view.translationY = -dp(20f).toFloat()
        view.animate().alpha(1f).translationY(0f).setDuration(260).start()
        handler.postDelayed({
            view.animate().alpha(0f).translationY(-dp(16f).toFloat()).setDuration(220).withEndAction {
                runCatching { wm.removeView(view) }; bubble = null
            }.start()
        }, 3_200)
    }

    companion object {
        /** The short-video players' view ids, per app (the feeds, chats and search aren't these). */
        private val PLAYERS = mapOf(
            "com.instagram.android" to listOf("clips_viewer_view_pager", "clips_viewer_container", "clips_video_container"),
            "com.google.android.youtube" to listOf("reel_recycler", "reel_player_page_container", "reel_watch_player"),
            "com.facebook.katana" to listOf("reels_viewer_container", "video_home_reels_container"),
        )
        private val APP_NAMES = mapOf("com.instagram.android" to "instagram", "com.google.android.youtube" to "youtube", "com.facebook.katana" to "facebook")

        /** Whether the user has switched the service on in Android's accessibility settings. */
        fun enabled(c: Context): Boolean = c.getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == c.packageName && it.resolveInfo.serviceInfo.name == ShortsGuard::class.java.name }
    }
}
