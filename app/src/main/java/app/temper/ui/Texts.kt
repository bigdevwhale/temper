package app.temper.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Bathtub
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Hardware
import androidx.compose.material.icons.rounded.HotTub
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shower
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Water
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.temper.R
import app.temper.content.Content
import app.temper.data.Kind
import app.temper.data.Style
import app.temper.data.TempUnit
import app.temper.domain.Glow
import app.temper.domain.Plan
import app.temper.domain.Step
import app.temper.domain.StepType
import app.temper.domain.Trial
import app.temper.run.Words
import app.temper.ui.theme.U
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun locale(): Locale = LocalConfiguration.current.locales[0]

/** Texts in the current app language; switches along with it. */
@Composable
fun content(): Content {
    val l = locale()
    return remember(l.language) { Content.of(l) }
}

@Composable
fun kindColor(kind: Kind): Color = when (kind) {
    Kind.COLD -> U.c.ice
    Kind.CONTRAST -> U.c.gold
    Kind.SAUNA -> U.c.accent
}

@Composable
fun kindSoft(kind: Kind): Color = when (kind) {
    Kind.COLD -> U.c.iceSoft
    Kind.CONTRAST -> U.c.goldSoft
    Kind.SAUNA -> U.c.accentSoft
}

fun kindIcon(kind: Kind): ImageVector = when (kind) {
    Kind.COLD -> Icons.Rounded.AcUnit
    Kind.CONTRAST -> Icons.Rounded.Shower
    Kind.SAUNA -> Icons.Rounded.Whatshot
}

@Composable
fun kindName(kind: Kind): String = stringResource(
    when (kind) {
        Kind.COLD -> R.string.kind_cold
        Kind.CONTRAST -> R.string.kind_contrast
        Kind.SAUNA -> R.string.kind_sauna
    }
)

@Composable
fun styleName(style: Style): String = stringResource(
    when (style) {
        Style.SHOWER -> R.string.style_shower
        Style.BATH -> R.string.style_bath
        Style.OPEN -> R.string.style_open
        Style.ICE -> R.string.style_ice
        Style.CONTRAST -> R.string.kind_contrast
        Style.FINNISH -> R.string.style_finnish
        Style.BANYA -> R.string.style_banya
        Style.HAMMAM -> R.string.style_hammam
        Style.INFRARED -> R.string.style_infrared
    }
)

fun styleIcon(style: Style): ImageVector = when (style) {
    Style.SHOWER -> Icons.Rounded.Shower
    Style.BATH -> Icons.Rounded.Bathtub
    Style.OPEN -> Icons.Rounded.Pool
    Style.ICE -> Icons.Rounded.AcUnit
    Style.CONTRAST -> Icons.Rounded.Shower
    Style.FINNISH -> Icons.Rounded.Whatshot
    Style.BANYA -> Icons.Rounded.Spa
    Style.HAMMAM -> Icons.Rounded.HotTub
    Style.INFRARED -> Icons.Rounded.LocalFireDepartment
}

@Composable
fun planName(p: Plan): String = p.name ?: stringResource(
    when (p.key) {
        "gentle" -> R.string.plan_gentle
        "classic" -> R.string.plan_classic
        "awaken" -> R.string.plan_awaken
        "recovery" -> R.string.plan_recovery
        "steel" -> R.string.plan_steel
        "sauna_gentle" -> R.string.plan_sauna_gentle
        "finnish" -> R.string.plan_finnish
        "banya" -> R.string.plan_banya
        "soberg" -> R.string.plan_soberg
        else -> R.string.plan_hammam
    }
)

@Composable
fun planNote(p: Plan): String = if (p.custom) "" else stringResource(
    when (p.key) {
        "gentle" -> R.string.plan_gentle_note
        "classic" -> R.string.plan_classic_note
        "awaken" -> R.string.plan_awaken_note
        "recovery" -> R.string.plan_recovery_note
        "steel" -> R.string.plan_steel_note
        "sauna_gentle" -> R.string.plan_sauna_gentle_note
        "finnish" -> R.string.plan_finnish_note
        "banya" -> R.string.plan_banya_note
        "soberg" -> R.string.plan_soberg_note
        else -> R.string.plan_hammam_note
    }
)

/** "3 × 1:30 hot / 0:30 cold". */
@Composable
fun planShape(p: Plan): String = if (p.kind == Kind.SAUNA)
    stringResource(R.string.plan_shape_sauna, p.rounds, minutes(p.heatSec), Words.clock(p.coldSec), minutes(p.restSec))
else stringResource(R.string.plan_shape_contrast, p.rounds, Words.clock(p.heatSec), Words.clock(p.coldSec))

private fun minutes(sec: Int): String = if (sec % 60 == 0) "${sec / 60}" else Words.clock(sec)

@Composable
fun stepName(step: Step, kind: Kind): String {
    val context = LocalContext.current
    return remember(step, kind, locale()) { Words.step(context, step, kind) }
}

@Composable
fun stepColor(type: StepType): Color = when (type) {
    StepType.HEAT -> U.c.accent
    StepType.COLD -> U.c.ice
    StepType.BREATHE -> U.c.sage
    StepType.REST -> U.c.steel
    StepType.WARM -> U.c.gold
}

@Composable
fun duration(sec: Int): String {
    val context = LocalContext.current
    return remember(sec, locale()) { Words.duration(context, sec) }
}

/** Minutes, compact: "12 min", "1 h 05 min". */
@Composable
fun minutesText(sec: Long): String {
    val m = (sec / 60).toInt()
    return if (m < 60) stringResource(R.string.min_short, m) else stringResource(R.string.h_min_short, m / 60, m % 60)
}

@Composable
fun temp(c: Double, unit: TempUnit): String =
    if (unit == TempUnit.F) stringResource(R.string.temp_f, (c * 9 / 5 + 32).roundToInt())
    else stringResource(R.string.temp_c, if (c % 1.0 == 0.0) c.toInt().toString() else "%.1f".format(locale(), c))

@Composable
fun levelName(level: Int): String = stringResource(
    when (level) {
        1 -> R.string.temper_raw
        2 -> R.string.temper_pale
        3 -> R.string.temper_straw
        4 -> R.string.temper_bronze
        5 -> R.string.temper_purple
        6 -> R.string.temper_blue
        7 -> R.string.temper_sky
        else -> R.string.temper_grey
    }
)

@Composable
fun glowText(g: Glow): String = stringResource(
    when (g) {
        Glow.HOT -> R.string.glow_hot
        Glow.WARM -> R.string.glow_warm
        Glow.COOLING -> R.string.glow_cooling
        Glow.COLD -> R.string.glow_cold
    }
)

@Composable
fun badgeTitle(id: String): String = stringResource(badgeRes(id).first)

@Composable
fun badgeText(id: String): String = stringResource(badgeRes(id).second)

private fun badgeRes(id: String): Pair<Int, Int> = when (id) {
    "first_cold" -> R.string.b_first_cold to R.string.b_first_cold_t
    "first_contrast" -> R.string.b_first_contrast to R.string.b_first_contrast_t
    "first_sauna" -> R.string.b_first_sauna to R.string.b_first_sauna_t
    "cold_1m" -> R.string.b_cold_1m to R.string.b_cold_1m_t
    "cold_3m" -> R.string.b_cold_3m to R.string.b_cold_3m_t
    "cold_5m" -> R.string.b_cold_5m to R.string.b_cold_5m_t
    "water_10" -> R.string.b_water_10 to R.string.b_water_10_t
    "water_4" -> R.string.b_water_4 to R.string.b_water_4_t
    "open_water" -> R.string.b_open_water to R.string.b_open_water_t
    "ice_hole" -> R.string.b_ice_hole to R.string.b_ice_hole_t
    "dawn" -> R.string.b_dawn to R.string.b_dawn_t
    "soberg" -> R.string.b_soberg to R.string.b_soberg_t
    "sessions_10" -> R.string.b_sessions_10 to R.string.b_sessions_10_t
    "sessions_50" -> R.string.b_sessions_50 to R.string.b_sessions_50_t
    "sessions_100" -> R.string.b_sessions_100 to R.string.b_sessions_100_t
    "streak_7" -> R.string.b_streak_7 to R.string.b_streak_7_t
    "streak_30" -> R.string.b_streak_30 to R.string.b_streak_30_t
    "week_full" -> R.string.b_week_full to R.string.b_week_full_t
    "weeks_4" -> R.string.b_weeks_4 to R.string.b_weeks_4_t
    "fear_down" -> R.string.b_fear_down to R.string.b_fear_down_t
    "heat_10h" -> R.string.b_heat_10h to R.string.b_heat_10h_t
    "rounds_50" -> R.string.b_rounds_50 to R.string.b_rounds_50_t
    "trinity" -> R.string.b_trinity to R.string.b_trinity_t
    else -> R.string.b_trial to R.string.b_trial_t
}

fun badgeIcon(id: String): ImageVector = when (id) {
    "first_cold", "water_10" -> Icons.Rounded.WaterDrop
    "first_contrast", "rounds_50" -> Icons.Rounded.Repeat
    "first_sauna", "heat_10h" -> Icons.Rounded.Whatshot
    "cold_1m", "cold_3m", "cold_5m" -> Icons.Rounded.Timer
    "water_4", "ice_hole" -> Icons.Rounded.AcUnit
    "open_water" -> Icons.Rounded.Water
    "dawn" -> Icons.Rounded.WbTwilight
    "soberg" -> Icons.Rounded.Thermostat
    "sessions_10", "sessions_50", "sessions_100" -> Icons.Rounded.Hardware
    "streak_7", "streak_30" -> Icons.Rounded.LocalFireDepartment
    "week_full", "weeks_4" -> Icons.Rounded.CalendarMonth
    "fear_down" -> Icons.Rounded.Favorite
    "trinity" -> Icons.Rounded.AllInclusive
    else -> Icons.Rounded.MilitaryTech
}

@Composable
fun badgeColor(id: String): Pair<Color, Color> = when (id) {
    "first_cold", "cold_1m", "cold_3m", "cold_5m", "water_10", "water_4", "open_water", "ice_hole" -> U.c.ice to U.c.iceSoft
    "first_sauna", "heat_10h", "soberg", "streak_7", "streak_30" -> U.c.accent to U.c.accentSoft
    "first_contrast", "rounds_50", "trinity", "dawn" -> U.c.gold to U.c.goldSoft
    "fear_down", "week_full", "weeks_4" -> U.c.sage to U.c.sageSoft
    else -> U.c.steel to U.c.steelSoft
}

@Composable
fun trialTitle(t: Trial): String = stringResource(
    when (t.id) {
        "first_ice" -> R.string.trial_first_ice
        "dawn" -> R.string.trial_dawn
        "contrast" -> R.string.trial_contrast
        "sauna" -> R.string.trial_sauna
        else -> R.string.trial_winter
    }
)

@Composable
fun trialText(t: Trial): String = stringResource(
    when (t.id) {
        "first_ice" -> R.string.trial_first_ice_t
        "dawn" -> R.string.trial_dawn_t
        "contrast" -> R.string.trial_contrast_t
        "sauna" -> R.string.trial_sauna_t
        else -> R.string.trial_winter_t
    }
)

fun trialIcon(t: Trial): ImageVector = when (t.id) {
    "first_ice" -> Icons.Rounded.AcUnit
    "dawn" -> Icons.Rounded.WbTwilight
    "contrast" -> Icons.Rounded.Repeat
    "sauna" -> Icons.Rounded.Whatshot
    else -> Icons.Rounded.Water
}

@Composable
fun trialColor(t: Trial): Pair<Color, Color> = when (t.id) {
    "first_ice", "winter" -> U.c.ice to U.c.iceSoft
    "dawn" -> U.c.gold to U.c.goldSoft
    "contrast" -> U.c.sage to U.c.sageSoft
    else -> U.c.accent to U.c.accentSoft
}

fun topicIcon(topic: String): ImageVector = when (topic) {
    "cold" -> Icons.Rounded.AcUnit
    "contrast" -> Icons.Rounded.Shower
    "sauna" -> Icons.Rounded.Whatshot
    "safety" -> Icons.Rounded.Favorite
    else -> Icons.Rounded.Bolt
}

@Composable
fun topicColor(topic: String): Pair<Color, Color> = when (topic) {
    "cold" -> U.c.ice to U.c.iceSoft
    "contrast" -> U.c.gold to U.c.goldSoft
    "sauna" -> U.c.accent to U.c.accentSoft
    "safety" -> U.c.sage to U.c.sageSoft
    else -> U.c.steel to U.c.steelSoft
}

@Composable
fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.today)
    today.minusDays(1) -> stringResource(R.string.yesterday)
    else -> DateTimeFormatter.ofPattern(if (date.year == today.year) "EEEE, d MMMM" else "d MMMM yyyy", locale()).format(date)
        .replaceFirstChar { it.titlecase(locale()) }
}

@Composable
fun timeOf(ms: Long): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale()).format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

fun fearFace(v: Int): String = when (v) { 1 -> "😌"; 2 -> "🙂"; 3 -> "😬"; 4 -> "😨"; else -> "🥶" }

fun feelFace(v: Int): String = when (v) { 1 -> "😣"; 2 -> "😕"; 3 -> "😐"; 4 -> "🙂"; else -> "🤩" }

@Composable
fun sessionsCount(n: Int): String = pluralStringResource(R.plurals.sessions, n, n)

@Composable
fun daysCount(n: Int): String = pluralStringResource(R.plurals.days, n, n)
