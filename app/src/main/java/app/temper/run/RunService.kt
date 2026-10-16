package app.temper.run

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.temper.MainActivity
import app.temper.R
import app.temper.container
import app.temper.data.Kind
import app.temper.data.Settings
import app.temper.domain.Plans
import app.temper.domain.StepType
import app.temper.localized
import app.temper.notify.Alarms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Keeps a session going with the screen off and the phone out of reach:
 * it advances timed steps, speaks and rings the cues, and shows the step in a notification.
 */
class RunService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wake: PowerManager.WakeLock? = null
    private var voice: Voice? = null
    private lateinit var words: Context
    private var settings = Settings()

    /** Cues already given for the step at [cuedIndex], so each rings once. */
    private var cuedIndex = -1
    private val cued = HashSet<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        words = localized()
        Alarms.ensureChannels(this)
        startInForeground(build(null))
        wake = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "temper:session").apply {
            setReferenceCounted(false)
            acquire(4 * 3_600_000L)
        }
        voice = Voice(this, words.resources.configuration.locales[0])
        scope.launch {
            settings = container.settings.settings.first()
            loop()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_NEXT -> Engine.next(this)
            ACTION_PAUSE -> Engine.run.value?.let { if (it.paused) Engine.resume() else Engine.pause() }
        }
        if (Engine.run.value == null) stopSelf()
        return START_NOT_STICKY
    }

    private fun startInForeground(n: Notification) {
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, Alarms.ID_RUN, n, type)
    }

    private suspend fun loop() {
        var shown: Triple<Int, Boolean, Int>? = null
        while (true) {
            val r = Engine.run.value ?: break
            val now = SystemClock.elapsedRealtime()
            if (r.index != cuedIndex) {
                cuedIndex = r.index
                cued.clear()
                announce(r)
            }
            if (!r.paused) tick(r, now)
            val key = Triple(r.index, r.paused, (r.elapsed(now) / 60))
            if (key != shown) {
                shown = key
                getSystemService(android.app.NotificationManager::class.java).notify(Alarms.ID_RUN, build(Engine.run.value))
            }
            delay(200)
        }
        stopSelf()
    }

    private fun once(key: String, block: () -> Unit) {
        if (cued.add(key)) block()
    }

    private fun announce(r: Run) {
        val s = r.step
        val kind = r.setup.kind
        val lastRound = s.round == r.rounds && r.rounds > 1 && (r.index == 0 || r.setup.steps[r.index - 1].round != s.round)
        val prefix = if (lastRound && s.type == StepType.HEAT) w(R.string.v_last_round) + " " else ""
        when (s.type) {
            StepType.BREATHE -> { tone(Sound.Cue.BREATH_IN); say(w(R.string.v_breathe)) }
            StepType.HEAT -> {
                tone(Sound.Cue.ANVIL); buzz(long = false)
                if (s.away) {
                    val outAt = System.currentTimeMillis() + s.seconds * 1000L
                    say(prefix + words.getString(R.string.v_sauna_in, clockTime(outAt)))
                } else say(prefix + w(R.string.v_hot))
            }
            StepType.COLD -> {
                tone(Sound.Cue.HISS); buzz(long = true)
                say(
                    when (kind) {
                        Kind.COLD -> w(R.string.v_water)
                        Kind.SAUNA -> words.getString(R.string.v_cool, Words.duration(words, s.seconds))
                        Kind.CONTRAST -> w(R.string.v_cold)
                    }
                )
            }
            StepType.REST -> { tone(Sound.Cue.BELL); say(words.getString(R.string.v_rest, Words.duration(words, s.seconds))) }
            StepType.WARM -> { tone(Sound.Cue.BELL); say(w(R.string.v_warm)) }
        }
    }

    private fun tick(r: Run, now: Long) {
        val s = r.step
        val sec = r.elapsed(now)
        val left = r.remainingMs(now)
        when {
            s.away -> {
                // The phone is outside the sauna: ring loudly and keep ringing every minute until "I'm out".
                if (left <= 0) {
                    val n = ((-left) / 60_000).toInt()
                    if (n < 6) once("out$n") { tone(Sound.Cue.BELL); buzz(long = true); say(w(R.string.v_sauna_out)) }
                }
            }
            s.open -> {
                if (s.type == StepType.COLD) {
                    if (sec >= 30) once("p1") { say(w(R.string.v_phase1)) }
                    if (sec >= 90 && s.seconds > 90) once("p2") { say(w(R.string.v_phase2)) }
                    if (left <= 0) once("target") { tone(Sound.Cue.BELL); buzz(long = false); say(w(R.string.v_target)) }
                    val limit = r.setup.waterC?.let { Plans.coldLimit(it) }
                    if (limit != null && sec >= limit) {
                        val n = (sec - limit) / 30
                        if (n < 6) once("limit$n") { tone(Sound.Cue.BEEP_HIGH); buzz(long = true); say(w(R.string.v_limit)) }
                    }
                }
            }
            else -> {
                if (s.type == StepType.BREATHE) {
                    // 4 seconds in, 6 out.
                    val cycle = (r.elapsedMs(now) % 10_000)
                    val n = r.elapsedMs(now) / 10_000
                    if (n > 0 && cycle < 300) once("in$n") { tone(Sound.Cue.BREATH_IN) }
                    if (cycle in 4_000..4_300) once("out$n") { tone(Sound.Cue.BREATH_OUT) }
                }
                if (s.type == StepType.REST && s.seconds >= 240 && sec >= s.seconds / 2) once("drink") { say(w(R.string.v_drink)) }
                if (s.seconds >= 10) for (k in 1..3) if (left in 1..(k * 1000L)) once("c$k") { tone(Sound.Cue.BEEP) }
                if (left <= 0) {
                    if (r.nextStep == null) { tone(Sound.Cue.BELL); say(w(R.string.v_done)) }
                    Engine.next(this, s.seconds)
                }
            }
        }
    }

    private fun w(id: Int) = words.getString(id)
    private fun say(text: String) { if (settings.voice) voice?.say(text) }
    private fun tone(cue: Sound.Cue) { if (settings.tones) Sound.play(cue) }

    private fun buzz(long: Boolean) {
        if (!settings.vibrate) return
        val v = if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)
        val pattern = if (long) longArrayOf(0, 400, 150, 400, 150, 400) else longArrayOf(0, 250, 120, 250)
        runCatching { v.vibrate(VibrationEffect.createWaveform(pattern, -1)) }
    }

    private fun clockTime(ms: Long): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(words.resources.configuration.locales[0])
            .format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

    private fun build(r: Run?): Notification {
        val open = PendingIntent.getActivity(
            this, 300,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Alarms.EXTRA_ROUTE, "run"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val b = NotificationCompat.Builder(this, Alarms.CHANNEL_RUN)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(this, R.color.accent))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
        if (r == null) return b.setContentTitle(w(R.string.app_name)).build()
        val s = r.step
        val now = SystemClock.elapsedRealtime()
        val wall = System.currentTimeMillis()
        val title = Words.step(words, s, r.setup.kind) + if (r.rounds > 1 && s.round > 0) " · " + words.getString(R.string.round_of, s.round, r.rounds) else ""
        val countUp = s.open || s.away
        val text = when {
            r.paused -> w(R.string.paused)
            s.away -> words.getString(R.string.notif_out_at, clockTime(wall + r.remainingMs(now)))
            r.nextStep != null -> words.getString(R.string.next_step, Words.step(words, r.nextStep!!, r.setup.kind), Words.clock(r.nextStep!!.seconds))
            else -> w(R.string.last_step)
        }
        b.setContentTitle(title).setContentText(text)
        if (!r.paused) {
            b.setShowWhen(true).setUsesChronometer(true)
            if (countUp) b.setWhen(wall - r.elapsedMs(now))
            else b.setWhen(wall + r.remainingMs(now)).setChronometerCountDown(true)
        }
        val nextLabel = if (countUp) w(if (s.away) R.string.im_out else R.string.out_of_water) else w(R.string.skip)
        b.addAction(0, nextLabel, PendingIntent.getService(this, 301, Intent(this, RunService::class.java).setAction(ACTION_NEXT), PendingIntent.FLAG_IMMUTABLE))
        if (!s.away) b.addAction(0, w(if (r.paused) R.string.resume else R.string.pause), PendingIntent.getService(this, 302, Intent(this, RunService::class.java).setAction(ACTION_PAUSE), PendingIntent.FLAG_IMMUTABLE))
        return b.build()
    }

    override fun onDestroy() {
        scope.cancel()
        voice?.shutdown()
        runCatching { wake?.release() }
        super.onDestroy()
    }

    companion object {
        const val ACTION_NEXT = "app.temper.run.NEXT"
        const val ACTION_PAUSE = "app.temper.run.PAUSE"
    }
}
