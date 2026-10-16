package app.temper.run

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * The forge's sounds, synthesised on the fly so the app ships no audio files:
 * an anvil strike for heat, the hiss of quenched steel for cold, short beeps for a countdown
 * and a bell when it's time to come out.
 */
object Sound {
    private const val RATE = 44_100

    enum class Cue { ANVIL, HISS, BEEP, BEEP_HIGH, BELL, BREATH_IN, BREATH_OUT }

    private val cache = HashMap<Cue, ShortArray>()

    private fun pcm(cue: Cue): ShortArray = cache.getOrPut(cue) {
        when (cue) {
            Cue.ANVIL -> tone(1.1, listOf(880.0 to 1.0, 2428.8 to 0.55, 4752.0 to 0.3, 1320.0 to 0.25), decay = 5.5)
            Cue.HISS -> hiss(1.6)
            Cue.BEEP -> tone(0.14, listOf(1046.5 to 1.0), decay = 4.0)
            Cue.BEEP_HIGH -> tone(0.32, listOf(1568.0 to 1.0, 3136.0 to 0.2), decay = 3.0)
            Cue.BELL -> tone(2.2, listOf(660.0 to 1.0, 1320.0 to 0.5, 1980.0 to 0.3, 1640.0 to 0.2), decay = 2.2)
            Cue.BREATH_IN -> glide(0.5, 392.0, 523.3)
            Cue.BREATH_OUT -> glide(0.7, 523.3, 349.2)
        }
    }

    /** Inharmonic partials with an exponential decay: metal struck. */
    private fun tone(seconds: Double, partials: List<Pair<Double, Double>>, decay: Double): ShortArray {
        val n = (seconds * RATE).toInt()
        val sum = partials.sumOf { it.second }
        return ShortArray(n) { i ->
            val t = i.toDouble() / RATE
            val attack = (t / 0.004).coerceAtMost(1.0)
            var v = 0.0
            for ((f, a) in partials) v += a * sin(2 * PI * f * t) * exp(-t * decay * (1 + f / 4000))
            (v / sum * attack * 0.8 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** Filtered noise that flares and fades, with a few crackles: steel in water. */
    private fun hiss(seconds: Double): ShortArray {
        val n = (seconds * RATE).toInt()
        val rnd = Random(7)
        var prev = 0.0
        var low = 0.0
        return ShortArray(n) { i ->
            val t = i.toDouble() / RATE
            val white = rnd.nextDouble(-1.0, 1.0)
            // A crude high-pass leaves the sizzle, the low-pass softens its edge.
            val high = white - prev
            prev = white
            low += (high - low) * 0.55
            val env = (t / 0.05).coerceAtMost(1.0) * exp(-t * 2.4)
            val crackle = if (rnd.nextDouble() < 0.0008 * exp(-t * 2)) rnd.nextDouble(-1.0, 1.0) * 2 else 0.0
            ((low * 0.6 + crackle) * env * 0.7 * Short.MAX_VALUE).coerceIn(-32767.0, 32767.0).toInt().toShort()
        }
    }

    private fun glide(seconds: Double, from: Double, to: Double): ShortArray {
        val n = (seconds * RATE).toInt()
        var phase = 0.0
        return ShortArray(n) { i ->
            val t = i.toDouble() / RATE
            val f = from + (to - from) * (t / seconds)
            phase += 2 * PI * f / RATE
            val env = sin(PI * t / seconds)
            (sin(phase) * env * 0.35 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    fun play(cue: Cue) {
        val data = pcm(cue)
        runCatching {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(data.size * 2)
                .build()
            track.write(data, 0, data.size)
            track.setNotificationMarkerPosition(data.size - 1)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack) { t.release() }
                override fun onPeriodicNotification(t: AudioTrack) {}
            })
            track.play()
        }
    }
}
