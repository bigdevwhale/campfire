package app.campfire.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DomainTest {

    @Test
    fun `every stage has a full pool in both languages`() {
        Stage.entries.forEach { stage ->
            val pool = QUESTIONS.filter { it.stage == stage }
            assertEquals("pool for $stage", 16, pool.size)
            pool.forEach {
                assertTrue("ru blank: ${it.id}", it.ru.isNotBlank())
                assertTrue("en blank: ${it.id}", it.en.isNotBlank())
            }
        }
        assertEquals(QUESTIONS.size, QUESTIONS.map { it.id }.toSet().size)
        // 0 is how the settings say "no ember waiting".
        assertTrue(QUESTIONS.none { it.id == 0 })
    }

    @Test
    fun `a stage never repeats until its pool is spent, then starts over`() {
        val bag = QuestionBag()
        val first = List(16) { bag.draw(Stage.FLAME).id }
        assertEquals(16, first.toSet().size)
        assertTrue(first.all { it in 1..100 })
        // Spent: the next draw starts the pool over instead of failing.
        val again = bag.draw(Stage.FLAME)
        assertEquals(Stage.FLAME, again.stage)
    }

    @Test
    fun `stages keep their own pools`() {
        val bag = QuestionBag()
        repeat(16) { bag.draw(Stage.LOGS) }
        // Spending the logs' pool leaves the embers untouched.
        val embers = List(16) { bag.draw(Stage.EMBERS).id }
        assertEquals(16, embers.toSet().size)
    }

    @Test
    fun `the bag remembers across evenings`() {
        val bag = QuestionBag()
        val asked = List(10) { bag.draw(Stage.EMBERS).id }.toSet()
        val tomorrow = QuestionBag().apply { restore(bag.usedIds()) }
        val next = List(6) { tomorrow.draw(Stage.EMBERS).id }
        assertTrue(next.none { it in asked })
    }

    @Test
    fun `the fire burns down from flame to embers`() {
        assertEquals(Stage.FLAME, Stage.of(6))
        assertEquals(Stage.FLAME, Stage.of(5))
        assertEquals(Stage.LOGS, Stage.of(4))
        assertEquals(Stage.LOGS, Stage.of(2))
        assertEquals(Stage.EMBERS, Stage.of(1))
        assertEquals(Stage.EMBERS, Stage.of(0))
    }

    @Test
    fun `more people get a little more wood`() {
        assertEquals(6, logsFor(2))
        assertEquals(7, logsFor(3))
        assertEquals(9, logsFor(7))
    }

    @Test
    fun `the fire remembers about a year ago, the nearest day first`() {
        val today = LocalDate.of(2026, 10, 21)
        val days = listOf(LocalDate.of(2025, 10, 19), LocalDate.of(2025, 10, 22), LocalDate.of(2025, 9, 1), LocalDate.of(2026, 10, 1))
        assertEquals(LocalDate.of(2025, 10, 22), yearAgo(days, today) { it })
        assertNull(yearAgo(listOf(LocalDate.of(2025, 10, 10)), today) { it })
        assertNull(yearAgo(emptyList<LocalDate>(), today) { it })
    }
}
