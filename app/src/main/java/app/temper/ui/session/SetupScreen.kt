package app.temper.ui.session

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.temper.R
import app.temper.container
import app.temper.data.Kind
import app.temper.data.Style
import app.temper.domain.Plan
import app.temper.domain.Plans
import app.temper.run.Engine
import app.temper.run.Setup
import app.temper.run.Words
import app.temper.ui.Go
import app.temper.ui.TemperData
import app.temper.ui.components.Chip
import app.temper.ui.components.PrimaryButton
import app.temper.ui.components.ScalePicker
import app.temper.ui.components.SectionTitle
import app.temper.ui.components.SelectCard
import app.temper.ui.components.Stepper
import app.temper.ui.components.SwitchRow
import app.temper.ui.components.TopBar
import app.temper.ui.components.UCard
import app.temper.ui.components.UTextField
import app.temper.ui.fearFace
import app.temper.ui.kindColor
import app.temper.ui.kindName
import app.temper.ui.planName
import app.temper.ui.planNote
import app.temper.ui.planShape
import app.temper.ui.styleName
import app.temper.ui.temp
import app.temper.ui.theme.U
import app.temper.ui.theme.UType
import kotlinx.coroutines.launch

/** Choose how, then light the forge. */
@Composable
fun SetupScreen(kind: Kind, data: TemperData, go: Go) {
    val context = LocalContext.current
    val c = context.container
    val s = data.settings
    val color = kindColor(kind)

    var coldStyle by rememberSaveable { mutableStateOf(Style.of(s.coldStyle).takeIf { it.kind == Kind.COLD } ?: Style.SHOWER) }
    var saunaStyle by rememberSaveable { mutableStateOf(Style.of(s.saunaStyle).takeIf { it.kind == Kind.SAUNA } ?: Style.FINNISH) }
    var target by rememberSaveable { mutableIntStateOf(s.coldTarget) }
    var waterC by rememberSaveable { mutableDoubleStateOf(s.waterC) }
    var knowWater by rememberSaveable { mutableStateOf(Style.of(s.coldStyle) != Style.SHOWER) }
    var saunaC by rememberSaveable { mutableDoubleStateOf(s.saunaC) }
    var breathe by rememberSaveable { mutableStateOf(s.breathe) }
    var shiver by rememberSaveable { mutableStateOf(s.shiver) }
    var fear by rememberSaveable { mutableIntStateOf(0) }
    var spot by rememberSaveable { mutableStateOf("") }
    var planKey by rememberSaveable { mutableStateOf(if (kind == Kind.SAUNA) s.saunaPlan else s.contrastPlan) }

    val plans: List<Plan> = remember(data.programs, kind) {
        (if (kind == Kind.SAUNA) Plans.sauna else Plans.contrast) + data.programs.filter { it.kind == kind.name }.map(Plans::of)
    }
    val plan = plans.firstOrNull { it.key == planKey } ?: plans.first()
    val spots = remember(data.sessions) { data.sessions.map { it.spot }.filter { it.isNotBlank() }.distinct().takeLast(6).reversed() }

    fun start() {
        val steps = when (kind) {
            Kind.COLD -> Plans.cold(target, breathe, shiver)
            else -> Plans.steps(plan)
        }
        val setup = Setup(
            kind = kind,
            style = when (kind) { Kind.COLD -> coldStyle.name; Kind.SAUNA -> saunaStyle.name; Kind.CONTRAST -> Style.CONTRAST.name },
            program = if (kind == Kind.COLD) "" else plan.key,
            steps = steps,
            waterC = if (kind == Kind.COLD && knowWater) waterC else null,
            airC = if (kind == Kind.SAUNA) saunaC else null,
            fear = fear.takeIf { it > 0 },
            spot = spot.trim(),
        )
        c.scope.launch {
            c.settings.update {
                when (kind) {
                    Kind.COLD -> it.copy(coldStyle = coldStyle.name, coldTarget = target, waterC = waterC, breathe = breathe, shiver = shiver)
                    Kind.SAUNA -> it.copy(saunaStyle = saunaStyle.name, saunaPlan = plan.key, saunaC = saunaC)
                    Kind.CONTRAST -> it.copy(contrastPlan = plan.key)
                }
            }
        }
        Engine.start(context, setup)
        go.back()
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(kindName(kind), onBack = { go.back() })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        ) {
            when (kind) {
                Kind.COLD -> {
                    SectionTitle(stringResource(R.string.setup_where))
                    ChipRow(Style.of(Kind.COLD), coldStyle, color) { coldStyle = it; if (it != Style.SHOWER) knowWater = true }
                    if (coldStyle == Style.OPEN || coldStyle == Style.ICE) SpotField(spot, spots) { spot = it }

                    SectionTitle(stringResource(R.string.setup_water))
                    UCard {
                        SwitchRow(stringResource(R.string.setup_know_water), null, knowWater, { knowWater = it }, color)
                        if (knowWater) Stepper(
                            stringResource(R.string.setup_water_temp), temp(waterC, s.unit),
                            onMinus = { waterC = (waterC - 1).coerceAtLeast(0.0) }, onPlus = { waterC = (waterC + 1).coerceAtMost(30.0) }, color = color,
                        )
                    }

                    SectionTitle(stringResource(R.string.setup_target))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Plans.coldTargets.forEach { t -> Chip(Words.clock(t), t == target, { target = t }, color) }
                    }
                    Spacer(Modifier.height(8.dp))
                    val limit = if (knowWater) Plans.coldLimit(waterC) else null
                    if (limit != null && target > limit) {
                        Note(Icons.Rounded.WarningAmber, stringResource(R.string.setup_limit_warn, Words.clock(limit)), U.c.danger)
                    } else {
                        Note(Icons.Rounded.Info, stringResource(R.string.setup_target_note), U.c.muted)
                    }

                    SectionTitle(stringResource(R.string.setup_ritual))
                    UCard {
                        SwitchRow(stringResource(R.string.setup_breathe), stringResource(R.string.setup_breathe_sub), breathe, { breathe = it }, color)
                        SwitchRow(stringResource(R.string.setup_shiver), stringResource(R.string.setup_shiver_sub), shiver, { shiver = it }, color)
                    }
                }
                Kind.SAUNA -> {
                    UCard(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Rounded.Info, null, tint = color, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.setup_sauna_phone), style = UType.Muted.copy(color = U.c.ink))
                        }
                    }
                    SectionTitle(stringResource(R.string.setup_where))
                    ChipRow(Style.of(Kind.SAUNA), saunaStyle, color) { saunaStyle = it }
                    Spacer(Modifier.height(4.dp))
                    UCard {
                        Stepper(
                            stringResource(R.string.setup_sauna_temp), temp(saunaC, s.unit),
                            onMinus = { saunaC = (saunaC - 5).coerceAtLeast(40.0) }, onPlus = { saunaC = (saunaC + 5).coerceAtMost(120.0) }, color = color,
                        )
                    }
                    PlanList(plans, plan, color, kind, go) { planKey = it.key }
                }
                Kind.CONTRAST -> {
                    Note(Icons.Rounded.Info, stringResource(R.string.setup_contrast_note), U.c.muted)
                    PlanList(plans, plan, color, kind, go) { planKey = it.key }
                }
            }

            if (s.askFear) {
                SectionTitle(stringResource(R.string.setup_fear))
                ScalePicker(fear, { fear = if (fear == it) 0 else it }, { fearFace(it) }, color = color)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.setup_fear_note), style = UType.Small)
            }
            Spacer(Modifier.height(24.dp))
        }
        PrimaryButton(
            stringResource(if (kind == Kind.SAUNA) R.string.start_sauna else R.string.start), ::start,
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp).navigationBarsPadding(),
            color = color, icon = Icons.Rounded.PlayArrow,
        )
    }
}

@Composable
private fun ChipRow(styles: List<Style>, selected: Style, color: Color, onPick: (Style) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        styles.forEach { st -> Chip(styleName(st), st == selected, { onPick(st) }, color) }
    }
}

@Composable
private fun SpotField(spot: String, spots: List<String>, onChange: (String) -> Unit) {
    Spacer(Modifier.height(10.dp))
    UTextField(spot, onChange, placeholder = stringResource(R.string.setup_spot))
    if (spots.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            spots.forEach { Chip(it, it == spot, { onChange(it) }, U.c.ice) }
        }
    }
}

@Composable
private fun PlanList(plans: List<Plan>, plan: Plan, color: Color, kind: Kind, go: Go, onPick: (Plan) -> Unit) {
    SectionTitle(stringResource(R.string.setup_program))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        plans.forEach { p ->
            SelectCard(p.key == plan.key, { onPick(p) }, color, trailing = {
                if (p.custom) IconButton(onClick = { go.program(kind, p.key.removePrefix("p").toLongOrNull() ?: 0) }) {
                    Icon(Icons.Rounded.Edit, stringResource(R.string.edit), tint = U.c.muted)
                }
            }) {
                Column {
                    Text(planName(p), style = UType.Strong)
                    Text(planShape(p) + " · " + stringResource(R.string.about_min, (p.totalSec + 59) / 60), style = UType.Muted)
                    val note = planNote(p)
                    if (note.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(note, style = UType.Small.copy(color = U.c.muted))
                    }
                }
            }
        }
        SelectCard(false, { go.program(kind) }, color) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Add, null, tint = color)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.program_new), style = UType.Strong.copy(color = color))
            }
        }
    }
}

@Composable
private fun Note(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = UType.Muted.copy(color = if (color == U.c.danger) color else U.c.muted))
    }
}
