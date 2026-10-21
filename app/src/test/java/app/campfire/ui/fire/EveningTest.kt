package app.campfire.ui.fire

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import app.campfire.CampfireApp
import app.campfire.data.Seat
import app.campfire.domain.EMBER_BREATHS
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** A whole evening, start to finish, against the real database and settings. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EveningTest {
    private val app = ApplicationProvider.getApplicationContext<CampfireApp>()
    private val mum = Seat(id = 1, name = "Mum", glyph = 3)
    private val kid = Seat(id = 2, name = "Mia", glyph = 0)
    private val guest = Seat(id = Seat.GUEST_ID, name = "Guest", glyph = 5)

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun until(what: String, check: () -> Boolean) {
        repeat(200) {
            // Lets the main looper's clock run, so the smoke's delay passes too.
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            if (check()) return
            Thread.sleep(20)
        }
        throw AssertionError("never: $what")
    }

    private fun vm() = EveningVm(app.container).also { idle() }

    @Test
    fun `one pair of hands never lights it`() {
        val vm = vm()
        vm.hold(mum, true)
        vm.ignite(listOf(mum, kid, guest))
        assertEquals(Phase.COLD, vm.ui.value.phase)
    }

    @Test
    fun `the guest stone counts as a pair of hands`() {
        val vm = vm()
        vm.hold(mum, true)
        vm.hold(guest, true)
        vm.ignite(listOf(mum, kid, guest))
        assertEquals(Phase.BURNING, vm.ui.value.phase)
        assertEquals(listOf(mum, guest), vm.ui.value.present)
    }

    @Test
    fun `an evening burns down to ash, is covered, and leaves a ticket and an ember`() = runBlocking {
        val vm = vm()
        vm.hold(mum, true)
        vm.hold(kid, true)
        vm.ignite(listOf(mum, kid, guest))
        val logs = vm.ui.value.logs
        assertEquals(6, logs)

        repeat(logs) {
            vm.ask()
            assertNotNull(vm.ui.value.question)
            // A second tap while a question is in the air asks nothing.
            vm.ask()
            vm.letGo()
        }
        assertEquals(Phase.ASH, vm.ui.value.phase)

        // The embers can still be asked a few times, then they are quiet.
        repeat(EMBER_BREATHS) { vm.ask(); vm.letGo() }
        vm.ask()
        assertNull(vm.ui.value.question)

        vm.handBranch(kid)
        assertEquals(kid, vm.ui.value.branch)
        vm.ash("thunder")
        vm.cover()
        assertEquals(Phase.SMOKE, vm.ui.value.phase)

        until("a ticket in the box") { runBlocking { app.container.db.dao().evenings().first().isNotEmpty() } }
        val ticket = app.container.db.dao().evenings().first().first()
        assertEquals("thunder", ticket.word)
        assertEquals(listOf("Mum", "Mia"), ticket.names)
        assertEquals(logs + EMBER_BREATHS, ticket.questionList.size)

        until("an ember set aside") { runBlocking { app.container.settings.settings.first().emberId != 0 } }
        assertEquals(Ember.SEALED, vm.ember.value)
        until("the covered screen") { vm.ui.value.phase == Phase.COVERED }
        assertTrue(vm.ui.value.newEmber)

        // A second evening the same day keeps the ember that is still sealed.
        val sealed = app.container.settings.settings.first().emberId
        vm.backToCold()
        vm.hold(mum, true)
        vm.hold(kid, true)
        vm.ignite(listOf(mum, kid, guest))
        vm.ask()
        assertNotEquals(sealed, vm.ui.value.question!!.id.takeIf { vm.ui.value.questionIsEmber })
        vm.letGo()
        vm.cover()
        until("the second covered screen") { vm.ui.value.phase == Phase.COVERED }
        assertEquals(false, vm.ui.value.newEmber)
        assertEquals(sealed, app.container.settings.settings.first().emberId)
    }
}
