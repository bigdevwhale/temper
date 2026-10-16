package app.temper.domain

import app.temper.data.Kind
import app.temper.data.Program

enum class StepType { BREATHE, HEAT, COLD, REST, WARM }

/**
 * One stretch of a session.
 * [open] steps keep counting past [seconds] until the user taps "out" (cold water: you leave when you leave).
 * [away] steps happen without the phone (the sauna): it says when to come out and waits for the tap.
 */
data class Step(val type: StepType, val seconds: Int, val round: Int = 1, val open: Boolean = false, val away: Boolean = false)

/** A contrast or sauna program: [rounds] of heat, then cold, then rest. */
data class Plan(
    val key: String,
    val kind: Kind,
    val heatSec: Int,
    val coldSec: Int,
    val restSec: Int,
    val rounds: Int,
    /** The last round ends on cold, with no rest after it. */
    val endCold: Boolean = true,
    /** Set for programs the user made. */
    val name: String? = null,
) {
    val custom: Boolean get() = name != null
    val totalSec: Int get() = Plans.steps(this).sumOf { it.seconds }
}

object Plans {
    val contrast = listOf(
        Plan("gentle", Kind.CONTRAST, 60, 15, 0, 3),
        Plan("classic", Kind.CONTRAST, 90, 30, 0, 3),
        Plan("awaken", Kind.CONTRAST, 60, 30, 0, 5),
        Plan("recovery", Kind.CONTRAST, 180, 60, 0, 4),
        Plan("steel", Kind.CONTRAST, 60, 60, 0, 5),
    )

    val sauna = listOf(
        Plan("sauna_gentle", Kind.SAUNA, 8 * 60, 60, 10 * 60, 2, endCold = false),
        Plan("finnish", Kind.SAUNA, 12 * 60, 2 * 60, 10 * 60, 3, endCold = false),
        Plan("banya", Kind.SAUNA, 10 * 60, 60, 12 * 60, 3, endCold = false),
        Plan("soberg", Kind.SAUNA, 15 * 60, 2 * 60, 5 * 60, 3, endCold = true),
        Plan("hammam", Kind.SAUNA, 15 * 60, 2 * 60, 10 * 60, 3, endCold = false),
    )

    fun of(p: Program): Plan = Plan(
        "p${p.id}", Kind.valueOf(p.kind), p.heatSec, p.coldSec, p.restSec, p.rounds, p.endCold, p.name.ifBlank { "·" },
    )

    /** Built-in or custom; falls back to the first of its kind. */
    fun byKey(key: String, kind: Kind, programs: List<Program>): Plan {
        val builtIn = if (kind == Kind.SAUNA) sauna else contrast
        return builtIn.firstOrNull { it.key == key }
            ?: programs.firstOrNull { "p${it.id}" == key && it.kind == kind.name }?.let(::of)
            ?: builtIn.first { it.key == if (kind == Kind.SAUNA) "finnish" else "classic" }
    }

    fun steps(p: Plan): List<Step> = buildList {
        for (r in 1..p.rounds) {
            val last = r == p.rounds
            when (p.kind) {
                Kind.CONTRAST -> {
                    add(Step(StepType.HEAT, p.heatSec, r))
                    if (p.coldSec > 0 && (!last || p.endCold || p.rounds == 1)) add(Step(StepType.COLD, p.coldSec, r))
                }
                Kind.SAUNA -> {
                    add(Step(StepType.HEAT, p.heatSec, r, away = true))
                    if (p.coldSec > 0) add(Step(StepType.COLD, p.coldSec, r))
                    if (p.restSec > 0 && !(last && p.endCold)) add(Step(StepType.REST, p.restSec, r))
                }
                Kind.COLD -> add(Step(StepType.COLD, p.coldSec, r, open = true))
            }
        }
    }

    /** Cold water on its own: calm breathing on land, the water, then warming up without help. */
    fun cold(targetSec: Int, breathe: Boolean, shiver: Boolean): List<Step> = buildList {
        if (breathe) add(Step(StepType.BREATHE, BREATHE_SEC))
        add(Step(StepType.COLD, targetSec, open = true))
        if (shiver) add(Step(StepType.WARM, WARM_SEC))
    }

    const val BREATHE_SEC = 60
    const val WARM_SEC = 5 * 60

    /** Seconds in the water to stay well within, by water temperature. A rough, cautious guide, not a promise of safety. */
    fun coldLimit(waterC: Double): Int = when {
        waterC <= 2 -> 90
        waterC <= 5 -> 3 * 60
        waterC <= 10 -> 5 * 60
        waterC <= 15 -> 10 * 60
        else -> 15 * 60
    }

    /** Choices for the cold target. */
    val coldTargets = listOf(15, 30, 45, 60, 90, 120, 180, 240, 300, 420, 600)

    /** The three stretches of cold water, by seconds in: shock, settling, calm. */
    fun coldPhase(sec: Int): Int = when {
        sec < 30 -> 0
        sec < 90 -> 1
        else -> 2
    }
}
