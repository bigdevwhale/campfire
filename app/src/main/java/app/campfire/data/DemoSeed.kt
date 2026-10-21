package app.campfire.data

import app.campfire.BuildConfig
import app.campfire.domain.QUESTIONS
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

/**
 * The demo build starts with a family at the stones and a box with evenings in it, including one
 * exactly a year ago, so the fire has something to remember on a screenshot.
 */
object DemoSeed {
    suspend fun seedIfEmpty(db: CampfireDatabase, settings: SettingsStore, ru: Boolean) {
        if (!BuildConfig.DEMO) return
        if (db.dao().seats().first().isNotEmpty()) return
        settings.seeWelcome()

        fun t(r: String, e: String) = if (ru) r else e
        val mum = t("Мама", "Mum")
        val dad = t("Папа", "Dad")
        val kid = t("Дочка", "Mia")
        db.dao().addSeat(Seat(name = mum, glyph = 3))
        db.dao().addSeat(Seat(name = dad, glyph = 1))
        db.dao().addSeat(Seat(name = kid, glyph = 0))

        fun q(stage: Int, index: Int) = QUESTIONS.first { it.stage.ordinal == stage && (it.id - 1) % 100 == index }.text(ru)
        fun at(daysAgo: Long) =
            LocalDate.now().minusDays(daysAgo).atTime(21, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val evenings = listOf(
            Evening(
                endedAt = at(365), seatNames = Evening.join(listOf(mum, kid)),
                seatGlyphs = Evening.glyphIds(listOf(3, 0)),
                questions = Evening.join(listOf(q(0, 0), q(1, 1), q(2, 0))),
                word = t("смешно", "silly"), minutes = 14,
            ),
            Evening(
                endedAt = at(180), seatNames = Evening.join(listOf(dad, kid)),
                seatGlyphs = Evening.glyphIds(listOf(1, 0)),
                questions = Evening.join(listOf(q(0, 4), q(1, 11), q(2, 1))),
                word = t("маяк", "lighthouse"), minutes = 11,
            ),
            Evening(
                endedAt = at(40), seatNames = Evening.join(listOf(mum, dad, kid)),
                seatGlyphs = Evening.glyphIds(listOf(3, 1, 0)),
                questions = Evening.join(listOf(q(0, 8), q(1, 9), q(2, 15))),
                word = t("гром", "thunder"), minutes = 17,
            ),
            Evening(
                endedAt = at(9), seatNames = Evening.join(listOf(mum, kid)),
                seatGlyphs = Evening.glyphIds(listOf(3, 0)),
                questions = Evening.join(listOf(q(0, 12), q(1, 15), q(2, 7))),
                word = t("морошка", "cloudberry"), minutes = 12,
            ),
            Evening(
                endedAt = at(2), seatNames = Evening.join(listOf(dad, kid)),
                seatGlyphs = Evening.glyphIds(listOf(1, 0)),
                questions = Evening.join(listOf(q(0, 9), q(1, 0), q(2, 9))),
                word = t("тепло", "warm"), minutes = 13,
            ),
        )
        evenings.forEach { db.dao().addEvening(it) }
    }
}
