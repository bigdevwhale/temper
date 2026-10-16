package app.temper.run

import android.content.Context
import app.temper.R
import app.temper.data.Kind
import app.temper.domain.Step
import app.temper.domain.StepType

/** Step names and durations in words, shared by the session screen, the notification and the voice. */
object Words {
    fun step(context: Context, step: Step, kind: Kind): String = context.getString(
        when (step.type) {
            StepType.BREATHE -> R.string.step_breathe
            StepType.HEAT -> if (kind == Kind.SAUNA) R.string.step_sauna else R.string.step_hot
            StepType.COLD -> when (kind) {
                Kind.SAUNA -> R.string.step_cool
                Kind.COLD -> R.string.step_water
                Kind.CONTRAST -> R.string.step_cold
            }
            StepType.REST -> R.string.step_rest
            StepType.WARM -> R.string.step_warm
        }
    )

    /** "45 seconds", "2 minutes", "1 minute 30 seconds". */
    fun duration(context: Context, sec: Int): String {
        val r = context.resources
        val m = sec / 60
        val s = sec % 60
        return when {
            m == 0 -> r.getQuantityString(R.plurals.seconds, s, s)
            s == 0 -> r.getQuantityString(R.plurals.minutes, m, m)
            else -> r.getQuantityString(R.plurals.minutes, m, m) + " " + r.getQuantityString(R.plurals.seconds, s, s)
        }
    }

    /** "0:45", "12:00", "1:02:30". */
    fun clock(sec: Int): String {
        val s = sec.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val ss = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, ss) else "%d:%02d".format(m, ss)
    }
}
