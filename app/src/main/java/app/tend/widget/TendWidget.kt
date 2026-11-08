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

    fun refresh(context: Context, force: Boolean = false) {
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
        val shown = listOf(store.avatar, mood, remaining, next?.id, day.completedTasks.sorted(), placed, night, paper, ducks, fresh?.id, store.bridgeBuilt, visitor).joinToString("|")
        if (!force && shown == lastShown) return
        lastShown = shown

        val worn = store.accessories().first
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
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
            setImageViewBitmap(R.id.face, CharacterPainter.bitmap(store.avatar, mood, 192, worn))
            setContentDescription(R.id.face, Moods.status(store.avatar.displayName, mood))
            setOnClickPendingIntent(R.id.root, open)
        })
        if (garden.isNotEmpty()) manager.updateAppWidget(garden, RemoteViews(context.packageName, R.layout.widget_garden).apply {
            setImageViewBitmap(R.id.garden, clearingBitmap(store.avatar, placed, worn, 440, 440, night = night, paper = paper, ducks = ducks, bridge = store.bridgeBuilt, visitor = visitor,
                layout = store.layout.mapValues { androidx.compose.ui.geometry.Offset(it.value.first, it.value.second) }))
            val label = fresh?.let { "${it.name}: ${it.kind.short}" } ?: visitor?.let { "A ${it.title.lowercase()} is here" }
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
