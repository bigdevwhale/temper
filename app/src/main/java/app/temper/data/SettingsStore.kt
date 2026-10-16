package app.temper.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class TempUnit { C, F }

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val onboarded: Boolean = false,
    val name: String = "",
    /** Weekly minutes in cold water; 11 after Søberg. */
    val coldGoal: Int = 11,
    /** Weekly minutes in heat; 57 after Søberg. */
    val heatGoal: Int = 57,
    val unit: TempUnit = TempUnit.C,
    /** Spoken cues during a session. */
    val voice: Boolean = true,
    /** Anvil, hiss and countdown tones. */
    val tones: Boolean = true,
    val vibrate: Boolean = true,
    /** A minute of calm breathing before cold water. */
    val breathe: Boolean = true,
    /** A few minutes of warming up on your own after cold water. */
    val shiver: Boolean = true,
    /** Ask how hard it is to begin. */
    val askFear: Boolean = true,
    /** Last choices, so the next session starts the same way. */
    val coldStyle: String = Style.SHOWER.name,
    val coldTarget: Int = 60,
    val waterC: Double = 12.0,
    val contrastPlan: String = "classic",
    val saunaPlan: String = "finnish",
    val saunaStyle: String = Style.FINNISH.name,
    val saunaC: Double = 85.0,
    /** Daily reminder, minutes after midnight; -1 when off. */
    val remindAt: Int = -1,
    /** Active trial id and the epoch day it began; empty when none. */
    val trial: String = "",
    val trialStart: Long = 0,
    /** Trials completed (ids), kept for marks. */
    val trialsDone: Set<String> = emptySet(),
    val seenBadges: Set<String> = emptySet(),
    val levelSeen: Int = 1,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        theme = this[THEME]?.let { t -> runCatching { ThemeMode.valueOf(t) }.getOrNull() } ?: ThemeMode.SYSTEM,
        onboarded = this[ONBOARDED] ?: false,
        name = this[NAME] ?: "",
        coldGoal = this[COLD_GOAL] ?: 11,
        heatGoal = this[HEAT_GOAL] ?: 57,
        unit = this[UNIT]?.let { u -> runCatching { TempUnit.valueOf(u) }.getOrNull() } ?: TempUnit.C,
        voice = this[VOICE] ?: true,
        tones = this[TONES] ?: true,
        vibrate = this[VIBRATE] ?: true,
        breathe = this[BREATHE] ?: true,
        shiver = this[SHIVER] ?: true,
        askFear = this[ASK_FEAR] ?: true,
        coldStyle = this[COLD_STYLE] ?: Style.SHOWER.name,
        coldTarget = this[COLD_TARGET] ?: 60,
        waterC = this[WATER_C] ?: 12.0,
        contrastPlan = this[CONTRAST_PLAN] ?: "classic",
        saunaPlan = this[SAUNA_PLAN] ?: "finnish",
        saunaStyle = this[SAUNA_STYLE] ?: Style.FINNISH.name,
        saunaC = this[SAUNA_C] ?: 85.0,
        remindAt = this[REMIND_AT] ?: -1,
        trial = this[TRIAL] ?: "",
        trialStart = this[TRIAL_START] ?: 0,
        trialsDone = this[TRIALS_DONE] ?: emptySet(),
        seenBadges = this[SEEN_BADGES] ?: emptySet(),
        levelSeen = this[LEVEL_SEEN] ?: 1,
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[THEME] = s.theme.name
            it[ONBOARDED] = s.onboarded
            it[NAME] = s.name
            it[COLD_GOAL] = s.coldGoal
            it[HEAT_GOAL] = s.heatGoal
            it[UNIT] = s.unit.name
            it[VOICE] = s.voice
            it[TONES] = s.tones
            it[VIBRATE] = s.vibrate
            it[BREATHE] = s.breathe
            it[SHIVER] = s.shiver
            it[ASK_FEAR] = s.askFear
            it[COLD_STYLE] = s.coldStyle
            it[COLD_TARGET] = s.coldTarget
            it[WATER_C] = s.waterC
            it[CONTRAST_PLAN] = s.contrastPlan
            it[SAUNA_PLAN] = s.saunaPlan
            it[SAUNA_STYLE] = s.saunaStyle
            it[SAUNA_C] = s.saunaC
            it[REMIND_AT] = s.remindAt
            it[TRIAL] = s.trial
            it[TRIAL_START] = s.trialStart
            it[TRIALS_DONE] = s.trialsDone
            it[SEEN_BADGES] = s.seenBadges
            it[LEVEL_SEEN] = s.levelSeen
        }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val NAME = stringPreferencesKey("name")
        val COLD_GOAL = intPreferencesKey("cold_goal")
        val HEAT_GOAL = intPreferencesKey("heat_goal")
        val UNIT = stringPreferencesKey("unit")
        val VOICE = booleanPreferencesKey("voice")
        val TONES = booleanPreferencesKey("tones")
        val VIBRATE = booleanPreferencesKey("vibrate")
        val BREATHE = booleanPreferencesKey("breathe")
        val SHIVER = booleanPreferencesKey("shiver")
        val ASK_FEAR = booleanPreferencesKey("ask_fear")
        val COLD_STYLE = stringPreferencesKey("cold_style")
        val COLD_TARGET = intPreferencesKey("cold_target")
        val WATER_C = doublePreferencesKey("water_c")
        val CONTRAST_PLAN = stringPreferencesKey("contrast_plan")
        val SAUNA_PLAN = stringPreferencesKey("sauna_plan")
        val SAUNA_STYLE = stringPreferencesKey("sauna_style")
        val SAUNA_C = doublePreferencesKey("sauna_c")
        val REMIND_AT = intPreferencesKey("remind_at")
        val TRIAL = stringPreferencesKey("trial")
        val TRIAL_START = longPreferencesKey("trial_start")
        val TRIALS_DONE = stringSetPreferencesKey("trials_done")
        val SEEN_BADGES = stringSetPreferencesKey("seen_badges")
        val LEVEL_SEEN = intPreferencesKey("level_seen")
    }
}
