package app.temper.ui.blade

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.temper.R
import app.temper.container
import app.temper.data.Kind
import app.temper.data.Session
import app.temper.data.Style
import app.temper.run.Words
import app.temper.ui.TemperData
import app.temper.ui.components.Chip
import app.temper.ui.components.ConfirmDialog
import app.temper.ui.components.DateTimeDialog
import app.temper.ui.components.InfoRow
import app.temper.ui.components.PrimaryButton
import app.temper.ui.components.ScalePicker
import app.temper.ui.components.SectionTitle
import app.temper.ui.components.Stepper
import app.temper.ui.components.SwitchRow
import app.temper.ui.components.TopBar
import app.temper.ui.components.UCard
import app.temper.ui.components.UTextField
import app.temper.ui.dayLabel
import app.temper.ui.fearFace
import app.temper.ui.feelFace
import app.temper.ui.kindColor
import app.temper.ui.styleName
import app.temper.ui.temp
import app.temper.ui.timeOf
import app.temper.ui.theme.U
import java.time.Instant
import java.time.ZoneId

/** A session by hand, or a fix to one already in the ledger. [id] 0 adds a new one. */
@Composable
fun SessionEditScreen(id: Long, data: TemperData, onDone: () -> Unit) {
    val c = LocalContext.current.container
    var loaded by rememberSaveable { mutableStateOf(id == 0L) }
    var style by rememberSaveable { mutableStateOf(Style.of(data.settings.coldStyle)) }
    var start by rememberSaveable { mutableLongStateOf(System.currentTimeMillis() - 15 * 60_000L) }
    var cold by rememberSaveable { mutableIntStateOf(60) }
    var heat by rememberSaveable { mutableIntStateOf(0) }
    var rounds by rememberSaveable { mutableIntStateOf(1) }
    var hasWater by rememberSaveable { mutableStateOf(false) }
    var water by rememberSaveable { mutableDoubleStateOf(data.settings.waterC) }
    var hasAir by rememberSaveable { mutableStateOf(false) }
    var air by rememberSaveable { mutableDoubleStateOf(data.settings.saunaC) }
    var fear by rememberSaveable { mutableIntStateOf(0) }
    var energy by rememberSaveable { mutableIntStateOf(0) }
    var mood by rememberSaveable { mutableIntStateOf(0) }
    var note by rememberSaveable { mutableStateOf("") }
    var spot by rememberSaveable { mutableStateOf("") }
    var endedCold by rememberSaveable { mutableStateOf(false) }
    var program by rememberSaveable { mutableStateOf("") }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var askDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != 0L && !loaded) c.db.dao().session(id)?.let {
            style = Style.of(it.style); start = it.start; cold = it.coldSec; heat = it.heatSec; rounds = it.rounds
            hasWater = it.waterC != null; water = it.waterC ?: water; hasAir = it.airC != null; air = it.airC ?: air
            fear = it.fear ?: 0; energy = it.energy ?: 0; mood = it.mood ?: 0; note = it.note; spot = it.spot
            endedCold = it.endedCold; program = it.program
        }
        loaded = true
    }
    val kind = style.kind
    val color = kindColor(kind)
    val zone = ZoneId.systemDefault()

    fun save() {
        val s = Session(
            id = id, style = style.name, start = start, end = start + (cold + heat) * 1000L + if (kind == Kind.SAUNA) rounds * 10 * 60_000L else 0,
            coldSec = cold, heatSec = heat, rounds = rounds,
            waterC = water.takeIf { hasWater && cold > 0 }, airC = air.takeIf { hasAir && kind == Kind.SAUNA },
            fear = fear.takeIf { it > 0 }, energy = energy.takeIf { it > 0 }, mood = mood.takeIf { it > 0 },
            note = note.trim(), program = program, endedCold = if (kind == Kind.COLD) true else endedCold && cold > 0, spot = spot.trim(),
        )
        c.write { if (id == 0L) dao().insertSession(s) else dao().updateSession(s) }
        onDone()
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(if (id == 0L) R.string.session_add else R.string.session_edit), onBack = onDone) {
            if (id != 0L) IconButton(onClick = { askDelete = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = U.c.muted) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            SectionTitle(stringResource(R.string.session_what))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Style.entries.forEach { st ->
                    Chip(styleName(st), st == style, {
                        style = st
                        when (st.kind) {
                            Kind.COLD -> { heat = 0; rounds = 1 }
                            Kind.CONTRAST -> { if (heat == 0) heat = 270; if (cold == 0) cold = 90; if (rounds < 2) rounds = 3 }
                            Kind.SAUNA -> { if (heat < 300) heat = 36 * 60; if (rounds < 2) rounds = 3 }
                        }
                    }, kindColor(st.kind))
                }
            }
            Spacer(Modifier.height(10.dp))
            UCard {
                InfoRow(Icons.Rounded.CalendarMonth, stringResource(R.string.session_when), dayLabel(Instant.ofEpochMilli(start).atZone(zone).toLocalDate(), data.stats.today) + ", " + timeOf(start), color, color.copy(alpha = 0.14f)) { pickDate = true }
            }
            SectionTitle(stringResource(R.string.session_time))
            UCard {
                Stepper(stringResource(R.string.in_cold), Words.clock(cold), { cold = (cold - 15).coerceAtLeast(0) }, { cold = (cold + 15).coerceAtMost(3600) }, color = U.c.ice)
                if (kind != Kind.COLD) {
                    Stepper(stringResource(R.string.in_heat), Words.clock(heat), { heat = (heat - if (kind == Kind.SAUNA) 60 else 15).coerceAtLeast(0) }, { heat = (heat + if (kind == Kind.SAUNA) 60 else 15).coerceAtMost(4 * 3600) }, color = U.c.accent)
                    Stepper(stringResource(R.string.rounds), "$rounds", { rounds = (rounds - 1).coerceAtLeast(1) }, { rounds = (rounds + 1).coerceAtMost(12) }, color = color)
                    SwitchRow(stringResource(R.string.program_end_cold), null, endedCold, { endedCold = it }, U.c.ice)
                }
            }
            SectionTitle(stringResource(R.string.session_temps))
            UCard {
                if (cold > 0) {
                    SwitchRow(stringResource(R.string.setup_know_water), null, hasWater, { hasWater = it }, U.c.ice)
                    if (hasWater) Stepper(stringResource(R.string.water), temp(water, data.settings.unit), { water = (water - 1).coerceAtLeast(0.0) }, { water = (water + 1).coerceAtMost(30.0) }, color = U.c.ice)
                }
                if (kind == Kind.SAUNA) {
                    SwitchRow(stringResource(R.string.session_know_air), null, hasAir, { hasAir = it }, U.c.accent)
                    if (hasAir) Stepper(stringResource(R.string.air), temp(air, data.settings.unit), { air = (air - 5).coerceAtLeast(40.0) }, { air = (air + 5).coerceAtMost(120.0) }, color = U.c.accent)
                }
            }
            if (style == Style.OPEN || style == Style.ICE || kind == Kind.SAUNA) {
                SectionTitle(stringResource(R.string.session_spot))
                UTextField(spot, { spot = it }, placeholder = stringResource(R.string.setup_spot))
            }
            SectionTitle(stringResource(R.string.setup_fear))
            ScalePicker(fear, { fear = if (fear == it) 0 else it }, { fearFace(it) }, color = color)
            SectionTitle(stringResource(R.string.finish_energy))
            ScalePicker(energy, { energy = if (energy == it) 0 else it }, { feelFace(it) }, color = color)
            SectionTitle(stringResource(R.string.finish_mood))
            ScalePicker(mood, { mood = if (mood == it) 0 else it }, { feelFace(it) }, color = color)
            SectionTitle(stringResource(R.string.note))
            UTextField(note, { note = it }, placeholder = stringResource(R.string.finish_note_hint), minLines = 2)
            Spacer(Modifier.height(20.dp))
        }
        PrimaryButton(
            stringResource(R.string.save), ::save,
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp).navigationBarsPadding(),
            enabled = cold + heat > 0, color = color,
        )
    }
    if (pickDate) DateTimeDialog(start, onDismiss = { pickDate = false }) { start = it }
    if (askDelete) ConfirmDialog(
        stringResource(R.string.session_delete), stringResource(R.string.session_delete_text), stringResource(R.string.delete),
        onConfirm = { c.write { dao().let { d -> d.session(id)?.let { d.deleteSession(it) } } }; onDone() }, onDismiss = { askDelete = false }, danger = true,
    )
}
