package app.tend.monitor

import app.tend.core.tr

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import app.tend.core.Accessory
import app.tend.core.Avatar
import app.tend.core.Mood
import app.tend.ui.CharacterPainter

/**
 * The companion peeking in from the edge of the screen after a long stretch in a paused app: a
 * small face and the minutes, then gone again. Tapped, it opens into a little card: come along, or
 * a bit more. Kind, never in the way: no focus taken, gone by itself after a few seconds.
 */
class Peek(private val context: Context) {
    private val wm = context.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var root: FrameLayout? = null
    private var hideLater: Runnable? = null

    val showing: Boolean get() = root != null

    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, context.resources.displayMetrics)

    /** [minutes] in [label] so far: peek in, and go again after a few seconds unless tapped. */
    fun show(avatar: Avatar, worn: Set<Accessory>, minutes: Int, label: String, onCome: () -> Unit, onMore: () -> Unit) {
        if (root != null) return
        val mood = if (minutes >= 20) Mood.SLEEPY else Mood.HAPPY
        val face = ImageView(context).apply {
            setImageBitmap(CharacterPainter.bitmap(avatar, mood, dp(56f).toInt(), worn))
            layoutParams = LinearLayout.LayoutParams(dp(56f).toInt(), dp(56f).toInt())
        }
        val line = TextView(context).apply {
            text = tr("$minutes min", "$minutes perc")
            setTextColor(INK)
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dp(4f).toInt(), 0, dp(14f).toInt(), 0)
        }
        val bubble = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply { setColor(CARD); cornerRadii = floatArrayOf(dp(32f), dp(32f), 0f, 0f, 0f, 0f, dp(32f), dp(32f)) }
            elevation = dp(8f)
            setPadding(dp(6f).toInt(), dp(4f).toInt(), 0, dp(4f).toInt())
            addView(face)
            addView(line)
            contentDescription = tr("${avatar.displayName}: $minutes minutes in $label. Tap for a break.", "${avatar.displayName}: $minutes perce megy ez: $label. Koppints egy szünetért.")
        }
        val frame = FrameLayout(context).apply {
            setPadding(dp(8f).toInt(), dp(8f).toInt(), 0, dp(8f).toInt())
            clipToPadding = false
            addView(bubble)
        }
        bubble.setOnClickListener { open(frame, avatar, minutes, label, onCome, onMore) }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            y = (context.resources.displayMetrics.heightPixels * 0.28f).toInt()
        }
        runCatching { wm.addView(frame, params) }.onFailure { return }
        root = frame
        // In from the edge with a little bounce, and a nod once there.
        bubble.translationX = dp(140f)
        bubble.animate().translationX(0f).setDuration(520).setInterpolator(OvershootInterpolator(1.4f)).withEndAction {
            face.animate().rotation(-12f).setDuration(160).withEndAction { face.animate().rotation(0f).setDuration(220).start() }.start()
        }.start()
        hideLater = Runnable { hide() }.also { handler.postDelayed(it, SHOW_MS) }
    }

    /** Tapped: the little card, with the two ways on. */
    private fun open(frame: FrameLayout, avatar: Avatar, minutes: Int, label: String, onCome: () -> Unit, onMore: () -> Unit) {
        hideLater?.let { handler.removeCallbacks(it) }
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { setColor(CARD); cornerRadius = dp(22f) }
            elevation = dp(10f)
            setPadding(dp(18f).toInt(), dp(16f).toInt(), dp(18f).toInt(), dp(14f).toInt())
            layoutParams = FrameLayout.LayoutParams(dp(260f).toInt(), FrameLayout.LayoutParams.WRAP_CONTENT)
        }
        card.addView(TextView(context).apply {
            text = when {
                minutes >= 30 -> tr("$minutes minutes of $label. I could use a walk. You?", "$minutes perce megy ez: $label. Rám férne egy séta. Rád is?")
                minutes >= 20 -> tr("$minutes minutes already. I'm getting sleepy here.", "Már $minutes perc. Kezdek elálmosodni.")
                else -> tr("Psst, $minutes minutes of $label. Still the plan?", "Pszt, $minutes perce megy ez: $label. Még mindig ez a terv?")
            }
            setTextColor(INK); textSize = 15f
        })
        fun button(text: String, filled: Boolean, onTap: () -> Unit) = TextView(context).apply {
            this.text = text
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(if (filled) 0xFFFFFFFF.toInt() else MOSS)
            background = GradientDrawable().apply { setColor(if (filled) MOSS else SAGE); cornerRadius = dp(14f) }
            setPadding(0, dp(10f).toInt(), 0, dp(10f).toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { if (!filled) marginStart = dp(8f).toInt() }
            setOnClickListener { hide(); onTap() }
        }
        card.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12f).toInt(), 0, 0)
            addView(button(tr("Come with ${avatar.displayName}", "Gyere, ${avatar.displayName} vár"), true, onCome))
            addView(button(tr("A bit more", "Még egy kicsit"), false, onMore))
        })
        frame.removeAllViews()
        frame.setPadding(0, dp(8f).toInt(), dp(12f).toInt(), dp(8f).toInt())
        frame.addView(card)
        card.alpha = 0f
        card.scaleX = 0.85f; card.scaleY = 0.85f
        card.pivotX = dp(260f); card.pivotY = 0f
        card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(OvershootInterpolator(1.1f)).start()
        hideLater = Runnable { hide() }.also { handler.postDelayed(it, OPEN_MS) }
    }

    /** Out to the edge again, then gone. */
    fun hide() {
        val frame = root ?: return
        root = null
        hideLater?.let { handler.removeCallbacks(it) }
        val child = frame.getChildAt(0)
        val remove = { runCatching { wm.removeView(frame) }; Unit }
        if (child == null || !frame.isAttachedToWindow) { remove(); return }
        child.animate().translationX(dp(300f)).alpha(0.4f).setDuration(260).setInterpolator(AccelerateInterpolator()).withEndAction(remove).start()
    }

    companion object {
        private const val SHOW_MS = 7_000L
        private const val OPEN_MS = 15_000L
        private const val INK = 0xFF1D2A23.toInt()
        private const val MOSS = 0xFF316849.toInt()
        private const val SAGE = 0xFFE8EFE4.toInt()
        private const val CARD = 0xFFFFFFFF.toInt()

        /** Peeks come at 10, 20, 30... minutes in a row in paused apps. */
        const val EVERY_MS = 10 * 60_000L
    }
}
