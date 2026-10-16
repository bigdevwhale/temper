package app.temper.ui.forge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.temper.R
import app.temper.container
import app.temper.domain.Trial
import app.temper.domain.Trials
import app.temper.run.Words
import app.temper.ui.TemperData
import app.temper.ui.components.Bar
import app.temper.ui.components.ConfirmDialog
import app.temper.ui.components.IconDisc
import app.temper.ui.components.SoftButton
import app.temper.ui.components.TopBar
import app.temper.ui.components.UCard
import app.temper.ui.trialColor
import app.temper.ui.trialIcon
import app.temper.ui.trialText
import app.temper.ui.trialTitle
import app.temper.ui.theme.U
import app.temper.ui.theme.UType
import kotlinx.coroutines.launch

/** Trials: goals counted in sessions, never broken by a missed day. */
@Composable
fun TrialsScreen(data: TemperData, onBack: () -> Unit) {
    val c = LocalContext.current.container
    val active = data.trial
    var confirm by remember { mutableStateOf<Trial?>(null) }
    var abandon by remember { mutableStateOf(false) }

    fun begin(t: Trial) {
        c.scope.launch { c.settings.update { it.copy(trial = t.id, trialStart = data.stats.today.toEpochDay()) } }
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.trials), onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.trials_intro), style = UType.Sub)
            Spacer(Modifier.height(4.dp))
            Trials.all.forEach { t ->
                val (color, soft) = trialColor(t)
                val isActive = active?.trial?.id == t.id
                UCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconDisc(trialIcon(t), 44.dp, color, soft)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(trialTitle(t), style = UType.Strong)
                            if (t.id in data.settings.trialsDone) Text(stringResource(R.string.trial_passed), style = UType.Small.copy(color = U.c.sage))
                        }
                        if (t.id in data.settings.trialsDone) Icon(Icons.Rounded.CheckCircle, null, tint = U.c.sage)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(trialText(t), style = UType.Muted)
                    Spacer(Modifier.height(12.dp))
                    if (isActive) {
                        Text(stringResource(R.string.trial_progress, active!!.done, t.goal), style = UType.Strong.copy(color = color))
                        if (t.id == "first_ice" && !active.finished) Text(stringResource(R.string.trial_next_ice, Words.clock(active.target)), style = UType.Muted)
                        Spacer(Modifier.height(8.dp))
                        Bar(active.done / t.goal.toFloat(), color = color, track = soft)
                        Spacer(Modifier.height(12.dp))
                        SoftButton(stringResource(R.string.trial_abandon), { abandon = true }, Modifier.fillMaxWidth(), color = U.c.muted, soft = U.c.surface2)
                    } else {
                        SoftButton(stringResource(R.string.trial_begin), { if (active != null) confirm = t else begin(t) }, Modifier.fillMaxWidth(), color = color, soft = soft)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    confirm?.let { t ->
        ConfirmDialog(
            stringResource(R.string.trial_switch), stringResource(R.string.trial_switch_text), stringResource(R.string.trial_begin),
            onConfirm = { begin(t) }, onDismiss = { confirm = null },
        )
    }
    if (abandon) ConfirmDialog(
        stringResource(R.string.trial_abandon), stringResource(R.string.trial_abandon_text), stringResource(R.string.trial_abandon),
        onConfirm = { c.scope.launch { c.settings.update { it.copy(trial = "") } } }, onDismiss = { abandon = false }, danger = true,
    )
}
