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

/** Home screen widget: face, minutes left, next task. Refreshed by [TendStore] on every change. */
class TendWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = render(context, manager, ids)

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TendWidget::class.java))
            if (ids.isNotEmpty()) render(context, manager, ids)
        }

        private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val store = TendStore(context)
            val cfg = store.config
            val day = store.today()
            val remaining = Budget.remainingMinutes(cfg, day)
            val next = Tasks.next(day)
            val views = RemoteViews(context.packageName, R.layout.widget).apply {
                setImageViewBitmap(R.id.face, CharacterPainter.bitmap(store.avatar, Moods.of(cfg, day), 240))
                setTextViewText(R.id.minutes, if (remaining > 0) "$remaining min" else "0 min")
                setTextViewText(R.id.next, next?.let { "Next: ${it.title.lowercase()} · +${it.rewardMinutes}" } ?: "All done today")
                setOnClickPendingIntent(
                    R.id.root,
                    PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE),
                )
            }
            manager.updateAppWidget(ids, views)
        }
    }
}
