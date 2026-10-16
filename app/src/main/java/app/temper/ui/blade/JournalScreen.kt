package app.temper.ui.blade

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.temper.R
import app.temper.ui.Go
import app.temper.ui.TemperData
import app.temper.ui.components.SectionTitle
import app.temper.ui.components.TopBar
import app.temper.ui.components.UCard
import app.temper.ui.dayLabel
import app.temper.ui.forge.SessionRow
import app.temper.ui.theme.U
import app.temper.ui.theme.UType

/** The smith's ledger: every session, newest first. */
@Composable
fun JournalScreen(data: TemperData, go: Go) {
    val st = data.stats
    val days = remember(data.sessions) { data.sessions.sortedByDescending { it.start }.groupBy { st.day(it) }.toList() }
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.journal), onBack = { go.back() }) {
            IconButton(onClick = { go.session(0) }) { Icon(Icons.Rounded.Add, stringResource(R.string.session_add), tint = U.c.ink) }
        }
        if (days.isEmpty()) {
            Text(stringResource(R.string.journal_empty), style = UType.Sub, modifier = Modifier.padding(24.dp))
            return@Column
        }
        LazyColumn(contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp)) {
            items(days, key = { it.first.toEpochDay() }) { (date, list) ->
                SectionTitle(dayLabel(date, st.today))
                UCard(padding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                    list.forEach { s ->
                        SessionRow(s) { go.session(s.id) }
                        if (s.note.isNotBlank()) {
                            Text("“${s.note}”", style = UType.Muted.copy(color = U.c.ink), modifier = Modifier.padding(start = 52.dp, bottom = 8.dp))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}
