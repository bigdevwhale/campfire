package app.campfire.ui.fire

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.campfire.AppContainer
import app.campfire.BuildConfig
import app.campfire.data.Evening
import app.campfire.data.Seat
import app.campfire.domain.EMBER_BREATHS
import app.campfire.domain.QUESTIONS
import app.campfire.domain.Question
import app.campfire.domain.QuestionBag
import app.campfire.domain.Stage
import app.campfire.domain.logsFor
import app.campfire.domain.yearAgo
import app.campfire.ru
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Where one evening is. COLD is the dark clearing with last night's ash; BURNING and ASH are the
 * fire's short life; SMOKE is the moment after two palms cover it; COVERED shows the ticket going
 * into the box before the clearing turns cold again.
 */
enum class Phase { COLD, BURNING, ASH, SMOKE, COVERED }

/** The sealed ember from the last evening: none, still sealed until tomorrow, or ready to open. */
enum class Ember { NONE, SEALED, READY }

data class EveningUi(
    val phase: Phase = Phase.COLD,
    /** Stones being held right now, in the cold. */
    val held: Set<Long> = emptySet(),
    /** Who lit the fire tonight, as they were named then. */
    val present: List<Seat> = emptyList(),
    val logs: Int = 0,
    val question: Question? = null,
    /** The spark in the air is yesterday's sealed ember. */
    val questionIsEmber: Boolean = false,
    /** Questions asked of the embers after the logs ran out. */
    val breaths: Int = 0,
    /** Whoever holds the burning branch answers. */
    val branch: Seat? = null,
    val ash: String = "",
    val recording: Boolean = false,
    /** COVERED: a new ember was set aside tonight (false: one is still sealed from before). */
    val newEmber: Boolean = false,
    /** COVERED: the fire went out on its own — nobody covered it. */
    val silent: Boolean = false,
    val startedAt: Long = 0,
    val asked: Int = 0,
) {
    val stage: Stage get() = if (phase == Phase.ASH) Stage.EMBERS else Stage.of(logs)
    val canAsk: Boolean
        get() = question == null && when (phase) {
            Phase.BURNING -> logs > 0
            Phase.ASH -> breaths < EMBER_BREATHS
            else -> false
        }
}

class EveningVm(private val container: AppContainer) : ViewModel() {

    val seats: StateFlow<List<Seat>> = container.db.dao().seats()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** The evening the fire remembers today, if there was one about a year ago. */
    val remembered: StateFlow<Evening?> = container.db.dao().evenings()
        .map { all -> yearAgo(all, LocalDate.now()) { it.day } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** The last evening, whose word still shows in the cold ash. */
    val last: StateFlow<Evening?> = container.db.dao().evenings()
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val voice: StateFlow<Float> = container.recorder.amplitude

    private val _ui = MutableStateFlow(EveningUi())
    val ui: StateFlow<EveningUi> = _ui

    private val _ember = MutableStateFlow(Ember.NONE)
    val ember: StateFlow<Ember> = _ember

    /** Whether the cassette is laid by the fire for this evening. Never remembered between evenings. */
    private val _cassette = MutableStateFlow(false)
    val cassette: StateFlow<Boolean> = _cassette

    /** One-off things to tell the people at the fire (the microphone wouldn't come, say). */
    val notices = MutableStateFlow<Int?>(null)

    private val bag = QuestionBag()
    private var sealed: Question? = null
    private var sealedFrom: Long = 0
    private var opening: Question? = null
    private val asked = mutableListOf<Question>()
    private var settle: Job? = null
    /** An evening has been lit since this screen opened. */
    private var started = false

    init {
        viewModelScope.launch {
            val s = container.settings.settings.first()
            // The read may land after an evening has already begun: keep what it asked, and
            // don't bring back an ember it has opened or replace one it has set aside.
            bag.restore(s.usedIds + bag.usedIds())
            if (!started) {
                sealed = QUESTIONS.firstOrNull { it.id == s.emberId }
                sealedFrom = s.emberFrom
                refreshEmber()
            }
        }
    }

    private fun refreshEmber() {
        _ember.value = when {
            sealed == null -> Ember.NONE
            LocalDate.now().isBefore(dayOf(sealedFrom)) -> Ember.SEALED
            else -> Ember.READY
        }
    }

    fun hold(seat: Seat, held: Boolean) {
        val u = _ui.value
        if (u.phase != Phase.COLD) return
        _ui.value = u.copy(held = if (held) u.held + seat.id else u.held - seat.id)
    }

    fun cassette(on: Boolean) {
        if (_ui.value.phase == Phase.COLD) _cassette.value = on
    }

    /**
     * Two stones held long enough: the fire lights for whoever held them. [stones] is every stone
     * by the fire tonight, the guest's included. Screenshots light it for everyone at once.
     */
    fun ignite(stones: List<Seat>, everyone: Boolean = false) {
        val u = _ui.value
        if (u.phase != Phase.COLD) return
        val who = if (everyone) stones else stones.filter { it.id in u.held }
        if (who.size < 2) return
        started = true
        refreshEmber()
        opening = if (_ember.value == Ember.READY) sealed else null
        asked.clear()
        val recording = _cassette.value && container.recorder.start()
        if (_cassette.value && !recording) {
            _cassette.value = false
            notices.value = app.campfire.R.string.mic_needed
        }
        _ui.value = EveningUi(
            phase = Phase.BURNING,
            present = who,
            logs = logsFor(who.size),
            recording = recording,
            startedAt = System.currentTimeMillis(),
        )
        settleLater()
    }

    /** A tap on the fire: a spark rises with a question, and one log gives in to it. */
    fun ask() {
        val u = _ui.value
        if (!u.canAsk) return
        val ember = opening
        val q = if (ember != null && asked.isEmpty()) {
            opening = null
            sealed = null
            container.write { settings.clearEmber() }
            refreshEmber()
            ember
        } else {
            bag.draw(u.stage)
        }
        asked += q
        _ui.value = u.copy(
            question = q,
            questionIsEmber = q === ember,
            breaths = if (u.phase == Phase.ASH) u.breaths + 1 else u.breaths,
            logs = if (u.phase == Phase.BURNING) u.logs - 1 else u.logs,
            asked = asked.size,
        )
        settleLater()
    }

    /** The question is let go; when the last log has burned, the ash is ready for a word. */
    fun letGo() {
        val u = _ui.value
        if (u.question == null) return
        _ui.value = u.copy(
            question = null,
            phase = if (u.phase == Phase.BURNING && u.logs == 0) Phase.ASH else u.phase,
        )
        settleLater()
    }

    /** The burning branch goes to whoever answers next; tapping its holder takes it back. */
    fun handBranch(to: Seat) {
        val u = _ui.value
        if (u.phase != Phase.BURNING && u.phase != Phase.ASH) return
        _ui.value = u.copy(branch = if (u.branch?.id == to.id) null else to)
    }

    fun ash(word: String) {
        _ui.value = _ui.value.copy(ash = word.take(24))
        settleLater()
    }

    /** Two palms held over the fire: smoke, the ticket goes into the box, an ember is set aside. */
    fun cover(silent: Boolean = false) {
        val u = _ui.value
        if (u.phase != Phase.BURNING && u.phase != Phase.ASH) return
        settle?.cancel()
        val recorded = if (u.recording) container.recorder.stop() else null
        _ui.value = u.copy(phase = Phase.SMOKE, question = null, recording = false)

        // Only one ember waits at a time: a second evening the same day keeps the sealed one.
        val newEmber = if (sealed == null) bag.drawEmber() else null
        if (newEmber != null) {
            sealed = newEmber
            sealedFrom = tomorrowMidnight()
        }
        val ru = container.context.ru
        val now = System.currentTimeMillis()
        val evening = Evening(
            endedAt = now,
            seatNames = Evening.join(u.present.map { it.name }),
            seatGlyphs = Evening.glyphIds(u.present.map { it.glyph }),
            questions = Evening.join(asked.map { it.text(ru) }),
            word = u.ash.trim(),
            recording = recorded,
            minutes = ((now - u.startedAt) / 60_000).toInt().coerceAtLeast(1),
        )
        val used = bag.usedIds()
        val from = sealedFrom
        container.write {
            db.dao().addEvening(evening)
            settings.saveUsed(used)
            if (newEmber != null) settings.saveEmber(newEmber.id, from)
        }
        refreshEmber()
        viewModelScope.launch {
            delay(if (silent) 300 else 1800)
            _ui.value = EveningUi(phase = Phase.COVERED, present = u.present, ash = u.ash.trim(), newEmber = newEmber != null, silent = silent)
        }
    }

    fun backToCold() {
        if (_ui.value.phase == Phase.COVERED) {
            _cassette.value = false
            _ui.value = EveningUi()
        }
    }

    /**
     * When nobody asks anything, the fire settles by itself: a log every few quiet minutes, and a
     * long quiet in the ash puts it out — the evening is still ticketed, just without a cover.
     */
    private fun settleLater() {
        settle?.cancel()
        if (BuildConfig.SHOTS) return
        settle = viewModelScope.launch {
            while (true) {
                val u = _ui.value
                when {
                    u.phase == Phase.BURNING && u.question == null -> {
                        delay(LOG_SETTLES_MS)
                        val now = _ui.value
                        if (now.phase != Phase.BURNING || now.question != null) return@launch
                        val logs = now.logs - 1
                        _ui.value = now.copy(logs = logs.coerceAtLeast(0), phase = if (logs <= 0) Phase.ASH else Phase.BURNING)
                    }
                    u.phase == Phase.BURNING || u.phase == Phase.ASH -> {
                        // A question in the air means people are talking: give them long.
                        delay(if (u.question != null) TALKING_MS else GOES_OUT_MS)
                        cover(silent = true)
                        return@launch
                    }
                    else -> return@launch
                }
            }
        }
    }

    override fun onCleared() {
        // The screen went away mid-evening: no ticket, so the take goes with it.
        container.recorder.cancel()
        super.onCleared()
    }

    companion object {
        const val LOG_SETTLES_MS = 4 * 60 * 1000L
        const val GOES_OUT_MS = 6 * 60 * 1000L
        const val TALKING_MS = 20 * 60 * 1000L

        private fun dayOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

        fun tomorrowMidnight(): Long =
            LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
