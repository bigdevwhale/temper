package app.temper.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import app.temper.MainActivity
import app.temper.R
import app.temper.container
import app.temper.domain.Glow
import app.temper.domain.Stats
import app.temper.localized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.ZoneId

/** The week's cold and heat against the goal, and how warm the blade is. A tap opens the forge. */
class ForgeWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                refresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        suspend fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ForgeWidget::class.java))
            if (ids.isEmpty()) return
            val c = context.container
            val s = c.settings.now()
            val st = Stats(c.db.dao().sessions(), System.currentTimeMillis(), ZoneId.systemDefault(), s.coldGoal, s.heatGoal)
            val l = context.localized()
            val views = RemoteViews(context.packageName, R.layout.widget_forge)
            views.setTextViewText(
                R.id.label,
                l.getString(
                    when (st.glow) {
                        Glow.HOT -> R.string.glow_hot
                        Glow.WARM -> R.string.glow_warm
                        Glow.COOLING -> R.string.glow_cooling
                        Glow.COLD -> R.string.glow_cold
                    }
                ),
            )
            fun row(text: Int, bar: Int, line: Int, sec: Int, goal: Int, name: Int) {
                val show = goal > 0
                views.setViewVisibility(text, if (show) View.VISIBLE else View.GONE)
                views.setViewVisibility(bar, if (show) View.VISIBLE else View.GONE)
                if (!show) return
                views.setTextViewText(text, l.getString(line, l.getString(name), sec / 60, goal))
                views.setProgressBar(bar, 1000, (sec * 1000 / (goal * 60)).coerceIn(0, 1000), false)
            }
            row(R.id.cold, R.id.cold_bar, R.string.widget_line, st.weekColdSec, s.coldGoal, R.string.norm_cold)
            row(R.id.heat, R.id.heat_bar, R.string.widget_line, st.weekHeatSec, s.heatGoal, R.string.norm_heat)
            views.setOnClickPendingIntent(
                R.id.root,
                PendingIntent.getActivity(
                    context, 5000,
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            manager.updateAppWidget(ids, views)
        }
    }
}
