package app.campfire.data

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
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A stone by the fire with a name and a silhouette. Glyph 0..5 picks the drawn head. */
@Entity(tableName = "seats")
data class Seat(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val glyph: Int,
) {
    companion object {
        /** The nameless stone that is always by the fire for whoever drops in. */
        const val GUEST_ID = -1L
        const val GLYPHS = 6
    }
}

/**
 * One evening by the fire: who sat (names and glyphs as they were that night — a snapshot, so
 * renamed or removed stones don't rewrite history), the questions the sparks brought, the word
 * written in the ash, and the cassette file name if one was laid by the fire.
 */
@Entity(tableName = "evenings")
data class Evening(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val endedAt: Long,
    val seatNames: String,
    val seatGlyphs: String,
    val questions: String,
    val word: String,
    val recording: String? = null,
    val minutes: Int,
) {
    val names: List<String> get() = seatNames.split(SEP).filter { it.isNotBlank() }
    val glyphs: List<Int> get() = seatGlyphs.split(SEP).filter { it.isNotBlank() }.map { it.toInt() }
    val questionList: List<String> get() = questions.split(SEP).filter { it.isNotBlank() }
    val day: LocalDate get() = Instant.ofEpochMilli(endedAt).atZone(ZoneId.systemDefault()).toLocalDate()

    companion object {
        const val SEP = "\u001F"
        fun join(items: List<String>) = items.joinToString(SEP)
        fun glyphIds(glyphs: List<Int>) = glyphs.joinToString(SEP) { it.toString() }
    }
}

@Dao
interface CampfireDao {
    @Query("SELECT * FROM seats ORDER BY id")
    fun seats(): Flow<List<Seat>>

    @Insert
    suspend fun addSeat(seat: Seat): Long

    @Update
    suspend fun updateSeat(seat: Seat)

    @Delete
    suspend fun removeSeat(seat: Seat)

    @Query("SELECT * FROM evenings ORDER BY endedAt DESC")
    fun evenings(): Flow<List<Evening>>

    @Insert
    suspend fun addEvening(evening: Evening): Long

    @Delete
    suspend fun removeEvening(evening: Evening)
}

@Database(entities = [Seat::class, Evening::class], version = 1, exportSchema = true)
abstract class CampfireDatabase : RoomDatabase() {
    abstract fun dao(): CampfireDao

    companion object {
        fun create(context: Context) = Room.databaseBuilder(context, CampfireDatabase::class.java, "campfire.db").build()
    }
}
