package app.temper.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** What was practised: cold water, a contrast shower, or a sauna with its cool-downs. */
enum class Kind { COLD, CONTRAST, SAUNA }

/** Where or how. Cold: shower, bath, open water, ice hole. Sauna: Finnish, banya, hammam, infrared. */
enum class Style(val kind: Kind) {
    SHOWER(Kind.COLD), BATH(Kind.COLD), OPEN(Kind.COLD), ICE(Kind.COLD),
    CONTRAST(Kind.CONTRAST),
    FINNISH(Kind.SAUNA), BANYA(Kind.SAUNA), HAMMAM(Kind.SAUNA), INFRARED(Kind.SAUNA);

    companion object {
        fun of(name: String): Style = entries.firstOrNull { it.name == name } ?: SHOWER
        fun of(kind: Kind): List<Style> = entries.filter { it.kind == kind }
    }
}

/** One finished session: a layer of the blade. */
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [Style] name; the kind follows from it. */
    val style: String,
    val start: Long,
    val end: Long,
    /** Seconds spent in cold water. */
    val coldSec: Int,
    /** Seconds spent in heat: the hot part of a contrast shower or the sauna itself. */
    val heatSec: Int,
    val rounds: Int = 1,
    /** Water temperature, °C. */
    val waterC: Double? = null,
    /** Sauna or outside air temperature, °C. */
    val airC: Double? = null,
    /** How hard it was to begin, 1 (easy) .. 5 (I really didn't want to). */
    val fear: Int? = null,
    /** Asked when it ends, 1..5. */
    val energy: Int? = null,
    val mood: Int? = null,
    val note: String = "",
    /** Program key from [app.temper.domain.Plans], empty when logged by hand. */
    val program: String = "",
    /** Finished on cold (the Søberg principle). */
    val endedCold: Boolean = false,
    /** A lake, a beach, a bathhouse; free text. */
    val spot: String = "",
) {
    val kind: Kind get() = Style.of(style).kind
    val totalSec: Int get() = ((end - start) / 1000).toInt().coerceAtLeast(coldSec + heatSec)
}

/** A contrast or sauna program the user made. */
@Entity(tableName = "programs")
data class Program(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [Kind] name: CONTRAST or SAUNA. */
    val kind: String,
    val name: String,
    val heatSec: Int,
    val coldSec: Int,
    val restSec: Int,
    val rounds: Int,
    val endCold: Boolean = true,
)

class Snapshot(val sessions: List<Session>, val programs: List<Program>)

@Dao
abstract class TemperDao {
    @Query("SELECT * FROM sessions ORDER BY start")
    abstract fun observeSessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions ORDER BY start")
    abstract suspend fun sessions(): List<Session>

    @Query("SELECT * FROM sessions WHERE id = :id")
    abstract suspend fun session(id: Long): Session?

    @Insert
    abstract suspend fun insertSession(s: Session): Long

    @Update
    abstract suspend fun updateSession(s: Session)

    @Delete
    abstract suspend fun deleteSession(s: Session)

    @Query("SELECT * FROM programs ORDER BY id")
    abstract fun observePrograms(): Flow<List<Program>>

    @Query("SELECT * FROM programs ORDER BY id")
    abstract suspend fun programs(): List<Program>

    @Query("SELECT * FROM programs WHERE id = :id")
    abstract suspend fun program(id: Long): Program?

    @Insert
    abstract suspend fun insertProgram(p: Program): Long

    @Update
    abstract suspend fun updateProgram(p: Program)

    @Delete
    abstract suspend fun deleteProgram(p: Program)

    @Transaction
    open suspend fun snapshot() = Snapshot(sessions(), programs())

    @Query("DELETE FROM sessions") protected abstract suspend fun clearSessions()
    @Query("DELETE FROM programs") protected abstract suspend fun clearPrograms()
    @Insert protected abstract suspend fun insertSessions(l: List<Session>)
    @Insert protected abstract suspend fun insertPrograms(l: List<Program>)

    @Transaction
    open suspend fun replaceAll(s: Snapshot) {
        clearSessions(); clearPrograms()
        insertSessions(s.sessions); insertPrograms(s.programs)
    }
}

@Database(entities = [Session::class, Program::class], version = 1, exportSchema = true)
abstract class TemperDatabase : RoomDatabase() {
    abstract fun dao(): TemperDao

    companion object {
        fun create(context: Context): TemperDatabase =
            Room.databaseBuilder(context.applicationContext, TemperDatabase::class.java, "temper.db").build()
    }
}
