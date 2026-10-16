package app.temper.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.temper.MainActivity
import app.temper.R
import app.temper.container
import app.temper.content.Content
import app.temper.localized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** The session notification's channel and the one daily nudge. */
object Alarms {
    const val CHANNEL_RUN = "session_v1"
    private const val CHANNEL_REMIND = "remind_v1"
    const val EXTRA_ROUTE = "route"
    const val ID_RUN = 1
    private const val ID_REMIND = 2
    private const val ACTION_REMIND = "app.temper.notify.REMIND"

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val l = context.localized()
        if (nm.getNotificationChannel(CHANNEL_RUN) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_RUN, l.getString(R.string.channel_run), NotificationManager.IMPORTANCE_LOW).apply {
                    description = l.getString(R.string.channel_run_desc)
                    setShowBadge(false)
                }
            )
        }
        if (nm.getNotificationChannel(CHANNEL_REMIND) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_REMIND, l.getString(R.string.channel_remind), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = l.getString(R.string.channel_remind_desc)
                }
            )
        }
    }

    private fun pi(context: Context) = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, AlarmReceiver::class.java).setAction(ACTION_REMIND),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Next time the clock shows [minutes] after midnight, strictly after now. */
    fun nextAt(minutes: Int, now: LocalDateTime): LocalDateTime {
        val m = Math.floorMod(minutes, 24 * 60)
        val today = now.toLocalDate().atTime(m / 60, m % 60)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    suspend fun reschedule(context: Context) {
        val s = context.container.settings.now()
        val am = context.getSystemService(AlarmManager::class.java)
        val p = pi(context)
        if (s.remindAt < 0 || !s.onboarded) {
            am.cancel(p)
            return
        }
        val zone = ZoneId.systemDefault()
        val at = nextAt(s.remindAt, LocalDateTime.now(zone)).atZone(zone).toInstant().toEpochMilli()
        // A nudge doesn't need to-the-second precision; this avoids the exact-alarm permission.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
    }

    suspend fun fire(context: Context) {
        val c = context.container
        val s = c.settings.now()
        if (s.remindAt < 0 || !canNotify(context)) return
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        // Quiet on days with a session already.
        if (c.db.dao().sessions().any { Instant.ofEpochMilli(it.start).atZone(zone).toLocalDate() == today }) return
        val l = context.localized()
        val content = Content.of(l.resources.configuration.locales[0])
        val n = NotificationCompat.Builder(context, CHANNEL_REMIND)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(l.getString(R.string.notif_remind_title))
            .setContentText(content.tip(today.toEpochDay()))
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.tip(today.toEpochDay())))
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 100,
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID_REMIND, n)
        } catch (_: SecurityException) {
        }
    }

    internal const val REMIND = ACTION_REMIND
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Alarms.REMIND) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Alarms.fire(context)
                context.container.changed()
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                context.container.changed()
            } finally {
                pending.finish()
            }
        }
    }
}
