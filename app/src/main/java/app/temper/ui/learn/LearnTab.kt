package app.temper.ui.learn

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.temper.R
import app.temper.content.Article
import app.temper.ui.Go
import app.temper.ui.components.Chip
import app.temper.ui.components.IconDisc
import app.temper.ui.components.SectionTitle
import app.temper.ui.components.UCard
import app.temper.ui.content
import app.temper.ui.topicColor
import app.temper.ui.topicIcon
import app.temper.ui.theme.U
import app.temper.ui.theme.UType

private val topics = listOf("", "cold", "contrast", "sauna", "science", "safety")

@Composable
fun LearnTab(go: Go) {
    val text = content()
    var topic by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(bottom = 24.dp)) {
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.tab_learn), style = UType.Display, modifier = Modifier.padding(horizontal = 18.dp))
        Text(stringResource(R.string.learn_sub), style = UType.Sub, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 18.dp)) {
            UCard(onClick = { go.article("safety") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconDisc(Icons.Rounded.HealthAndSafety, 44.dp, U.c.danger, U.c.danger.copy(alpha = 0.14f))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.safety_title), style = UType.Strong)
                        Text(stringResource(R.string.safety_sub), style = UType.Muted)
                    }
                }
            }
        }
        SectionTitle(stringResource(R.string.articles), Modifier.padding(horizontal = 18.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            topics.forEach { t ->
                Chip(
                    stringResource(
                        when (t) {
                            "cold" -> R.string.topic_cold
                            "contrast" -> R.string.topic_contrast
                            "sauna" -> R.string.topic_sauna
                            "science" -> R.string.topic_science
                            "safety" -> R.string.topic_safety
                            else -> R.string.topic_all
                        }
                    ),
                    t == topic, { topic = t }, if (t.isEmpty()) U.c.accent else topicColor(t).first,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 18.dp)) {
            text.articles.filter { topic.isEmpty() || it.topic == topic }.forEach { a -> ArticleCard(a) { go.article(a.id) } }
        }
    }
}

@Composable
fun ArticleCard(a: Article, onClick: () -> Unit) {
    val (color, soft) = topicColor(a.topic)
    UCard(Modifier.padding(bottom = 10.dp), onClick = onClick) {
        Row {
            IconDisc(topicIcon(a.topic), 40.dp, color, soft)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(a.title, style = UType.Strong)
                Spacer(Modifier.height(2.dp))
                Text(a.lead, style = UType.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.min_read, a.minutes), style = UType.Small)
            }
        }
    }
}
