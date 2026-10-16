package app.temper.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import app.temper.data.Program
import app.temper.domain.Plans
import app.temper.run.Words
import app.temper.ui.components.ConfirmDialog
import app.temper.ui.components.PrimaryButton
import app.temper.ui.components.SectionTitle
import app.temper.ui.components.Stepper
import app.temper.ui.components.SwitchRow
import app.temper.ui.components.TopBar
import app.temper.ui.components.UCard
import app.temper.ui.components.UTextField
import app.temper.ui.kindColor
import app.temper.ui.planShape
import app.temper.ui.theme.U
import app.temper.ui.theme.UType

/** A contrast or sauna program of the user's own. [id] 0 makes a new one. */
@Composable
fun ProgramScreen(kind: Kind, id: Long, onDone: () -> Unit) {
    val c = LocalContext.current.container
    val sauna = kind == Kind.SAUNA
    var loaded by rememberSaveable { mutableStateOf(id == 0L) }
    var name by rememberSaveable { mutableStateOf("") }
    var heat by rememberSaveable { mutableIntStateOf(if (sauna) 12 * 60 else 90) }
    var cold by rememberSaveable { mutableIntStateOf(if (sauna) 60 else 30) }
    var rest by rememberSaveable { mutableIntStateOf(if (sauna) 10 * 60 else 0) }
    var rounds by rememberSaveable { mutableIntStateOf(3) }
    var endCold by rememberSaveable { mutableStateOf(true) }
    var askDelete by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(id) {
        if (id != 0L && !loaded) c.db.dao().program(id)?.let {
            name = it.name; heat = it.heatSec; cold = it.coldSec; rest = it.restSec; rounds = it.rounds; endCold = it.endCold
        }
        loaded = true
    }
    val color = kindColor(kind)
    val heatStep = if (sauna) 60 else 15
    val program = Program(id, kind.name, name.trim(), heat, cold, rest, rounds, endCold)

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(if (id == 0L) R.string.program_new else R.string.program_edit), onBack = onDone) {
            if (id != 0L) IconButton(onClick = { askDelete = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = U.c.muted) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            SectionTitle(stringResource(R.string.program_name))
            UTextField(name, { name = it.take(40) }, placeholder = stringResource(R.string.program_name_hint))
            SectionTitle(stringResource(R.string.program_rhythm))
            UCard {
                Stepper(stringResource(if (sauna) R.string.step_sauna else R.string.step_hot), Words.clock(heat), { heat = (heat - heatStep).coerceAtLeast(heatStep) }, { heat = (heat + heatStep).coerceAtMost(if (sauna) 30 * 60 else 600) }, color = U.c.accent)
                Stepper(stringResource(if (sauna) R.string.step_cool else R.string.step_cold), Words.clock(cold), { cold = (cold - 15).coerceAtLeast(if (sauna) 0 else 15) }, { cold = (cold + 15).coerceAtMost(600) }, color = U.c.ice)
                if (sauna) Stepper(stringResource(R.string.step_rest), Words.clock(rest), { rest = (rest - 60).coerceAtLeast(0) }, { rest = (rest + 60).coerceAtMost(40 * 60) }, color = U.c.steel)
                Stepper(stringResource(R.string.rounds), "$rounds", { rounds = (rounds - 1).coerceAtLeast(1) }, { rounds = (rounds + 1).coerceAtMost(12) }, color = color)
            }
            Spacer(Modifier.height(8.dp))
            UCard {
                SwitchRow(stringResource(R.string.program_end_cold), stringResource(R.string.program_end_cold_sub), endCold, { endCold = it }, U.c.ice)
            }
            Spacer(Modifier.height(12.dp))
            val plan = Plans.of(program.copy(name = name.ifBlank { "·" }))
            Text(planShape(plan) + " · " + stringResource(R.string.about_min, (plan.totalSec + 59) / 60), style = UType.Muted)
            Spacer(Modifier.height(20.dp))
        }
        PrimaryButton(
            stringResource(R.string.save),
            {
                c.write { if (id == 0L) dao().insertProgram(program.copy(name = name.trim())) else dao().updateProgram(program) }
                onDone()
            },
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp).navigationBarsPadding(),
            enabled = name.isNotBlank(), color = color,
        )
    }
    if (askDelete) ConfirmDialog(
        stringResource(R.string.program_delete), stringResource(R.string.program_delete_text), stringResource(R.string.delete),
        onConfirm = { c.write { dao().let { d -> d.program(id)?.let { d.deleteProgram(it) } } }; onDone() }, onDismiss = { askDelete = false }, danger = true,
    )
}
