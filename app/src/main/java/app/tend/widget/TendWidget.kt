package app.tend.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.tend.MainActivity
import app.tend.R
import app.tend.core.Budget
import app.tend.core.Moods
import app.tend.core.Tasks
import app.tend.data.TendStore
import app.tend.ui.CharacterPainter
import app.tend.ui.clearingBitmap

/** 2x1: face, minutes left (or what earns them back), and how the companion is doing. */
class TendWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = Widgets.refresh(context, force = true)
}

/** 1x1: just the companion, in today's mood and with what it has earned to wear. */
class TendWidgetFace : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = Widgets.refresh(context, force = true)
}

/** 2x2: the camp with the companion, and the minutes left. (Class name kept: placed widgets depend on it.) */
class TendWidgetGarden : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = Widgets.refresh(context, force = true)
}

object Widgets {
    /** What the widgets showed last; the store changes every second, the widgets only when this does. */
    @Volatile private var lastShown: String? = null

    private val main = android.os.Handler(android.os.Looper.getMainLooper())

    /**
     * Redraws the widgets when what they show has changed. Called on every store write, from any
     * thread: the drawing itself always happens on the main thread (the painters share their
     * brushes with the app's own screens), and a widget that can't be drawn never takes the app down.
     */
    fun refresh(context: Context, force: Boolean = false) {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            val app = context.applicationContext
            main.post { refresh(app, force) }
            return
        }
        runCatching { draw(context, force) }.onFailure { android.util.Log.w("tend", "Widgets not redrawn", it) }
    }

    private fun draw(context: Context, force: Boolean) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = { cls: Class<*> -> manager.getAppWidgetIds(ComponentName(context, cls)) }
        val wide = ids(TendWidget::class.java)
        val face = ids(TendWidgetFace::class.java)
        val garden = ids(TendWidgetGarden::class.java)
        if (wide.isEmpty() && face.isEmpty() && garden.isEmpty()) return

        val store = TendStore(context)
        val cfg = store.config
        val day = store.today()
        val remaining = Budget.remainingMinutes(cfg, day)
        val night = Budget.isEvening(store.eveningFrom, java.time.LocalTime.now().hour)
        val mood = Moods.atNight(Moods.of(cfg, day), night)
        val next = Tasks.next(day, store.selectedTasks)
        val placed = store.placed
        val paper = app.tend.data.Friends.paperOnMe(context).isNotEmpty()
        val ducks = app.tend.data.Friends.ducks(context).size.coerceAtMost(3)
        val fresh = app.tend.data.Friends.unseen(context)
        val visitor = store.visitorWaiting()
        // Evening (from the evening hour until 5:00): the camp at night, a dark card and a sleepy companion.
        // Cheap things only before the "has anything changed?" check: this runs on every store write.
        val layout = store.layout
        val guests = app.tend.data.Friends.atFire(context).map { (runCatching { app.tend.core.Avatar.valueOf(it.companion) }.getOrDefault(app.tend.core.Avatar.BO)) to it.name }
        val shown = listOf(store.avatar, mood, remaining, next?.id, day.completedTasks.sorted(), placed, night, paper, ducks, fresh?.id, store.bridgeBuilt, visitor, store.outfitWorn, layout, guests, app.tend.core.LiveWeather.fresh(), java.time.LocalTime.now().hour).joinToString("|")
        if (!force && shown == lastShown) return
        lastShown = shown
        val worn = store.accessories().first

        // Something from a friend: a tap goes straight to the camp, where it is (the paper, the duck, the wave).
        val open = if (fresh != null) PendingIntent.getActivity(
            context, 1,
            Intent(context, MainActivity::class.java).putExtra(app.tend.reminder.RecapReminder.EXTRA_OPEN, app.tend.reminder.WeeklyLetter.OPEN_CLEARING),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        ) else PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val minutes = "${remaining.coerceAtLeast(0)}"
        val available = (cfg.freeMinutes + day.earnedMinutes).coerceAtLeast(1)

        if (wide.isNotEmpty()) manager.updateAppWidget(wide, RemoteViews(context.packageName, R.layout.widget).apply {
            setImageViewBitmap(R.id.face, CharacterPainter.bitmap(store.avatar, mood, 160, worn))
            setTextViewText(R.id.minutes, minutes)
            setProgressBar(R.id.progress, 100, (remaining * 100 / available).coerceIn(0, 100), false)
            // Out of minutes: point to what earns them back instead.
            setTextViewText(
                R.id.status,
                if (fresh != null) fresh.kind.received(fresh.name) + "!"
                else if (remaining > 0) Moods.status(store.avatar.displayName, mood)
                else next?.let { "${it.title} +${it.rewardMinutes}" } ?: "Fresh start tomorrow",
            )
            setOnClickPendingIntent(R.id.root, open)
            setInt(R.id.root, "setBackgroundResource", if (night) R.drawable.widget_bg_night else R.drawable.widget_bg)
            setTextColor(R.id.minutes, if (night) NIGHT_TEXT else DAY_TEXT)
            setTextColor(R.id.status, if (night) NIGHT_TEXT else DAY_TEXT)
            setTextColor(R.id.caption, if (night) NIGHT_MUTED else DAY_MUTED)
        })
        if (face.isNotEmpty()) manager.updateAppWidget(face, RemoteViews(context.packageName, R.layout.widget_face).apply {
            // A friend did something: the companion lights up, and a badge says what.
            setImageViewBitmap(R.id.face, CharacterPainter.bitmap(store.avatar, if (fresh != null) app.tend.core.Mood.HAPPY else mood, 192, worn))
            setContentDescription(R.id.face, fresh?.let { it.kind.received(it.name) } ?: Moods.status(store.avatar.displayName, mood))
            setViewVisibility(R.id.badge, if (fresh != null) android.view.View.VISIBLE else android.view.View.GONE)
            fresh?.let { setImageViewBitmap(R.id.badge, app.tend.ui.noteBadge(96, it.kind == app.tend.data.Friends.Kind.VISIT, it.kind.emoji)) }
            setOnClickPendingIntent(R.id.root, open)
        })
        if (garden.isNotEmpty()) manager.updateAppWidget(garden, RemoteViews(context.packageName, R.layout.widget_garden).apply {
            setImageViewBitmap(R.id.garden, clearingBitmap(store.avatar, placed, worn, 440, 440, night = night, paper = paper, ducks = ducks, bridge = store.bridgeBuilt, visitor = visitor,
                layout = layout.mapValues { androidx.compose.ui.geometry.Offset(it.value.first, it.value.second) }, guests = guests))
            val label = fresh?.let { "${it.name}: ${it.kind.short}" } ?: guests.firstOrNull()?.let { "${it.second} at your fire" } ?: visitor?.let { "${it.aName.replaceFirstChar { c -> c.uppercase() }} is here" }
            setViewVisibility(R.id.note, if (label != null) android.view.View.VISIBLE else android.view.View.GONE)
            setTextViewText(R.id.note, label ?: "")
            setInt(R.id.note, "setBackgroundResource", if (night) R.drawable.widget_pill_night else R.drawable.widget_pill)
            setTextColor(R.id.note, if (night) NIGHT_TEXT else DAY_TEXT)
            setTextViewText(R.id.minutes, minutes)
            setOnClickPendingIntent(R.id.root, open)
            setInt(R.id.root, "setBackgroundResource", if (night) R.drawable.widget_bg_night else R.drawable.widget_bg)
            setInt(R.id.pill, "setBackgroundResource", if (night) R.drawable.widget_pill_night else R.drawable.widget_pill)
            setTextColor(R.id.minutes, if (night) NIGHT_TEXT else DAY_TEXT)
            setTextColor(R.id.pill_label, if (night) NIGHT_MUTED else DAY_MUTED)
        })
    }

    private const val DAY_TEXT = 0xFF316849.toInt()
    private const val DAY_MUTED = 0xFF6B756F.toInt()
    private const val NIGHT_TEXT = 0xFF8FD3AE.toInt()
    private const val NIGHT_MUTED = 0xFFA3B0A8.toInt()
}
