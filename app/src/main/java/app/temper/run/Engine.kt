package app.temper.run

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import app.temper.container
import app.temper.data.Kind
import app.temper.data.Session
import app.temper.domain.Step
import app.temper.domain.StepType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the user chose before starting. */
data class Setup(
    val kind: Kind,
    val style: String,
    val program: String,
    val steps: List<Step>,
    val waterC: Double? = null,
    val airC: Double? = null,
    val fear: Int? = null,
    val spot: String = "",
)

/** A session in progress. Times are [SystemClock.elapsedRealtime] millis, immune to clock changes. */
data class Run(
    val setup: Setup,
    val startedWall: Long,
    val index: Int = 0,
    val stepStart: Long,
    val pausedAt: Long? = null,
    /** Real seconds of each finished step. */
    val done: List<Int> = emptyList(),
) {
    val step: Step get() = setup.steps[index]
    val nextStep: Step? get() = setup.steps.getOrNull(index + 1)
    val rounds: Int get() = setup.steps.maxOf { it.round }
    fun elapsedMs(now: Long = SystemClock.elapsedRealtime()): Long = ((pausedAt ?: now) - stepStart).coerceAtLeast(0)
    fun elapsed(now: Long = SystemClock.elapsedRealtime()): Int = (elapsedMs(now) / 1000).toInt()
    fun remainingMs(now: Long = SystemClock.elapsedRealtime()): Long = step.seconds * 1000L - elapsedMs(now)
    val paused: Boolean get() = pausedAt != null

    /** Seconds of [type] including the step in progress. */
    fun seconds(type: StepType, now: Long = SystemClock.elapsedRealtime()): Int =
        setup.steps.take(done.size).zip(done).filter { it.first.type == type }.sumOf { it.second } +
            if (step.type == type) elapsed(now) else 0
}

/** The one session that can run at a time; [RunService] keeps it alive with the screen off. */
object Engine {
    private val _run = MutableStateFlow<Run?>(null)
    val run: StateFlow<Run?> = _run.asStateFlow()

    /** Id of the session just saved, so the app opens its finish screen. */
    val finished = MutableStateFlow<Long?>(null)

    /** True between the end of a run and its session landing in the database. */
    val saving = MutableStateFlow(false)

    fun start(context: Context, setup: Setup) {
        if (setup.steps.isEmpty()) return
        _run.value = Run(setup, System.currentTimeMillis(), stepStart = SystemClock.elapsedRealtime())
        ContextCompat.startForegroundService(context, Intent(context, RunService::class.java))
    }

    /** Ends the current step after [seconds] (default: as timed) and moves on, or finishes after the last one. */
    fun next(context: Context, seconds: Int? = null) {
        val r = _run.value ?: return
        val now = SystemClock.elapsedRealtime()
        val took = seconds ?: r.elapsed(now)
        val done = r.done + took.coerceAtLeast(0)
        if (r.index + 1 >= r.setup.steps.size) {
            save(context, r.copy(done = done), now)
        } else {
            _run.value = r.copy(index = r.index + 1, stepStart = now, pausedAt = null, done = done)
        }
    }

    fun pause() {
        val r = _run.value ?: return
        if (r.pausedAt == null) _run.value = r.copy(pausedAt = SystemClock.elapsedRealtime())
    }

    fun resume() {
        val r = _run.value ?: return
        val p = r.pausedAt ?: return
        _run.value = r.copy(stepStart = r.stepStart + (SystemClock.elapsedRealtime() - p), pausedAt = null)
    }

    /** Stops now and keeps what was done, if anything was. */
    fun finish(context: Context) {
        val r = _run.value ?: return
        val now = SystemClock.elapsedRealtime()
        save(context, r.copy(done = r.done + r.elapsed(now)), now)
    }

    /** Stops without saving. */
    fun discard(context: Context) {
        _run.value = null
        context.stopService(Intent(context, RunService::class.java))
    }

    private fun save(context: Context, r: Run, @Suppress("UNUSED_PARAMETER") now: Long) {
        val steps = r.setup.steps.take(r.done.size).zip(r.done)
        val cold = steps.filter { it.first.type == StepType.COLD }.sumOf { it.second }
        val heat = steps.filter { it.first.type == StepType.HEAT }.sumOf { it.second }
        if (cold + heat >= 5) saving.value = true
        _run.value = null
        context.stopService(Intent(context, RunService::class.java))
        if (cold + heat < 5) return
        val worked = steps.filter { (it.first.type == StepType.COLD || it.first.type == StepType.HEAT) && it.second > 0 }
        val session = Session(
            style = r.setup.style,
            start = r.startedWall,
            end = maxOf(System.currentTimeMillis(), r.startedWall + (cold + heat) * 1000L),
            coldSec = cold,
            heatSec = heat,
            rounds = worked.maxOfOrNull { it.first.round } ?: 1,
            waterC = r.setup.waterC,
            airC = r.setup.airC,
            fear = r.setup.fear,
            program = r.setup.program,
            endedCold = worked.lastOrNull()?.first?.type == StepType.COLD,
            spot = r.setup.spot,
        )
        val c = context.container
        c.scope.launch {
            val id = c.db.dao().insertSession(session)
            finished.value = id
            saving.value = false
            c.changed()
        }
    }
}
