package app.temper.domain

import app.temper.data.Kind
import app.temper.data.Session
import app.temper.data.Settings
import app.temper.data.Style
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** How warm the blade is: it glows after a session and cools over the days without one. */
enum class Glow { HOT, WARM, COOLING, COLD }

/** Everything counted from the sessions. */
class Stats(val sessions: List<Session>, val now: Long, val zone: ZoneId, coldGoal: Int, heatGoal: Int) {
    val today: LocalDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    fun day(s: Session): LocalDate = Instant.ofEpochMilli(s.start).atZone(zone).toLocalDate()

    val count = sessions.size
    fun count(kind: Kind) = sessions.count { it.kind == kind }
    val coldSec: Long = sessions.sumOf { it.coldSec.toLong() }
    val heatSec: Long = sessions.sumOf { it.heatSec.toLong() }
    val rounds: Int = sessions.filter { it.kind != Kind.COLD }.sumOf { it.rounds }
    val longestCold: Int = sessions.filter { it.kind == Kind.COLD }.maxOfOrNull { it.coldSec } ?: 0
    val coldestWater: Double? = sessions.filter { it.coldSec > 0 }.mapNotNull { it.waterC }.minOrNull()

    val weekStart: LocalDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private fun inWeek(s: Session, monday: LocalDate) = day(s).let { !it.isBefore(monday) && it.isBefore(monday.plusDays(7)) }
    fun week(monday: LocalDate): List<Session> = sessions.filter { inWeek(it, monday) }
    val thisWeek = week(weekStart)
    val weekColdSec = thisWeek.sumOf { it.coldSec }
    val weekHeatSec = thisWeek.sumOf { it.heatSec }

    /** Every goal that isn't zero is filled. */
    private val goals = coldGoal to heatGoal
    fun weekFull(list: List<Session>): Boolean {
        val (c, h) = goals
        if (c <= 0 && h <= 0) return false
        return (c <= 0 || list.sumOf { it.coldSec } >= c * 60) && (h <= 0 || list.sumOf { it.heatSec } >= h * 60)
    }

    /** Full weeks in a row, counting this week once it is full, otherwise ending last week. */
    val weekStreak: Int = run {
        var monday = if (weekFull(thisWeek)) weekStart else weekStart.minusWeeks(1)
        var n = 0
        while (weekFull(week(monday))) { n++; monday = monday.minusWeeks(1) }
        n
    }
    val fullWeeks: Int = sessions.map { day(it).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }.distinct().count { weekFull(week(it)) }

    val days: Set<LocalDate> = sessions.map(::day).toSet()
    fun on(date: LocalDate) = sessions.filter { day(it) == date }

    /** Days of practice in a chain; one rest day between them doesn't break it. */
    val streak: Int = chain(today).let { if (it == 0) chain(today.minusDays(1)).let { y -> if (y == 0) chain(today.minusDays(2)) else y } else it }
    val bestStreak: Int = days.maxOfOrNull { chain(it) } ?: 0

    private fun chain(from: LocalDate): Int {
        if (from !in days) return 0
        var n = 0
        var d = from
        while (true) {
            if (d in days) { n++; d = d.minusDays(1); continue }
            if (d.minusDays(1) in days) { d = d.minusDays(1); continue }
            break
        }
        return n
    }

    val last: Session? = sessions.maxByOrNull { it.end }
    val glow: Glow = last?.let {
        val h = (now - it.end) / 3_600_000.0
        when {
            h < 36 -> Glow.HOT
            h < 4 * 24 -> Glow.WARM
            h < 8 * 24 -> Glow.COOLING
            else -> Glow.COLD
        }
    } ?: Glow.COLD

    /** How hard it was to begin, in order. */
    val fears: List<Int> = sessions.mapNotNull { it.fear }
    val fearFirst: Double? = fears.take(5).takeIf { it.size >= 3 }?.average()
    val fearLast: Double? = fears.takeLast(5).takeIf { fears.size >= 8 }?.average()

    val xp: Int = sessions.sumOf { Levels.xp(it) }
}

/** Steel tempering colours, from raw iron to grey: the blade's level. */
object Levels {
    val keys = listOf("raw", "pale", "straw", "bronze", "purple", "blue", "sky", "grey")
    private val thresholds = listOf(0, 60, 180, 400, 750, 1250, 2000, 3000)

    fun xp(s: Session): Int = 5 + s.coldSec / 15 + s.heatSec / 60 + if (s.kind == Kind.CONTRAST) s.rounds * 2 else 0

    /** 1-based. */
    fun level(xp: Int): Int = thresholds.indexOfLast { xp >= it } + 1
    fun key(level: Int): String = keys[(level - 1).coerceIn(0, keys.size - 1)]
    fun floor(level: Int): Int = thresholds[(level - 1).coerceIn(0, thresholds.size - 1)]
    fun next(level: Int): Int? = thresholds.getOrNull(level)
    fun progress(xp: Int): Float {
        val l = level(xp)
        val n = next(l) ?: return 1f
        return ((xp - floor(l)).toFloat() / (n - floor(l))).coerceIn(0f, 1f)
    }
    val max = keys.size
}

/** A maker's mark stamped on the blade. */
data class Badge(val id: String)

object Badges {
    val all = listOf(
        "first_cold", "first_contrast", "first_sauna", "cold_1m", "cold_3m", "cold_5m",
        "water_10", "water_4", "open_water", "ice_hole", "dawn", "soberg",
        "sessions_10", "sessions_50", "sessions_100", "streak_7", "streak_30", "week_full",
        "weeks_4", "fear_down", "heat_10h", "rounds_50", "trinity", "trial",
    )

    fun unlocked(st: Stats, s: Settings): List<Badge> {
        val ss = st.sessions
        val got = buildSet {
            if (st.count(Kind.COLD) > 0) add("first_cold")
            if (st.count(Kind.CONTRAST) > 0) add("first_contrast")
            if (st.count(Kind.SAUNA) > 0) add("first_sauna")
            if (st.longestCold >= 60) add("cold_1m")
            if (st.longestCold >= 180) add("cold_3m")
            if (st.longestCold >= 300) add("cold_5m")
            st.coldestWater?.let { if (it <= 10) add("water_10"); if (it <= 4) add("water_4") }
            if (ss.any { it.style == Style.OPEN.name }) add("open_water")
            if (ss.any { it.style == Style.ICE.name }) add("ice_hole")
            if (ss.any { Instant.ofEpochMilli(it.start).atZone(st.zone).hour < 7 }) add("dawn")
            if (ss.any { it.kind == Kind.SAUNA && it.endedCold }) add("soberg")
            if (st.count >= 10) add("sessions_10")
            if (st.count >= 50) add("sessions_50")
            if (st.count >= 100) add("sessions_100")
            if (st.bestStreak >= 7) add("streak_7")
            if (st.bestStreak >= 30) add("streak_30")
            if (st.fullWeeks >= 1) add("week_full")
            if (st.weekStreak >= 4) add("weeks_4")
            val firstHard = st.fears.indexOfFirst { it >= 4 }
            if (firstHard >= 0 && st.fears.drop(firstHard + 1).any { it == 1 }) add("fear_down")
            if (st.heatSec >= 10 * 3600) add("heat_10h")
            if (st.rounds >= 50) add("rounds_50")
            val weeks = ss.groupBy { st.day(it).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
            if (weeks.values.any { w -> w.map { it.kind }.toSet().size == 3 }) add("trinity")
            if (s.trialsDone.isNotEmpty()) add("trial")
        }
        return all.filter { it in got }.map(::Badge)
    }
}

/** A trial: a goal counted in sessions since it began, never broken by a missed day. */
data class Trial(val id: String, val goal: Int)

data class TrialState(val trial: Trial, val done: Int, val start: LocalDate, val doneToday: Boolean) {
    val finished get() = done >= trial.goal
    /** For First Ice: the cold to hold next, in seconds. */
    val target: Int get() = Trials.iceTarget(done)
}

object Trials {
    val all = listOf(
        Trial("first_ice", 30),
        Trial("dawn", 21),
        Trial("contrast", 14),
        Trial("sauna", 8),
        Trial("winter", 10),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }

    /** 15 seconds on the first day up to 3 minutes on the thirtieth. */
    fun iceTarget(doneDays: Int): Int {
        val d = doneDays.coerceIn(0, 29)
        return (15 + (165.0 * d / 29)).let { (Math.round(it / 5) * 5).toInt() }
    }

    fun state(s: Settings, st: Stats): TrialState? {
        val t = byId(s.trial) ?: return null
        val start = LocalDate.ofEpochDay(s.trialStart)
        val since = st.sessions.filter { !st.day(it).isBefore(start) }.sortedBy { it.start }
        var done = 0
        var today = false
        when (t.id) {
            "first_ice" -> {
                // One day counts once, when its target was held.
                for ((date, list) in since.groupBy { st.day(it) }.toSortedMap()) {
                    if (list.any { it.coldSec >= iceTarget(done) && it.kind != Kind.SAUNA }) {
                        done++
                        if (date == st.today) today = true
                    }
                }
            }
            "dawn" -> since.filter { it.coldSec > 0 && Instant.ofEpochMilli(it.start).atZone(st.zone).hour < 9 }
                .map { st.day(it) }.distinct().let { done = it.size; today = st.today in it }
            "contrast" -> since.filter { it.kind == Kind.CONTRAST }.map { st.day(it) }.distinct().let { done = it.size; today = st.today in it }
            "sauna" -> since.filter { it.kind == Kind.SAUNA && it.endedCold }.let { done = it.size; today = it.any { x -> st.day(x) == st.today } }
            "winter" -> since.filter { it.style == Style.OPEN.name || it.style == Style.ICE.name }.let { done = it.size; today = it.any { x -> st.day(x) == st.today } }
        }
        return TrialState(t, done.coerceAtMost(t.goal), start, today)
    }
}
