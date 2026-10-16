package app.temper.ui.session

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.temper.R
import app.temper.container
import app.temper.ui.TemperData
import app.temper.ui.components.Blade
import app.temper.ui.components.PrimaryButton
import app.temper.ui.components.ScalePicker
import app.temper.ui.components.SectionTitle
import app.temper.ui.components.Sparks
import app.temper.ui.components.Stat
import app.temper.ui.components.UCard
import app.temper.ui.components.UTextField
import app.temper.ui.components.layersOf
import app.temper.ui.content
import app.temper.ui.feelFace
import app.temper.ui.kindColor
import app.temper.ui.temp
import app.temper.run.Words
import app.temper.ui.theme.U
import app.temper.ui.theme.UType

/** The session is quenched: a new layer on the blade, and how it felt. */
@Composable
fun FinishScreen(id: Long, data: TemperData, onDone: () -> Unit) {
    val c = LocalContext.current.container
    val session = data.sessions.firstOrNull { it.id == id }
    if (session == null) {
        // The list may lag behind the insert for a moment; only leave if the session is really gone.
        LaunchedEffect(id) { if (c.db.dao().session(id) == null) onDone() }
        Box(Modifier.fillMaxSize().background(U.c.bg))
        return
    }
    var energy by rememberSaveable { mutableIntStateOf(session.energy ?: 0) }
    var mood by rememberSaveable { mutableIntStateOf(session.mood ?: 0) }
    var note by rememberSaveable { mutableStateOf(session.note) }
    val glow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { glow.animateTo(1f, tween(1800)) }
    val color = kindColor(session.kind)

    fun save() {
        c.write { dao().updateSession(session.copy(energy = energy.takeIf { it > 0 }, mood = mood.takeIf { it > 0 }, note = note.trim())) }
        onDone()
    }

    Box(Modifier.fillMaxSize().background(U.c.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.finish_eyebrow).uppercase(), style = UType.Eyebrow.copy(color = color), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.finish_title), style = UType.Display.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Blade(layersOf(data.sessions), data.level, data.stats.glow, data.stats.streak, Modifier.fillMaxWidth().aspectRatio(2.9f), highlight = glow.value)
                Spacer(Modifier.height(8.dp))
                Text(content().cheer(session.start / 1000), style = UType.Quote.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(18.dp))
                UCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        if (session.coldSec > 0) Stat(Words.clock(session.coldSec), stringResource(R.string.in_cold), color = U.c.ice)
                        if (session.heatSec > 0) Stat(Words.clock(session.heatSec), stringResource(R.string.in_heat), color = U.c.accent)
                        if (session.rounds > 1) Stat("${session.rounds}", stringResource(R.string.rounds))
                        session.waterC?.let { Stat(temp(it, data.settings.unit), stringResource(R.string.water)) }
                        session.airC?.let { Stat(temp(it, data.settings.unit), stringResource(R.string.air)) }
                    }
                }
                SectionTitle(stringResource(R.string.finish_energy))
                ScalePicker(energy, { energy = it }, { feelFace(it) }, color = color)
                SectionTitle(stringResource(R.string.finish_mood))
                ScalePicker(mood, { mood = it }, { feelFace(it) }, color = color)
                SectionTitle(stringResource(R.string.note))
                UTextField(note, { note = it }, placeholder = stringResource(R.string.finish_note_hint), minLines = 2)
                Spacer(Modifier.height(20.dp))
            }
            PrimaryButton(stringResource(R.string.save), ::save, Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), color = color, icon = Icons.Rounded.Check)
        }
        Sparks(Modifier.fillMaxSize(), key = id)
    }
}
