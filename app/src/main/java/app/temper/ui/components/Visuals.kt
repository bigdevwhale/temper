package app.temper.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import app.temper.data.Session
import app.temper.domain.Glow
import app.temper.ui.theme.U
import app.temper.ui.theme.temperColor
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** One folded layer of the blade: a session. */
@Immutable
data class Layer(val cold: Boolean, val weight: Float, val seed: Int)

/** The newest 36 sessions as layers; older ones are folded deep into the steel already. */
fun layersOf(sessions: List<Session>): List<Layer> = sessions.sortedBy { it.start }.takeLast(36).map {
    Layer(
        cold = it.coldSec >= it.heatSec / 6,
        weight = (it.coldSec / 180f + it.heatSec / 1800f).coerceIn(0.3f, 1f),
        seed = (it.start xor it.id).hashCode(),
    )
}

/**
 * The user's blade: Damascus layers from their sessions, a hamon whose waves grow with the streak,
 * the spine tinted with the temper colour of their level, and a glow that fades when the forge goes cold.
 * [highlight] lights up the newest layer (0..1), for the finish screen.
 */
@Composable
fun Blade(
    layers: List<Layer>,
    level: Int,
    glow: Glow,
    streak: Int,
    modifier: Modifier = Modifier,
    highlight: Float = 0f,
    animate: Boolean = true,
) {
    val dark = U.c.dark
    val t by if (animate) rememberInfiniteTransition(label = "glow").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse), label = "pulse",
    ) else remember { androidx.compose.runtime.mutableFloatStateOf(0.5f) }
    val temper = temperColor(level)
    val ember = Color(0xFFFF7A3D)
    Canvas(modifier) { drawBlade(layers, temper, glow, streak, highlight, t, dark, ember) }
}

private fun bladePath(w: Float, h: Float): Path = Path().apply {
    moveTo(w * 0.17f, h * 0.22f)
    lineTo(w * 0.80f, h * 0.22f)
    quadraticTo(w * 0.94f, h * 0.24f, w * 0.995f, h * 0.56f)
    quadraticTo(w * 0.80f, h * 0.86f, w * 0.48f, h * 0.86f)
    lineTo(w * 0.17f, h * 0.86f)
    close()
}

private fun edgePath(w: Float, h: Float): Path = Path().apply {
    moveTo(w * 0.17f, h * 0.86f)
    lineTo(w * 0.48f, h * 0.86f)
    quadraticTo(w * 0.80f, h * 0.86f, w * 0.995f, h * 0.56f)
}

private fun DrawScope.drawBlade(layers: List<Layer>, temper: Color, glow: Glow, streak: Int, highlight: Float, t: Float, dark: Boolean, ember: Color) {
    val w = size.width
    val h = size.height
    val blade = bladePath(w, h)
    val edge = edgePath(w, h)
    val heat = when (glow) { Glow.HOT -> 1f; Glow.WARM -> 0.5f; Glow.COOLING -> 0.18f; Glow.COLD -> 0f }

    // Heat haze behind the steel.
    if (heat > 0f) {
        for (i in 4 downTo 1) {
            drawPath(edge, ember.copy(alpha = heat * (0.05f + 0.03f * t) * (5 - i) / 4f), style = Stroke(h * 0.07f * i, cap = StrokeCap.Round))
        }
    }

    // Handle: wrapped grip, a pommel and a guard.
    val gripTop = h * 0.36f
    val gripBottom = h * 0.74f
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFF3B2A20), Color(0xFF241812)), gripTop, gripBottom),
        Offset(w * 0.015f, gripTop), Size(w * 0.14f, gripBottom - gripTop),
        androidx.compose.ui.geometry.CornerRadius(h * 0.06f),
    )
    for (i in 0 until 6) {
        val x = w * (0.03f + i * 0.022f)
        drawLine(Color(0xFF5A4232), Offset(x, gripTop), Offset(x + w * 0.012f, gripBottom), strokeWidth = h * 0.025f)
    }
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFFB9B4AC), Color(0xFF6B655E))),
        Offset(w * 0.152f, h * 0.14f), Size(w * 0.03f, h * 0.8f),
        androidx.compose.ui.geometry.CornerRadius(w * 0.01f),
    )

    clipPath(blade) {
        // Base steel: bright along the spine, darker toward the edge.
        val steelTop = if (dark) Color(0xFFC9CED3) else Color(0xFFB9C0C6)
        val steelBottom = if (dark) Color(0xFF7E868D) else Color(0xFF8C949B)
        drawRect(Brush.verticalGradient(listOf(steelTop, steelBottom), h * 0.2f, h * 0.86f))

        // Damascus: one wavy band per session, cold ones dark, hot ones bright.
        val n = (layers.size + 4).coerceAtLeast(10)
        val top = h * 0.22f
        val span = h * 0.64f
        val all = List(n - layers.size) { Layer(it % 2 == 0, 0.5f, it * 7919) } + layers
        all.forEachIndexed { k, layer ->
            val rnd = Random(layer.seed)
            val y0 = top + span * (k + 0.5f) / n
            val amp = span / n * (1.2f + rnd.nextFloat() * 1.6f) * if (layer.cold) 1.4f else 1f
            val f1 = 1.5f + rnd.nextFloat() * 1.5f
            val f2 = 4f + rnd.nextFloat() * 3f
            val p1 = rnd.nextFloat() * 6.28f
            val p2 = rnd.nextFloat() * 6.28f
            val path = Path()
            val steps = 60
            for (i in 0..steps) {
                val x = w * (0.16f + 0.86f * i / steps)
                val u = i / steps.toFloat()
                // Waves grow toward the tip, where a real billet is drawn out most.
                val y = y0 + amp * (0.5f + u) * (sin(u * f1 * PI.toFloat() + p1 + k * 0.35f) * 0.8f + sin(u * f2 * PI.toFloat() + p2) * 0.25f)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val newest = k == all.lastIndex && highlight > 0f
            val color = when {
                newest -> lerp(if (layer.cold) Color(0xFF8FD3FF) else Color(0xFFFFC27A), Color.White, 0.3f * t).copy(alpha = 0.55f + 0.45f * highlight)
                layer.cold -> Color(0xFF2E3A46).copy(alpha = 0.5f)
                else -> Color(0xFFF3F5F7).copy(alpha = 0.45f)
            }
            drawPath(path, color, style = Stroke(span / n * (0.55f + 0.6f * layer.weight) * if (newest) 1.6f else 1f, cap = StrokeCap.Round))
        }

        // Temper colour washes in from the spine.
        drawRect(Brush.verticalGradient(listOf(temper.copy(alpha = 0.62f), temper.copy(alpha = 0.18f), Color.Transparent), h * 0.2f, h * 0.6f))

        // Hamon: the hardened edge, its waves deeper with a longer streak.
        val hamonAmp = h * (0.012f + 0.006f * streak.coerceAtMost(12))
        val hamon = Path()
        val hy = h * 0.70f
        for (i in 0..80) {
            val u = i / 80f
            val x = w * (0.16f + 0.86f * u)
            val y = hy + u * h * 0.0f + hamonAmp * sin(u * 26f + 1.3f) * (0.6f + 0.4f * sin(u * 7f))
            if (i == 0) hamon.moveTo(x, y) else hamon.lineTo(x, y)
        }
        val frost = Path().apply { addPath(hamon); lineTo(w, h); lineTo(0f, h); close() }
        drawPath(frost, Color.White.copy(alpha = 0.26f))
        drawPath(hamon, Color.White.copy(alpha = 0.55f), style = Stroke(h * 0.012f))

        // Heat colours from the tip while the forge is hot.
        if (heat > 0f) {
            drawRect(
                Brush.horizontalGradient(listOf(Color.Transparent, ember.copy(alpha = 0.10f * heat), ember.copy(alpha = (0.45f + 0.15f * t) * heat), Color(0xFFFFD27A).copy(alpha = 0.5f * heat)), w * 0.4f, w),
            )
        }
        // A cold blade gathers frost.
        if (glow == Glow.COLD || glow == Glow.COOLING) {
            val rnd = Random(3)
            val a = if (glow == Glow.COLD) 0.7f else 0.3f
            repeat(70) {
                drawCircle(Color.White.copy(alpha = a * rnd.nextFloat()), h * 0.006f + rnd.nextFloat() * h * 0.008f, Offset(w * (0.17f + rnd.nextFloat() * 0.8f), h * (0.22f + rnd.nextFloat() * 0.64f)))
            }
        }
        // Spine highlight.
        drawLine(Color.White.copy(alpha = 0.7f), Offset(w * 0.17f, h * 0.225f), Offset(w * 0.8f, h * 0.225f), strokeWidth = h * 0.012f)
    }
    drawPath(edge, Color.White.copy(alpha = 0.8f), style = Stroke(h * 0.012f))
    drawPath(blade, Color.Black.copy(alpha = 0.35f), style = Stroke(h * 0.008f))
}

/** Embers rising, for a moment worth marking. */
@Composable
fun Sparks(modifier: Modifier = Modifier, key: Any = Unit) {
    val pieces = remember(key) {
        val rnd = Random(key.hashCode())
        List(70) { Spark(rnd.nextFloat(), 0.7f + rnd.nextFloat() * 0.4f, 0.25f + rnd.nextFloat() * 0.5f, rnd.nextFloat() * 6.28f, 1.5f + rnd.nextFloat() * 3f) }
    }
    var elapsed by remember(key) { mutableLongStateOf(0L) }
    LaunchedEffect(key) {
        val start = withFrameMillis { it }
        while (elapsed < 3200) elapsed = withFrameMillis { it } - start
    }
    if (elapsed >= 3200) return
    Canvas(modifier) {
        val t = elapsed / 1000f
        pieces.forEach { p ->
            val life = (t / (1.6f + p.speed * 2)).coerceIn(0f, 1f)
            if (life >= 1f) return@forEach
            val y = (p.y - p.speed * t * 0.55f) * size.height
            val x = (p.x + 0.03f * sin(t * 3 + p.phase)) * size.width
            val c = lerp(Color(0xFFFFE08A), Color(0xFFFF5A1F), life)
            drawCircle(c.copy(alpha = 1f - life), p.r * (1f - life * 0.5f) * density, Offset(x, y))
        }
    }
}

private class Spark(val x: Float, val y: Float, val speed: Float, val phase: Float, val r: Float)

/** Values 1..5 over time as a soft line, lower is better. */
@Composable
fun ScoreLine(values: List<Int>, color: Color, modifier: Modifier = Modifier) {
    val line = U.c.line
    Canvas(modifier) {
        for (v in 1..5) {
            val y = size.height * (1 - (v - 1) / 4f)
            drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        if (values.size < 2) return@Canvas
        // Moving average of three, so one hard morning doesn't spike the line.
        val smooth = values.indices.map { i -> values.subList((i - 1).coerceAtLeast(0), (i + 2).coerceAtMost(values.size)).average().toFloat() }
        val path = Path()
        smooth.forEachIndexed { i, v ->
            val x = size.width * i / (smooth.size - 1)
            val y = size.height * (1 - (v - 1) / 4f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(3f * density, cap = StrokeCap.Round))
        val lx = size.width
        val ly = size.height * (1 - (smooth.last() - 1) / 4f)
        drawCircle(color, 5f * density, Offset(lx - 5f * density, ly))
    }
}
