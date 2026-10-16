package app.temper.data

import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.random.Random

/** Made-up history for screenshots (`-Pdemo` builds only): ten weeks of a habit taking hold. */
object DemoSeed {
    suspend fun seedIfEmpty(db: TemperDatabase, settings: SettingsStore) {
        val dao = db.dao()
        if (dao.sessions().isNotEmpty()) return
        val ru = Locale.getDefault().language == "ru"
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val rnd = Random(42)
        val days = 70
        val list = mutableListOf<Session>()
        for (ago in days downTo 0) {
            val date = today.minusDays(ago.toLong())
            val progress = (days - ago) / days.toFloat()
            fun at(h: Int, m: Int) = date.atTime(h, m).atZone(zone).toInstant().toEpochMilli()
            val fear = (5 - progress * 4 + rnd.nextFloat() * 1.2f - 0.6f).toInt().coerceIn(1, 5)
            // A cold shower most mornings, longer as the weeks go by.
            if (ago == 0 || rnd.nextFloat() < 0.78f) {
                val start = at(7, 10 + rnd.nextInt(30))
                val cold = (30 + progress * 150 + rnd.nextInt(-10, 20)).toInt().coerceAtLeast(20)
                val style = if (ago in listOf(9, 23, 37)) Style.OPEN else if (rnd.nextFloat() < 0.15f) Style.BATH else Style.SHOWER
                list += Session(
                    style = style.name, start = start, end = start + (cold + 360) * 1000L, coldSec = cold, heatSec = 0,
                    waterC = when (style) { Style.OPEN -> 11.0; Style.BATH -> 9.0; else -> 13.0 },
                    fear = fear, energy = (3 + progress * 2 + rnd.nextFloat() - 0.5f).toInt().coerceIn(1, 5),
                    mood = (3 + progress * 1.6f + rnd.nextFloat() - 0.5f).toInt().coerceIn(1, 5), endedCold = true,
                    spot = if (style == Style.OPEN) (if (ru) "Озеро у дачи" else "Lake by the cabin") else "",
                    note = when (ago) { 37 -> if (ru) "Первый раз в озере. Дыхание сбилось, потом выровнялось" else "First time in the lake. Lost my breath, then found it"; 9 -> if (ru) "Вода 11°, а было спокойно" else "Water at 11°, and it felt calm"; else -> "" },
                )
            }
            // Contrast showers on some evenings.
            if (ago % 3 == 1 && ago > 2) {
                val start = at(20, 30)
                list += Session(
                    style = Style.CONTRAST.name, start = start, end = start + 8 * 60_000L, coldSec = 90 + (progress * 90).toInt(), heatSec = 270, rounds = 3,
                    program = "classic", endedCold = true, fear = (fear - 1).coerceAtLeast(1), energy = 4, mood = 4,
                )
            }
            // The banya on Saturdays.
            if (date.dayOfWeek == java.time.DayOfWeek.SATURDAY || (ago == 1 && date.dayOfWeek != java.time.DayOfWeek.SUNDAY)) {
                val start = at(17, 0)
                list += Session(
                    style = Style.BANYA.name, start = start, end = start + 75 * 60_000L, coldSec = 180, heatSec = 50 * 60, rounds = 3,
                    airC = 90.0, program = if (progress > 0.5f) "soberg" else "banya", endedCold = progress > 0.5f, energy = 4, mood = 5,
                    spot = if (ru) "Баня на Садовой" else "Garden Street banya",
                )
            }
        }
        list.forEach { dao.insertSession(it) }
        dao.insertProgram(Program(kind = Kind.CONTRAST.name, name = if (ru) "После зала" else "After the gym", heatSec = 120, coldSec = 45, restSec = 0, rounds = 4))
        settings.update {
            it.copy(
                onboarded = true, name = if (ru) "Саша" else "Alex", remindAt = 7 * 60, theme = ThemeMode.DARK,
                trial = "first_ice", trialStart = today.minusDays(19).toEpochDay(),
                seenBadges = app.temper.domain.Badges.all.toSet(),
                levelSeen = 8,
            )
        }
    }
}
