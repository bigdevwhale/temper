package app.temper.data

import org.json.JSONArray
import org.json.JSONObject

/** Plain JSON backup: everything stays on the device unless the user exports it. */
object Backup {
    private const val VERSION = 1

    fun encode(s: Snapshot): String = JSONObject()
        .put("app", "temper")
        .put("version", VERSION)
        .put("sessions", JSONArray(s.sessions.map {
            JSONObject().put("id", it.id).put("style", it.style).put("start", it.start).put("end", it.end)
                .put("coldSec", it.coldSec).put("heatSec", it.heatSec).put("rounds", it.rounds)
                .put("waterC", it.waterC ?: JSONObject.NULL).put("airC", it.airC ?: JSONObject.NULL)
                .put("fear", it.fear ?: JSONObject.NULL).put("energy", it.energy ?: JSONObject.NULL).put("mood", it.mood ?: JSONObject.NULL)
                .put("note", it.note).put("program", it.program).put("endedCold", it.endedCold).put("spot", it.spot)
        }))
        .put("programs", JSONArray(s.programs.map {
            JSONObject().put("id", it.id).put("kind", it.kind).put("name", it.name).put("heatSec", it.heatSec)
                .put("coldSec", it.coldSec).put("restSec", it.restSec).put("rounds", it.rounds).put("endCold", it.endCold)
        }))
        .toString(2)

    /** Throws [IllegalArgumentException] when the text is not a Temper backup. */
    fun decode(text: String): Snapshot {
        val root = runCatching { JSONObject(text) }.getOrNull()
        require(root != null && root.optString("app") == "temper") { "Not a Temper backup" }
        return Snapshot(
            sessions = root.list("sessions") {
                Session(
                    id = getLong("id"), style = getString("style"), start = getLong("start"), end = getLong("end"),
                    coldSec = getInt("coldSec"), heatSec = getInt("heatSec"), rounds = optInt("rounds", 1),
                    waterC = optD("waterC"), airC = optD("airC"),
                    fear = optI("fear"), energy = optI("energy"), mood = optI("mood"),
                    note = optString("note"), program = optString("program"), endedCold = optBoolean("endedCold"), spot = optString("spot"),
                )
            },
            programs = root.list("programs") {
                Program(getLong("id"), getString("kind"), getString("name"), getInt("heatSec"), getInt("coldSec"), getInt("restSec"), getInt("rounds"), optBoolean("endCold", true))
            },
        )
    }

    private fun JSONObject.optD(key: String): Double? = if (!has(key) || isNull(key)) null else getDouble(key)
    private fun JSONObject.optI(key: String): Int? = if (!has(key) || isNull(key)) null else getInt(key)

    private fun <T> JSONObject.list(key: String, read: JSONObject.() -> T): List<T> {
        val arr = optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).map { arr.getJSONObject(it).read() }
    }
}
