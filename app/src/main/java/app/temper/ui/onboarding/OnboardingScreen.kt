package app.temper.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.temper.R
import app.temper.container
import app.temper.data.Kind
import app.temper.domain.Glow
import app.temper.ui.components.Blade
import app.temper.ui.components.IconDisc
import app.temper.ui.components.Layer
import app.temper.ui.components.PrimaryButton
import app.temper.ui.components.SelectCard
import app.temper.ui.components.UCard
import app.temper.ui.components.UTextField
import app.temper.ui.content
import app.temper.ui.kindColor
import app.temper.ui.kindIcon
import app.temper.ui.kindName
import app.temper.ui.kindSoft
import app.temper.ui.theme.U
import app.temper.ui.theme.UType
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val c = LocalContext.current.container
    var page by rememberSaveable { mutableIntStateOf(0) }
    var understood by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var norm by rememberSaveable { mutableIntStateOf(1) }
    val norms = listOf(5 to 30, 11 to 57, 11 to 0)

    fun finish() {
        val (cold, heat) = norms[norm]
        c.scope.launch {
            c.settings.update { it.copy(onboarded = true, name = name.trim(), coldGoal = cold, heatGoal = heat) }
            c.changed()
        }
        onDone()
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { i -> Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(if (i <= page) U.c.accent else U.c.line)) }
        }
        AnimatedContent(page, Modifier.weight(1f), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "page") { p ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(28.dp))
                when (p) {
                    0 -> Welcome()
                    1 -> Ways()
                    2 -> Safety(understood) { understood = it }
                    else -> Profile(name, { name = it.take(30) }, norm, { norm = it }, norms)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (page > 0) TextButton(onClick = { page-- }) { Text(stringResource(R.string.back), color = U.c.muted, style = UType.Button) }
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                stringResource(if (page == 3) R.string.ob_light else R.string.next),
                { if (page == 3) finish() else page++ },
                Modifier.width(200.dp),
                enabled = page != 2 || understood,
            )
        }
    }
}

@Composable
private fun Welcome() {
    val demo = remember {
        List(26) { Layer(it % 3 != 1, 0.4f + (it % 4) * 0.15f, it * 7919 + 13) }
    }
    Blade(demo, 3, Glow.HOT, 6, Modifier.fillMaxWidth().aspectRatio(2.6f))
    Spacer(Modifier.height(24.dp))
    Text(stringResource(R.string.ob_title), style = UType.Display)
    Spacer(Modifier.height(10.dp))
    Text(stringResource(R.string.ob_text), style = UType.Sub.copy(color = U.c.ink))
    Spacer(Modifier.height(14.dp))
    Text(stringResource(R.string.ob_text2), style = UType.Sub)
}

@Composable
private fun Ways() {
    Text(stringResource(R.string.ob_ways), style = UType.Title)
    Spacer(Modifier.height(16.dp))
    listOf(
        Kind.COLD to R.string.ob_cold,
        Kind.CONTRAST to R.string.ob_contrast,
        Kind.SAUNA to R.string.ob_sauna,
    ).forEach { (k, text) ->
        UCard(Modifier.padding(bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                IconDisc(kindIcon(k), 44.dp, kindColor(k), kindSoft(k))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(kindName(k), style = UType.Strong.copy(color = kindColor(k)))
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(text), style = UType.Muted)
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(stringResource(R.string.ob_free), style = UType.Small.copy(color = U.c.muted))
}

@Composable
private fun Safety(understood: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.WarningAmber, null, tint = U.c.danger, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.ob_safety), style = UType.Title)
    }
    Spacer(Modifier.height(10.dp))
    Text(stringResource(R.string.ob_safety_text), style = UType.Sub)
    Spacer(Modifier.height(12.dp))
    UCard {
        content().warnings.forEach { w ->
            Row(Modifier.padding(vertical = 4.dp)) {
                Text("•", style = UType.Body.copy(color = U.c.danger))
                Spacer(Modifier.width(10.dp))
                Text(w, style = UType.Body)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth().clip(U.RMd).clickable { onChange(!understood) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(understood, onChange, colors = CheckboxDefaults.colors(checkedColor = U.c.accent))
        Text(stringResource(R.string.ob_understood), style = UType.Body)
    }
}

@Composable
private fun Profile(name: String, onName: (String) -> Unit, norm: Int, onNorm: (Int) -> Unit, norms: List<Pair<Int, Int>>) {
    Text(stringResource(R.string.ob_profile), style = UType.Title)
    Spacer(Modifier.height(14.dp))
    UTextField(name, onName, placeholder = stringResource(R.string.ob_name_hint))
    Spacer(Modifier.height(20.dp))
    Text(stringResource(R.string.ob_norm), style = UType.Strong)
    Spacer(Modifier.height(4.dp))
    Text(stringResource(R.string.ob_norm_text), style = UType.Muted)
    Spacer(Modifier.height(10.dp))
    val labels = listOf(R.string.ob_norm_gentle, R.string.ob_norm_soberg, R.string.ob_norm_cold)
    norms.forEachIndexed { i, (cold, heat) ->
        SelectCard(norm == i, { onNorm(i) }, U.c.accent, Modifier.padding(bottom = 8.dp)) {
            Column {
                Text(stringResource(labels[i]), style = UType.Strong)
                Text(
                    stringResource(R.string.ob_norm_line, cold) + if (heat > 0) " · " + stringResource(R.string.ob_norm_heat, heat) else "",
                    style = UType.Muted,
                )
            }
        }
    }
}
