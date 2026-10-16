package app.temper.content

import java.util.Locale

/**
 * A short read. Each block is a paragraph; "## " starts a heading, "• " a bullet,
 * "> " a highlighted note.
 */
data class Article(
    val id: String,
    /** cold, contrast, sauna, safety or science; picks the colour and the glyph. */
    val topic: String,
    val title: String,
    val lead: String,
    val minutes: Int,
    val blocks: List<String>,
)

class Content(
    val articles: List<Article>,
    /** One is shown per day on the forge. */
    val tips: List<String>,
    /** Said on the finish screen, one per session. */
    val cheers: List<String>,
    /** When not to practise; shown in onboarding and on the safety page. */
    val warnings: List<String>,
) {
    fun article(id: String): Article? = articles.firstOrNull { it.id == id }

    fun tip(day: Long): String = tips[Math.floorMod(day, tips.size.toLong()).toInt()]

    fun cheer(seed: Long): String = cheers[Math.floorMod(seed, cheers.size.toLong()).toInt()]

    companion object {
        fun of(locale: Locale): Content = if (locale.language == "ru") ContentRu else ContentEn
    }
}
