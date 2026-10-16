package app.temper.ui.session

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.temper.R
import app.temper.container
import app.temper.data.Kind
import app.temper.data.Settings
import app.temper.domain.Plans
import app.temper.domain.StepType
import app.temper.run.Engine
import app.temper.run.Run
import app.temper.run.Words
import app.temper.ui.components.PrimaryButton
import app.temper.ui.components.SoftButton
import app.temper.ui.kindName
import app.temper.ui.stepColor
import app.temper.ui.stepName
import app.temper.ui.timeOf
import app.temper.ui.theme.U
import app.temper.ui.theme.UType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun RunScreen(run: Run, settings: Settings, onLeave: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) { now = SystemClock.elapsedRealtime(); delay(100) }
    }
    var askStop by remember { mutableStateOf(false) }
    var askOut by remember { mutableStateOf(false) }
    BackHandler { onLeave() }

    val step = run.step
    val kind = run.setup.kind
    val color by animateColorAsState(stepColor(step.type), tween(900), label = "step")
    val elapsedMs = run.elapsedMs(now)
    val elapsed = (elapsedMs / 1000).toInt()
    val countUp = step.open || step.away
    val over = countUp && elapsed >= step.seconds
    val fraction = if (step.seconds == 0) 1f else (elapsedMs / (step.seconds * 1000f)).coerceIn(0f, 1f)

    fun out() {
        // Tapped long after the bell: probably came out on time and forgot the phone was waiting.
        if (step.away && elapsed - step.seconds > 120) askOut = true else Engine.next(context)
    }

    Box(
        Modifier.fillMaxSize().background(U.c.bg).background(
            Brush.radialGradient(listOf(color.copy(alpha = if (U.c.dark) 0.30f else 0.20f), Color.Transparent), center = Offset.Unspecified, radius = 1400f),
        ),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { askStop = true }) { Icon(Icons.Rounded.Close, stringResource(R.string.stop), tint = U.c.ink) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(kindName(kind).uppercase(), style = UType.Eyebrow)
                    if (run.rounds > 1) Text(stringResource(R.string.round_of, step.round, run.rounds), style = UType.Muted)
                }
                IconButton(onClick = { c.scope.launch { c.settings.update { it.copy(voice = !it.voice) } } }) {
                    Icon(if (settings.voice) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff, stringResource(R.string.voice), tint = U.c.muted)
                }
            }

            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(stepName(step, kind), style = UType.Display.copy(color = color, textAlign = TextAlign.Center))
                Spacer(Modifier.height(22.dp))
                Box(Modifier.size(270.dp), contentAlignment = Alignment.Center) {
                    if (step.type == StepType.BREATHE) BreathOrb(elapsedMs, color)
                    Dial(fraction, color, pulse = over, elapsedMs = elapsedMs)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val shown = if (countUp) elapsed else ((run.remainingMs(now) + 999) / 1000).toInt()
                        Text(Words.clock(shown), style = UType.Timer)
                        val sub = when {
                            run.paused -> stringResource(R.string.paused)
                            step.type == StepType.BREATHE -> stringResource(if (elapsedMs % 10_000 < 4_000) R.string.breathe_in else R.string.breathe_out)
                            countUp -> stringResource(if (over) R.string.target_done else R.string.target_of, Words.clock(step.seconds))
                            else -> stringResource(R.string.left)
                        }
                        Text(sub, style = UType.Muted)
                    }
                }
                Spacer(Modifier.height(22.dp))
                Hint(run, elapsed, now)
            }

            Steps(run, now)
            Spacer(Modifier.height(16.dp))
            when {
                step.away -> PrimaryButton(stringResource(R.string.im_out), ::out, Modifier.fillMaxWidth().height(64.dp), color = color, icon = Icons.Rounded.DirectionsWalk)
                step.open -> PrimaryButton(stringResource(R.string.out_of_water), ::out, Modifier.fillMaxWidth().height(64.dp), color = color, icon = Icons.Rounded.DirectionsWalk)
                else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(
                        stringResource(if (run.paused) R.string.resume else R.string.pause),
                        { if (run.paused) Engine.resume() else Engine.pause() },
                        Modifier.weight(1f).height(60.dp), color = color, icon = if (run.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    )
                    SoftButton(stringResource(R.string.skip), { Engine.next(context) }, Modifier.height(60.dp), color = color, soft = color.copy(alpha = 0.14f), icon = Icons.Rounded.SkipNext)
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }

    if (askStop) {
        AlertDialog(
            onDismissRequest = { askStop = false },
            containerColor = U.c.bg,
            title = { Text(stringResource(R.string.stop_title), style = UType.Strong.copy(fontSize = 19.sp)) },
            text = { Text(stringResource(R.string.stop_text), style = UType.Sub) },
            confirmButton = {
                TextButton(onClick = { askStop = false; Engine.finish(context) }) { Text(stringResource(R.string.stop_save), color = U.c.accent, style = UType.Button) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { askStop = false; Engine.discard(context); onLeave() }) { Text(stringResource(R.string.stop_discard), color = U.c.danger, style = UType.Button) }
                    TextButton(onClick = { askStop = false }) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) }
                }
            },
        )
    }
    if (askOut) {
        val planned = System.currentTimeMillis() - (elapsed - step.seconds) * 1000L
        AlertDialog(
            onDismissRequest = { askOut = false },
            containerColor = U.c.bg,
            title = { Text(stringResource(R.string.out_when), style = UType.Strong.copy(fontSize = 19.sp)) },
            text = { Text(stringResource(R.string.out_when_text), style = UType.Sub) },
            confirmButton = {
                TextButton(onClick = { askOut = false; Engine.next(context, step.seconds) }) {
                    Text(stringResource(R.string.out_planned, timeOf(planned)), color = U.c.accent, style = UType.Button)
                }
            },
            dismissButton = {
                TextButton(onClick = { askOut = false; Engine.next(context) }) {
                    Text(stringResource(R.string.out_now, timeOf(System.currentTimeMillis())), color = U.c.muted, style = UType.Button)
                }
            },
        )
    }
}

@Composable
private fun Hint(run: Run, elapsed: Int, now: Long) {
    val step = run.step
    val text = when (step.type) {
        StepType.HEAT -> if (step.away) stringResource(R.string.hint_sauna, timeOf(System.currentTimeMillis() + run.remainingMs(now)))
        else stringResource(R.string.hint_hot)
        StepType.COLD -> when {
            run.setup.kind != Kind.COLD -> stringResource(if (run.setup.kind == Kind.SAUNA) R.string.hint_cool else R.string.hint_cold)
            run.setup.waterC != null && elapsed >= Plans.coldLimit(run.setup.waterC) -> stringResource(R.string.hint_limit)
            else -> stringResource(
                when (Plans.coldPhase(elapsed)) {
                    0 -> R.string.hint_phase0
                    1 -> R.string.hint_phase1
                    else -> R.string.hint_phase2
                }
            )
        }
        StepType.BREATHE -> stringResource(R.string.hint_breathe)
        StepType.REST -> stringResource(R.string.hint_rest)
        StepType.WARM -> stringResource(R.string.hint_warm)
    }
    if (step.away) {
        Text(timeOf(System.currentTimeMillis() + run.remainingMs(now)), style = UType.Display.copy(color = U.c.accent, fontSize = 40.sp))
        Spacer(Modifier.height(6.dp))
    }
    val danger = step.type == StepType.COLD && run.setup.kind == Kind.COLD && run.setup.waterC != null && elapsed >= Plans.coldLimit(run.setup.waterC)
    Text(text, style = UType.Quote.copy(textAlign = TextAlign.Center, color = if (danger) U.c.danger else U.c.ink), modifier = Modifier.padding(horizontal = 8.dp))
    run.nextStep?.let { n ->
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.next_step, stepName(n, run.setup.kind), Words.clock(n.seconds)), style = UType.Muted)
    }
}

@Composable
private fun Dial(fraction: Float, color: Color, pulse: Boolean, elapsedMs: Long) {
    val track = U.c.line
    Canvas(Modifier.fillMaxSize()) {
        val w = 14.dp.toPx()
        val inset = w / 2
        val arc = Size(size.width - w, size.height - w)
        drawArc(track, -90f, 360f, false, Offset(inset, inset), arc, style = Stroke(w))
        val a = if (pulse) 0.55f + 0.45f * sin(elapsedMs / 300.0 * PI / 2).toFloat().let { it * it } else 1f
        drawArc(color.copy(alpha = a), -90f, 360f * fraction.coerceAtLeast(0.004f), false, Offset(inset, inset), arc, style = Stroke(w, cap = StrokeCap.Round))
    }
}

/** Swells for four seconds in, shrinks for six out. */
@Composable
private fun BreathOrb(elapsedMs: Long, color: Color) {
    val t = elapsedMs % 10_000
    val f = if (t < 4_000) t / 4_000f else 1f - (t - 4_000) / 6_000f
    val eased = (1 - kotlin.math.cos(f * PI).toFloat()) / 2
    Box(
        Modifier.size((150 + 90 * eased).dp).clip(CircleShape).background(color.copy(alpha = 0.10f + 0.12f * eased)),
    )
}

/** The whole session as a strip of segments: the forge's rhythm at a glance. */
@Composable
private fun Steps(run: Run, now: Long) {
    val colors = run.setup.steps.map { stepColor(it.type) }
    val line = U.c.line
    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
        val total = run.setup.steps.sumOf { it.seconds.coerceAtLeast(20) }.toFloat()
        var x = 0f
        val gap = 3.dp.toPx()
        run.setup.steps.forEachIndexed { i, s ->
            val w = size.width * s.seconds.coerceAtLeast(20) / total - gap
            val col = colors[i]
            val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
            drawRoundRect(if (i < run.index) col.copy(alpha = 0.85f) else line, Offset(x, 0f), Size(w.coerceAtLeast(2f), size.height), r)
            if (i == run.index) {
                val f = if (s.seconds == 0) 1f else (run.elapsedMs(now) / (s.seconds * 1000f)).coerceIn(0f, 1f)
                drawRoundRect(col, Offset(x, 0f), Size((w * f).coerceAtLeast(size.height), size.height), r)
            }
            x += w + gap
        }
    }
}
