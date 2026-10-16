package app.temper

import app.temper.data.Kind
import app.temper.data.Session
import app.temper.data.Settings
import app.temper.data.Style
import app.temper.domain.Glow
import app.temper.domain.Levels
import app.temper.domain.Plans
import app.temper.domain.Stats
import app.temper.domain.StepType
import app.temper.domain.Trials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class TemperTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 16)
    private val now = today.atTime(12, 0).toInstant(zone).toEpochMilli()

    private fun cold(daysAgo: Int, sec: Int = 60, hour: Int = 8): Session {
        val start = today.minusDays(daysAgo.toLong()).atTime(hour, 0).toInstant(zone).toEpochMilli()
        return Session(style = Style.SHOWER.name, start = start, end = start + sec * 1000L, coldSec = sec, heatSec = 0)
    }

    @Test
    fun contrastEndsOnCold() {
        val steps = Plans.steps(Plans.contrast.first { it.key == "classic" })
        assertEquals(6, steps.size)
        assertEquals(StepType.COLD, steps.last().type)
    }

    @Test
    fun sobergSkipsLastRest() {
        val steps = Plans.steps(Plans.sauna.first { it.key == "soberg" })
        assertEquals(StepType.COLD, steps.last().type)
        assertTrue(steps.filter { it.type == StepType.HEAT }.all { it.away })
        val finnish = Plans.steps(Plans.sauna.first { it.key == "finnish" })
        assertEquals(StepType.REST, finnish.last().type)
    }

    @Test
    fun coldSessionHasOpenWaterStep() {
        val steps = Plans.cold(90, breathe = true, shiver = true)
        assertEquals(listOf(StepType.BREATHE, StepType.COLD, StepType.WARM), steps.map { it.type })
        assertTrue(steps[1].open)
    }

    @Test
    fun streakSurvivesOneRestDay() {
        val st = Stats(listOf(cold(0), cold(2), cold(3), cold(6)), now, zone, 11, 0)
        assertEquals(3, st.streak)
        assertEquals(Glow.HOT, st.glow)
    }

    @Test
    fun streakCountsFromTwoDaysAgo() {
        val st = Stats(listOf(cold(2), cold(3)), now, zone, 11, 0)
        assertEquals(2, st.streak)
    }

    @Test
    fun weekFillsAtGoal() {
        val st = Stats(List(11) { cold(0, 60, 6 + it) }, now, zone, 11, 0)
        assertEquals(1, st.weekStreak)
    }

    @Test
    fun firstIceGrowsTo3Minutes() {
        assertEquals(15, Trials.iceTarget(0))
        assertEquals(180, Trials.iceTarget(29))
        val s = Settings(trial = "first_ice", trialStart = today.minusDays(3).toEpochDay())
        val st = Stats(listOf(cold(2, 15), cold(1, 10), cold(0, 30)), now, zone, 11, 0)
        val t = Trials.state(s, st)!!
        assertEquals(2, t.done)
        assertTrue(t.doneToday)
    }

    @Test
    fun levelsClimb() {
        assertEquals(1, Levels.level(0))
        assertEquals(2, Levels.level(60))
        assertEquals(8, Levels.level(10_000))
        assertTrue(Kind.CONTRAST != Kind.SAUNA)
    }

    @Test
    fun coldLimitShrinksWithTemperature() {
        assertTrue(Plans.coldLimit(2.0) < Plans.coldLimit(12.0))
    }
}
