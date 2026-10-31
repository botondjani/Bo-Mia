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
import app.tend.core.Garden
import app.tend.core.Moods
import app.tend.core.Tasks
import app.tend.data.TendStore
import app.tend.ui.CharacterPainter
import app.tend.ui.gardenBitmap

/** 2x1: face, minutes left (or what earns them back), and how the companion is doing. */
class TendWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = Widgets.refresh(context, force = true)
}

/** 1x1: just the companion, in today's mood and with what it has earned to wear. */
class TendWidgetFace : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = Widgets.refresh(context, force = true)
}

/** 2x2: the garden with the companion, and the minutes left. */
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
        val mood = Moods.of(cfg, day)
        val next = Tasks.next(day, store.selectedTasks)
        val plants = store.plants
        val shown = listOf(store.avatar, mood, remaining, next?.id, day.completedTasks.sorted(), plants.size, store.daysTended).joinToString("|")
        if (!force && shown == lastShown) return
        lastShown = shown

        val worn = store.accessories().first
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val minutes = if (remaining > 0) "$remaining min" else "0 min"

        if (wide.isNotEmpty()) manager.updateAppWidget(wide, RemoteViews(context.packageName, R.layout.widget).apply {
            setImageViewBitmap(R.id.face, CharacterPainter.bitmap(store.avatar, mood, 176, worn))
            setTextViewText(R.id.status, Moods.status(store.avatar.displayName, mood))
            setTextViewText(R.id.minutes, minutes)
            // Out of minutes: point to what earns them back instead.
            setTextViewText(
                R.id.caption,
                if (remaining > 0) context.getString(R.string.widget_left_today)
                else next?.let { "${it.title.lowercase()} +${it.rewardMinutes}" } ?: "fresh start tomorrow",
            )
            setOnClickPendingIntent(R.id.root, open)
        })
        if (face.isNotEmpty()) manager.updateAppWidget(face, RemoteViews(context.packageName, R.layout.widget_face).apply {
            setImageViewBitmap(R.id.face, CharacterPainter.bitmap(store.avatar, mood, 192, worn))
            setContentDescription(R.id.face, Moods.status(store.avatar.displayName, mood))
            setOnClickPendingIntent(R.id.root, open)
        })
        if (garden.isNotEmpty()) manager.updateAppWidget(garden, RemoteViews(context.packageName, R.layout.widget_garden).apply {
            setImageViewBitmap(R.id.garden, gardenBitmap(store.avatar, plants, Garden.homeStage(store.daysTended), worn, 440, 440))
            setTextViewText(R.id.minutes, "$minutes left")
            setOnClickPendingIntent(R.id.root, open)
        })
    }
}
