package app.temper.ui.me

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.temper.BuildConfig
import app.temper.R
import app.temper.container
import app.temper.data.Backup
import app.temper.data.Kind
import app.temper.data.Settings
import app.temper.data.TempUnit
import app.temper.data.ThemeMode
import app.temper.notify.Alarms
import app.temper.run.Sound
import app.temper.run.Voice
import app.temper.ui.Go
import app.temper.ui.Language
import app.temper.ui.TemperData
import app.temper.ui.components.Chip
import app.temper.ui.components.CommunityLinks
import app.temper.ui.components.ConfirmDialog
import app.temper.ui.components.InfoRow
import app.temper.ui.components.SectionTitle
import app.temper.ui.components.SoftButton
import app.temper.ui.components.SwitchRow
import app.temper.ui.components.TimeDialog
import app.temper.ui.components.TopBar
import app.temper.ui.components.UCard
import app.temper.ui.components.UTextField
import app.temper.ui.kindColor
import app.temper.ui.kindName
import app.temper.ui.locale
import app.temper.ui.theme.U
import app.temper.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun MeTab(data: TemperData, go: Go) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Spacer(Modifier.height(18.dp))
        Text(data.settings.name.ifBlank { stringResource(R.string.tab_me) }, style = UType.Display)
        Insights(data)
        SettingsBody(data.settings, go)
    }
}

/** Separate screen with the same settings, for links from elsewhere. */
@Composable
fun SettingsScreen(settings: Settings, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.settings), onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
            SettingsBody(settings, null)
        }
    }
}

/** How sessions leave you feeling, by kind, once there are a few rated ones. */
@Composable
private fun Insights(data: TemperData) {
    val rows = Kind.entries.mapNotNull { k ->
        val rated = data.sessions.filter { it.kind == k && (it.energy != null || it.mood != null) }
        if (rated.size < 3) null else Triple(k, rated.mapNotNull { it.energy }.average(), rated.mapNotNull { it.mood }.average())
    }
    SectionTitle(stringResource(R.string.insights))
    UCard {
        if (rows.isEmpty()) {
            Text(stringResource(R.string.insights_soon), style = UType.Muted)
        } else rows.forEachIndexed { i, (k, e, m) ->
            if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = U.c.line)
            Row(Modifier.fillMaxWidth()) {
                Text(kindName(k), style = UType.Strong.copy(color = kindColor(k)), modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.insights_row, if (e.isNaN()) "—" else "%.1f".format(locale(), e), if (m.isNaN()) "—" else "%.1f".format(locale(), m)),
                    style = UType.Muted,
                )
            }
        }
        val morning = data.sessions.filter { it.energy != null && java.time.Instant.ofEpochMilli(it.start).atZone(data.stats.zone).hour < 12 }
        val later = data.sessions.filter { it.energy != null && java.time.Instant.ofEpochMilli(it.start).atZone(data.stats.zone).hour >= 12 }
        if (morning.size >= 3 && later.size >= 3) {
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.insights_time, "%.1f".format(locale(), morning.mapNotNull { it.energy }.average()), "%.1f".format(locale(), later.mapNotNull { it.energy }.average())),
                style = UType.Small.copy(color = U.c.muted),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsBody(settings: Settings, go: Go?) {
    val context = LocalContext.current
    val container = context.container
    fun update(change: (Settings) -> Settings) {
        container.scope.launch {
            container.settings.update(change)
            container.changed()
        }
    }
    var canNotify by remember { mutableStateOf(Alarms.canNotify(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { canNotify = Alarms.canNotify(context) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canNotify = it
        container.scope.launch { container.changed() }
    }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var pickTime by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(settings.name) }
    LaunchedEffect(name) {
        delay(400)
        if (name.trim() != settings.name) container.settings.update { it.copy(name = name.trim()) }
    }
    val l = locale()
    val voice = remember(l.language) { Voice(context, l) }
    DisposableEffect(voice) { onDispose { voice.shutdown() } }
    val testLine = stringResource(R.string.v_test)

    val exported = stringResource(R.string.backup_done)
    val failed = stringResource(R.string.backup_failed)
    val restored = stringResource(R.string.backup_restored)
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        container.scope.launch {
            val ok = runCatching {
                val json = Backup.encode(container.db.dao().snapshot())
                context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
            }.isSuccess
            withContext(Dispatchers.Main) { Toast.makeText(context, if (ok) exported else failed, Toast.LENGTH_SHORT).show() }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri }

    if (!canNotify) {
        Spacer(Modifier.height(12.dp))
        UCard(onClick = { if (Build.VERSION.SDK_INT >= 33) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
            Text(stringResource(R.string.perm_title), style = UType.Strong.copy(color = U.c.accent))
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.perm_text), style = UType.Muted)
        }
    }

    SectionTitle(stringResource(R.string.set_name))
    UTextField(name, { name = it.take(30) }, placeholder = stringResource(R.string.ob_name_hint))

    SectionTitle(stringResource(R.string.set_norm))
    UCard {
        Text(stringResource(R.string.norm_cold), style = UType.Strong.copy(color = U.c.ice))
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 5, 11, 20, 30).forEach { n -> Chip(if (n == 0) stringResource(R.string.off) else stringResource(R.string.min_short, n), settings.coldGoal == n, { update { it.copy(coldGoal = n) } }, U.c.ice) }
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.norm_heat), style = UType.Strong.copy(color = U.c.accent))
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 30, 57, 90, 120).forEach { n -> Chip(if (n == 0) stringResource(R.string.off) else stringResource(R.string.min_short, n), settings.heatGoal == n, { update { it.copy(heatGoal = n) } }, U.c.accent) }
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.set_norm_note), style = UType.Small.copy(color = U.c.muted))
        go?.let {
            TextButton(onClick = { it.article("soberg") }) { Text(stringResource(R.string.set_norm_why), color = U.c.accent, style = UType.Button) }
        }
    }

    SectionTitle(stringResource(R.string.set_session))
    UCard {
        SwitchRow(stringResource(R.string.set_voice), stringResource(R.string.set_voice_sub), settings.voice, { v -> update { it.copy(voice = v) } })
        SwitchRow(stringResource(R.string.set_tones), stringResource(R.string.set_tones_sub), settings.tones, { v -> update { it.copy(tones = v) } })
        SwitchRow(stringResource(R.string.set_vibrate), null, settings.vibrate, { v -> update { it.copy(vibrate = v) } })
        SwitchRow(stringResource(R.string.set_fear), stringResource(R.string.set_fear_sub), settings.askFear, { v -> update { it.copy(askFear = v) } })
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftButton(stringResource(R.string.set_test), {
                container.scope.launch {
                    Sound.play(Sound.Cue.ANVIL); delay(1200); Sound.play(Sound.Cue.HISS); delay(1300)
                    withContext(Dispatchers.Main) { voice.say(testLine) }
                }
            }, Modifier.weight(1f), icon = Icons.Rounded.RecordVoiceOver)
        }
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.set_voice_note), style = UType.Small.copy(color = U.c.muted))
    }

    SectionTitle(stringResource(R.string.set_remind))
    UCard {
        val t = settings.remindAt
        InfoRow(
            Icons.Rounded.Notifications, stringResource(R.string.set_remind_daily),
            if (t < 0) stringResource(R.string.off) else DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(l).format(LocalTime.of(t / 60, t % 60)),
        ) { pickTime = true }
        if (t >= 0) TextButton(onClick = { update { it.copy(remindAt = -1) } }) { Text(stringResource(R.string.set_remind_off), color = U.c.muted, style = UType.Button) }
        Text(stringResource(R.string.set_remind_note), style = UType.Small.copy(color = U.c.muted))
    }
    Spacer(Modifier.height(10.dp))
    UCard(onClick = {
        context.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
    }) {
        Text(stringResource(R.string.autostart_title), style = UType.Strong)
        Spacer(Modifier.height(2.dp))
        Text(stringResource(R.string.autostart_text), style = UType.Muted)
    }

    SectionTitle(stringResource(R.string.set_units))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("°C", settings.unit == TempUnit.C, { update { it.copy(unit = TempUnit.C) } })
        Chip("°F", settings.unit == TempUnit.F, { update { it.copy(unit = TempUnit.F) } })
    }

    SectionTitle(stringResource(R.string.set_appearance))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip(stringResource(R.string.theme_system), settings.theme == ThemeMode.SYSTEM, { update { it.copy(theme = ThemeMode.SYSTEM) } })
        Chip(stringResource(R.string.theme_light), settings.theme == ThemeMode.LIGHT, { update { it.copy(theme = ThemeMode.LIGHT) } })
        Chip(stringResource(R.string.theme_dark), settings.theme == ThemeMode.DARK, { update { it.copy(theme = ThemeMode.DARK) } })
    }

    SectionTitle(stringResource(R.string.language))
    val current = Language.current()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip(stringResource(R.string.lang_system), current == Language.SYSTEM, { Language.set(Language.SYSTEM) })
        Chip("English", current == Language.EN, { Language.set(Language.EN) })
        Chip("Русский", current == Language.RU, { Language.set(Language.RU) })
    }

    SectionTitle(stringResource(R.string.backup))
    UCard {
        Text(stringResource(R.string.backup_text), style = UType.Muted)
        Spacer(Modifier.height(8.dp))
        Row {
            TextButton(onClick = { export.launch("temper-${LocalDate.now()}.json") }) {
                Text(stringResource(R.string.backup_export), color = U.c.accent, style = UType.Button)
            }
            TextButton(onClick = { import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) {
                Text(stringResource(R.string.backup_import), color = U.c.accent, style = UType.Button)
            }
        }
    }

    CommunityLinks()

    SectionTitle(stringResource(R.string.about))
    UCard {
        Text(stringResource(R.string.about_text), style = UType.Sub)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.disclaimer), style = UType.Small)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = UType.Small)
    }

    if (pickTime) TimeDialog(if (settings.remindAt < 0) 7 * 60 else settings.remindAt, { pickTime = false }) { m -> update { it.copy(remindAt = m) } }
    pendingImport?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.backup_import_title),
            text = stringResource(R.string.backup_import_text),
            confirm = stringResource(R.string.backup_import),
            danger = true,
            onConfirm = {
                container.scope.launch {
                    val ok = runCatching {
                        val text = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                        container.db.dao().replaceAll(Backup.decode(text))
                        container.changed()
                    }.isSuccess
                    withContext(Dispatchers.Main) { Toast.makeText(context, if (ok) restored else failed, Toast.LENGTH_SHORT).show() }
                }
            },
            onDismiss = { pendingImport = null },
        )
    }
}
