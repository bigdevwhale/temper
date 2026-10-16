package app.temper.ui.blade

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import app.temper.R
import app.temper.data.Kind
import app.temper.domain.Badges
import app.temper.domain.Levels
import app.temper.run.Words
import app.temper.ui.Go
import app.temper.ui.TemperData
import app.temper.ui.badgeColor
import app.temper.ui.badgeIcon
import app.temper.ui.badgeText
import app.temper.ui.badgeTitle
import app.temper.ui.components.Blade
import app.temper.ui.components.CelebrateDialog
import app.temper.ui.components.IconDisc
import app.temper.ui.components.ScoreLine
import app.temper.ui.components.SectionTitle
import app.temper.ui.components.SoftButton
import app.temper.ui.components.Stat
import app.temper.ui.components.UCard
import app.temper.ui.components.layersOf
import app.temper.ui.daysCount
import app.temper.ui.glowText
import app.temper.ui.kindColor
import app.temper.ui.levelName
import app.temper.ui.locale
import app.temper.ui.minutesText
import app.temper.ui.temp
import app.temper.ui.theme.TemperColors
import app.temper.ui.theme.U
import app.temper.ui.theme.UType
import app.temper.ui.theme.temperColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters

@Composable
fun BladeTab(data: TemperData, go: Go) {
    val context = LocalContext.current
    val st = data.stats
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    var shownBadge by remember { mutableStateOf<String?>(null) }
    val shareTitle = stringResource(R.string.share_blade)
    val shareText = stringResource(R.string.share_text, levelName(data.level), st.count)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.blade_title), style = UType.Display, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                scope.launch {
                    val bmp = layer.toImageBitmap().asAndroidBitmap()
                    share(context, bmp, shareTitle, shareText)
                }
            }) { Icon(Icons.Rounded.Share, shareTitle, tint = U.c.ink) }
        }
        Spacer(Modifier.height(12.dp))

        // Drawn into a layer so it can be shared as a picture.
        Column(
            Modifier
                .drawWithContent { layer.record { this@drawWithContent.drawContent() }; drawLayer(layer) }
                .clip(U.RLg)
                .background(U.c.surface)
                .border(1.dp, U.c.line, U.RLg)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Blade(layersOf(data.sessions), data.level, st.glow, st.streak, Modifier.fillMaxWidth().aspectRatio(2.6f))
            Spacer(Modifier.height(10.dp))
            Text(levelName(data.level), style = UType.Title.copy(color = temperColor(data.level).let { if (data.level == 2) U.c.gold else it }))
            Text(
                stringResource(R.string.blade_layers, st.count) + " · " + glowText(st.glow) + (if (data.settings.name.isNotBlank()) " · " + data.settings.name else ""),
                style = UType.Muted, textAlign = TextAlign.Center,
            )
        }

        // The temper ladder.
        SectionTitle(stringResource(R.string.blade_temper))
        UCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TemperColors.forEachIndexed { i, col ->
                    val reached = i < data.level
                    Box(
                        Modifier.size(if (i == data.level - 1) 30.dp else 24.dp).clip(CircleShape).background(col).alpha(if (reached) 1f else 0.25f)
                            .border(if (i == data.level - 1) 2.dp else 0.dp, U.c.ink, CircleShape),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            val next = Levels.next(data.level)
            Text(
                if (next == null) stringResource(R.string.blade_max) else stringResource(R.string.blade_next, levelName(data.level + 1), next - st.xp),
                style = UType.Muted,
            )
        }

        // Numbers.
        SectionTitle(stringResource(R.string.blade_numbers))
        UCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat("${st.count}", stringResource(R.string.stat_sessions), Modifier.weight(1f))
                Stat("${st.streak}", stringResource(R.string.stat_streak), Modifier.weight(1f), color = U.c.accent)
                Stat("${st.bestStreak}", stringResource(R.string.stat_best), Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat(minutesText(st.coldSec), stringResource(R.string.stat_cold), Modifier.weight(1f), color = U.c.ice)
                Stat(minutesText(st.heatSec), stringResource(R.string.stat_heat), Modifier.weight(1f), color = U.c.accent)
                Stat(if (st.longestCold > 0) Words.clock(st.longestCold) else "—", stringResource(R.string.stat_longest), Modifier.weight(1f))
            }
            st.coldestWater?.let {
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat(temp(it, data.settings.unit), stringResource(R.string.stat_coldest), Modifier.weight(1f), color = U.c.ice)
                    Stat("${st.rounds}", stringResource(R.string.stat_rounds), Modifier.weight(1f))
                    Stat("${st.fullWeeks}", stringResource(R.string.stat_weeks), Modifier.weight(1f))
                }
            }
        }

        // Fear going down.
        if (st.fears.size >= 3) {
            SectionTitle(stringResource(R.string.fear_title))
            UCard {
                ScoreLine(st.fears.takeLast(40), U.c.sage, Modifier.fillMaxWidth().height(110.dp))
                Spacer(Modifier.height(10.dp))
                val a = st.fearFirst
                val b = st.fearLast
                Text(
                    if (a != null && b != null) stringResource(R.string.fear_trend, "%.1f".format(locale(), a), "%.1f".format(locale(), b))
                    else stringResource(R.string.fear_soon),
                    style = UType.Muted,
                )
            }
        }

        // Five weeks of the forge.
        SectionTitle(stringResource(R.string.blade_calendar))
        UCard { Calendar(data) }

        // Marks.
        SectionTitle(stringResource(R.string.marks_title, data.badges.size, Badges.all.size))
        UCard {
            val got = data.badges.map { it.id }.toSet()
            Badges.all.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    row.forEach { id ->
                        val (color, soft) = badgeColor(id)
                        val has = id in got
                        Column(
                            Modifier.weight(1f).clip(U.RSm).clickable { shownBadge = id }.padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(Modifier.alpha(if (has) 1f else 0.3f)) { IconDisc(badgeIcon(id), 46.dp, if (has) color else U.c.faint, if (has) soft else U.c.surface2) }
                            Spacer(Modifier.height(4.dp))
                            Text(badgeTitle(id), style = UType.Small.copy(fontSize = 10.5.sp, textAlign = TextAlign.Center, color = if (has) U.c.muted else U.c.faint), maxLines = 2)
                        }
                    }
                }
            }
        }

        SectionTitle(stringResource(R.string.journal))
        UCard(onClick = { go.journal() }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.journal_open), style = UType.Strong)
                    Text(stringResource(R.string.journal_open_sub), style = UType.Muted)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
            }
        }
        Spacer(Modifier.height(10.dp))
        SoftButton(stringResource(R.string.session_add), { go.session(0) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
        Spacer(Modifier.height(24.dp))
    }

    shownBadge?.let { id ->
        val (color, soft) = badgeColor(id)
        val has = data.badges.any { it.id == id }
        CelebrateDialog(
            icon = badgeIcon(id), color = if (has) color else U.c.faint, soft = if (has) soft else U.c.surface2,
            eyebrow = stringResource(if (has) R.string.mark_have else R.string.mark_locked),
            title = badgeTitle(id), text = badgeText(id),
        ) { shownBadge = null }
    }
}

@Composable
private fun Calendar(data: TemperData) {
    val st = data.stats
    val monday = st.today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(4)
    Row(Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach {
            Text(it.getDisplayName(TextStyle.NARROW, locale()), style = UType.Small, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        }
    }
    Spacer(Modifier.height(6.dp))
    for (w in 0 until 5) {
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            for (d in 0 until 7) {
                val date = monday.plusDays((w * 7 + d).toLong())
                val kinds = st.on(date).map { it.kind }.distinct().sortedBy { it.ordinal }
                Box(Modifier.weight(1f).height(34.dp), contentAlignment = Alignment.Center) {
                    val future = date.isAfter(st.today)
                    Box(
                        Modifier.size(30.dp).clip(CircleShape)
                            .background(if (kinds.isEmpty()) U.c.surface2 else kindColor(kinds.first()).copy(alpha = 0.22f))
                            .then(if (date == st.today) Modifier.border(1.5.dp, U.c.ink, CircleShape) else Modifier)
                            .alpha(if (future) 0.35f else 1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (kinds.isEmpty()) Text("${date.dayOfMonth}", style = UType.Small)
                        else Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            kinds.forEach { k -> Box(Modifier.size(7.dp).clip(CircleShape).background(kindColor(k))) }
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Kind.entries.forEach { k ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(kindColor(k)))
                Spacer(Modifier.width(5.dp))
                Text(app.temper.ui.kindName(k), style = UType.Small)
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(stringResource(R.string.streak_rule, daysCount(st.streak)), style = UType.Small.copy(color = U.c.muted))
}

private suspend fun share(context: Context, bmp: Bitmap, title: String, text: String) {
    val uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "blade.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, context.packageName + ".files", file)
    }
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, text)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, title))
}
