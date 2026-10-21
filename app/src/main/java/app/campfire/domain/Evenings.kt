package app.campfire.domain

import java.time.LocalDate
import kotlin.math.abs

/**
 * How much wood goes on the fire: more hands, a little longer. Two people get six logs —
 * two playful sparks, three closer ones, one sleepy — about a quarter of an hour of talk.
 */
fun logsFor(people: Int): Int = (4 + people).coerceIn(6, 9)

/** After the logs are spent, the embers can still be asked this many times. */
const val EMBER_BREATHS = 3

/**
 * The evening the fire remembers today: one from about a year ago (within a few days either
 * side, so a missed anniversary still comes back), the closest to the day.
 */
fun <T> yearAgo(evenings: List<T>, today: LocalDate, dateOf: (T) -> LocalDate): T? {
    val target = today.minusYears(1)
    return evenings
        .map { it to abs(dateOf(it).toEpochDay() - target.toEpochDay()) }
        .filter { it.second <= 3 }
        .minByOrNull { it.second }
        ?.first
}
