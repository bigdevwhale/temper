package app.temper.ui.forge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.temper.R
import app.temper.data.Kind
import app.temper.data.Session
import app.temper.data.Style
import app.temper.domain.Levels
import app.temper.run.Words
import app.temper.ui.Go
import app.temper.ui.Tab
import app.temper.ui.TemperData
import app.temper.ui.components.Bar
import app.temper.ui.components.Blade
import app.temper.ui.components.IconDisc
import app.temper.ui.components.SectionTitle
import app.temper.ui.components.UCard
import app.temper.ui.components.layersOf
import app.temper.ui.content
import app.temper.ui.daysCount
import app.temper.ui.glowText
import app.temper.ui.kindColor
import app.temper.ui.kindIcon
import app.temper.ui.kindName
import app.temper.ui.kindSoft
import app.temper.ui.levelName
import app.temper.ui.minutesText
import app.temper.ui.styleIcon
import app.temper.ui.styleName
import app.temper.ui.timeOf
import app.temper.ui.trialColor
import app.temper.ui.trialIcon
import app.temper.ui.trialTitle
import app.temper.ui.theme.U
import app.temper.ui.theme.UType
import app.temper.ui.theme.temperColor

@Composable
fun ForgeTab(data: TemperData, go: Go, onTab: (Tab) -> Unit) {
    val st = data.stats
    val s = data.settings
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(18.dp))
        val hour = java.time.LocalTime.now().hour
        val greet = stringResource(
            when (hour) { in 5..11 -> R.string.greet_morning; in 12..17 -> R.string.greet_day; in 18..22 -> R.string.greet_evening; else -> R.string.greet_night }
        )
        Text(if (s.name.isBlank()) greet else "$greet, ${s.name}", style = UType.Muted)
        Text(stringResource(R.string.forge_title), style = UType.Display)
        Spacer(Modifier.height(14.dp))

        // The blade.
        UCard(onClick = { onTab(Tab.BLADE) }) {
            Blade(layersOf(data.sessions), data.level, st.glow, st.streak, Modifier.fillMaxWidth().aspectRatio(2.9f))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(temperColor(data.level)).border(1.dp, U.c.line, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(levelName(data.level), style = UType.Strong, modifier = Modifier.weight(1f))
                Text(glowText(st.glow), style = UType.Small.copy(color = if (st.glow.ordinal <= 1) U.c.accent else U.c.ice))
            }
            Spacer(Modifier.height(8.dp))
            Bar(Levels.progress(st.xp), color = temperColor(data.level), track = U.c.surface2)
        }

        // Light the forge.
        SectionTitle(stringResource(R.string.forge_start))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Kind.entries.forEach { k -> StartTile(k, Modifier.weight(1f)) { go.setup(k) } }
        }

        // The week.
        SectionTitle(stringResource(R.string.forge_week))
        UCard {
            if (s.coldGoal > 0) NormRow(stringResource(R.string.norm_cold), st.weekColdSec, s.coldGoal, U.c.ice, U.c.iceSoft)
            if (s.coldGoal > 0 && s.heatGoal > 0) Spacer(Modifier.height(14.dp))
            if (s.heatGoal > 0) NormRow(stringResource(R.string.norm_heat), st.weekHeatSec, s.heatGoal, U.c.accent, U.c.accentSoft)
            Spacer(Modifier.height(12.dp))
            Text(
                if (st.weekStreak > 0) stringResource(R.string.norm_weeks, st.weekStreak) else stringResource(R.string.norm_note),
                style = UType.Small.copy(color = U.c.muted),
            )
        }

        // Trial.
        SectionTitle(stringResource(R.string.forge_trial))
        val t = data.trial
        if (t != null) {
            val (color, soft) = trialColor(t.trial)
            UCard(onClick = { go.trials() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconDisc(trialIcon(t.trial), 44.dp, color, soft)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(trialTitle(t.trial), style = UType.Strong)
                        Text(
                            when {
                                t.doneToday -> stringResource(R.string.trial_today_done)
                                t.trial.id == "first_ice" -> stringResource(R.string.trial_today_ice, Words.clock(t.target))
                                else -> stringResource(R.string.trial_progress, t.done, t.trial.goal)
                            },
                            style = UType.Muted,
                        )
                    }
                    if (t.doneToday) Icon(Icons.Rounded.CheckCircle, null, tint = U.c.sage)
                    else Text("${t.done}/${t.trial.goal}", style = UType.Number.copy(fontSize = 18.sp, color = color))
                }
                Spacer(Modifier.height(10.dp))
                Bar(t.done / t.trial.goal.toFloat(), color = color, track = soft)
            }
        } else {
            UCard(onClick = { go.trials() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconDisc(Icons.Rounded.EmojiEvents, 44.dp, U.c.gold, U.c.goldSoft)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.trial_pick), style = UType.Strong)
                        Text(stringResource(R.string.trial_pick_sub), style = UType.Muted)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
                }
            }
        }

        // Today.
        val today = st.on(st.today)
        SectionTitle(stringResource(R.string.today))
        if (today.isEmpty()) {
            UCard {
                Text(
                    if (st.streak > 0) stringResource(R.string.forge_today_empty_streak, daysCount(st.streak)) else stringResource(R.string.forge_today_empty),
                    style = UType.Muted,
                )
            }
        } else UCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
            today.forEach { SessionRow(it) { go.session(it.id) } }
        }

        // Tip.
        SectionTitle(stringResource(R.string.forge_tip))
        UCard {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Lightbulb, null, tint = U.c.gold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(content().tip(st.today.toEpochDay()), style = UType.Body)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StartTile(kind: Kind, modifier: Modifier, onClick: () -> Unit) {
    val color = kindColor(kind)
    Column(
        modifier
            .clip(U.RMd)
            .background(kindSoft(kind))
            .border(1.dp, color.copy(alpha = 0.35f), U.RMd)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(kindIcon(kind), null, tint = color, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(8.dp))
        Text(kindName(kind), style = UType.Strong.copy(fontSize = 14.sp, color = color), maxLines = 1)
    }
}

@Composable
private fun NormRow(label: String, sec: Int, goalMin: Int, color: androidx.compose.ui.graphics.Color, soft: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(label, style = UType.Body, modifier = Modifier.weight(1f))
        Text(minutesText(sec.toLong()), style = UType.Number.copy(fontSize = 18.sp, color = color))
        Text(" / " + stringResource(R.string.min_short, goalMin), style = UType.Muted)
    }
    Spacer(Modifier.height(6.dp))
    Bar(sec / (goalMin * 60f), color = color, track = soft)
}

@Composable
fun SessionRow(s: Session, onClick: () -> Unit) {
    val style = Style.of(s.style)
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconDisc(styleIcon(style), 40.dp, kindColor(s.kind), kindSoft(s.kind))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(styleName(style), style = UType.Strong)
            val parts = buildList {
                if (s.coldSec > 0) add("❄ " + Words.clock(s.coldSec))
                if (s.heatSec > 0) add("🔥 " + Words.clock(s.heatSec))
                if (s.rounds > 1) add("× ${s.rounds}")
            }
            Text(timeOf(s.start) + " · " + parts.joinToString("  "), style = UType.Muted)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
    }
}
