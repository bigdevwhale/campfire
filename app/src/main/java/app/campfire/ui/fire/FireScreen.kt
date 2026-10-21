package app.campfire.ui.fire

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.campfire.BuildConfig
import app.campfire.R
import app.campfire.container
import app.campfire.data.Seat
import app.campfire.ru
import app.campfire.ui.components.Cassette
import app.campfire.ui.components.EmberDot
import app.campfire.ui.components.fireGestures
import app.campfire.ui.theme.LocalPalette
import app.campfire.ui.theme.LocalType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** How long two stones or two palms must be held before the fire answers. */
private const val IGNITE_MS = 1400f
private const val COVER_MS = 1200f

/**
 * The one screen of the app: a clearing at night. Hold two stones and the fire lights; tap it and a
 * spark brings a question; tap a stone to hand over the burning branch; write one word into the ash;
 * two palms over the fire put it out, and the evening goes into the box. Gestures, not menus — and
 * while the fire burns there is nothing on screen that calls anyone away.
 */
@Composable
fun FireScreen(onBox: () -> Unit, onSettings: () -> Unit, onNight: () -> Unit) {
    val context = LocalContext.current
    val vm: EveningVm = viewModel { EveningVm(context.container) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val seats by vm.seats.collectAsStateWithLifecycle()
    val ember by vm.ember.collectAsStateWithLifecycle()
    val remembered by vm.remembered.collectAsStateWithLifecycle()
    val last by vm.last.collectAsStateWithLifecycle()
    val voice by vm.voice.collectAsStateWithLifecycle()
    val cassette by vm.cassette.collectAsStateWithLifecycle()
    val notice by vm.notices.collectAsStateWithLifecycle()
    val p = LocalPalette.current
    val type = LocalType.current
    val haptics = LocalHapticFeedback.current

    // The fire's clock. Screenshots get a still fire, so uiautomator can read the screen.
    var t by remember { mutableFloatStateOf(1.3f) }
    LaunchedEffect(Unit) {
        if (!BuildConfig.SHOTS) while (isActive) withFrameNanos { t = it / 1_000_000_000f }
    }

    val burning = ui.phase == Phase.BURNING || ui.phase == Phase.ASH
    // Nobody should have to touch the phone just to keep the fire on the screen.
    val view = LocalView.current
    LaunchedEffect(burning || ui.phase == Phase.SMOKE) { view.keepScreenOn = burning || ui.phase == Phase.SMOKE }

    val guest = Seat(id = Seat.GUEST_ID, name = stringResource(R.string.guest), glyph = 5)
    // A stone still waiting for its name in the settings doesn't sit by the fire yet.
    val stones = seats.filter { it.name.isNotBlank() } + guest

    // Two stones held at once: the ring fills, and when it closes, the fire lights.
    var igniting by remember { mutableFloatStateOf(0f) }
    val twoHeld = ui.phase == Phase.COLD && ui.held.size >= 2
    LaunchedEffect(twoHeld) {
        igniting = 0f
        if (!twoHeld) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (igniting < 1f) withFrameNanos { igniting = ((it - start) / 1e6f / IGNITE_MS).coerceAtMost(1f) }
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        vm.ignite(stones)
        igniting = 0f
    }

    // Two palms resting on the fire: the smoke ring fills, then it goes out.
    var fingers by remember { mutableIntStateOf(0) }
    var covering by remember { mutableFloatStateOf(0f) }
    // The gesture surface goes away with the fire, and with it any fingers it was counting.
    LaunchedEffect(burning) { if (!burning) fingers = 0 }
    val twoPalms = burning && fingers >= 2
    LaunchedEffect(twoPalms) {
        covering = 0f
        if (!twoPalms) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (covering < 1f) withFrameNanos { covering = ((it - start) / 1e6f / COVER_MS).coerceAtMost(1f) }
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        vm.cover()
        covering = 0f
    }

    LaunchedEffect(ui.phase, ui.silent) {
        when {
            // The fire went out by itself: no ceremony, just the cold clearing again.
            ui.phase == Phase.COVERED && ui.silent -> vm.backToCold()
            ui.phase == Phase.COVERED -> {
                delay(12_000)
                vm.backToCold()
            }
        }
    }

    LaunchedEffect(notice) {
        notice?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.notices.value = null
        }
    }

    // The cassette's one honest ask for the microphone.
    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.cassette(true) else Toast.makeText(context, R.string.mic_needed, Toast.LENGTH_SHORT).show()
    }
    val toggleCassette = {
        when {
            cassette -> vm.cassette(false)
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> vm.cassette(true)
            else -> mic.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(p.bg)) {
        val w = maxWidth
        val h = maxHeight

        FireArt(
            look = when (ui.phase) {
                Phase.BURNING, Phase.ASH -> Look.BURNING
                Phase.SMOKE -> Look.SMOKE
                else -> Look.COLD
            },
            stage = ui.stage,
            time = t,
            voice = voice,
            hold = if (igniting > 0f) igniting else covering,
            holdIsCover = covering > 0f,
            modifier = Modifier.fillMaxSize(),
        )

        // The fire answers the whole clearing: a tap asks (or lets a question go), two palms cover.
        if (burning) {
            Box(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    fireGestures(
                        onTap = { if (vm.ui.value.question != null) vm.letGo() else vm.ask() },
                        onFingers = { fingers = it },
                    )
                },
            )
        }
        if (ui.phase == Phase.COLD && BuildConfig.SHOTS) {
            // Screenshots light the fire for everyone with one tap on the sky.
            Box(Modifier.fillMaxWidth().height(h * 0.4f).pointerInput(stones) { detectTapGestures { vm.ignite(stones, everyone = true) } })
        }

        // Last evening's word, still legible in the cold ash.
        if (ui.phase == Phase.COLD) {
            last?.word?.takeIf { it.isNotBlank() }?.let { word ->
                Text(
                    word,
                    style = type.question.copy(fontSize = 15.sp, fontStyle = FontStyle.Italic),
                    color = p.smoke,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().offset(y = h * Clearing.FIRE + 36.dp),
                )
            }
        }

        // The sky: questions, hints and the word for the ash all live up here, above the flames.
        Box(
            Modifier
                .fillMaxWidth()
                .height(h * (Clearing.HORIZON - 0.04f))
                .statusBarsPadding()
                .padding(top = 56.dp, start = 28.dp, end = 28.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Sky(ui, ember, twoHeld, onLetGo = vm::letGo, onWord = vm::ash)
        }

        // Up top, only in the cold: once the fire burns, nothing should call anyone away.
        if (ui.phase == Phase.COLD) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNight) { Icon(Icons.Outlined.Bedtime, stringResource(R.string.night), tint = p.muted) }
                Row {
                    IconButton(onClick = onBox) { Icon(Icons.Outlined.Inventory2, stringResource(R.string.box), tint = p.muted) }
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings), tint = p.muted) }
                }
            }
            Column(
                Modifier.fillMaxWidth().statusBarsPadding().padding(top = 52.dp, start = 32.dp, end = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (ember != Ember.NONE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmberDot(time = t)
                        Text(
                            stringResource(if (ember == Ember.READY) R.string.ember_ready else R.string.ember_still),
                            style = type.label,
                            color = if (ember == Ember.READY) p.flame else p.muted,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                remembered?.let { r ->
                    Text(
                        if (r.word.isNotBlank()) stringResource(R.string.recall_line, r.word) else stringResource(R.string.recall_plain),
                        style = type.label,
                        color = p.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onBox).padding(6.dp),
                    )
                }
            }
        }

        // The stones, on an arc around the fire's foot.
        val ring = if (ui.phase == Phase.COLD) stones else ui.present
        ring.forEachIndexed { i, seat ->
            val (x, y) = stoneAt(i, ring.size, w, h)
            Stone(
                seat = seat,
                held = ui.phase == Phase.COLD && seat.id in ui.held,
                warm = burning,
                branch = burning && ui.branch?.id == seat.id,
                onHold = if (ui.phase == Phase.COLD) { held -> vm.hold(seat, held) } else null,
                onTap = if (burning) { { vm.handBranch(seat) } } else null,
                modifier = Modifier.offset(x = x - (STONE_SIZE + 20.dp) / 2, y = y - 20.dp - STONE_SIZE / 2),
            )
        }

        // Bottom: the cassette, and once the fire burns, how to put it out.
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (ui.phase == Phase.COLD || ui.recording) {
                Cassette(
                    on = cassette || ui.recording,
                    turning = ui.recording,
                    time = t,
                    label = stringResource(if (cassette || ui.recording) R.string.cassette_on else R.string.cassette_off),
                    onClick = if (ui.phase == Phase.COLD) toggleCassette else null,
                )
            }
            Spacer(Modifier.weight(1f))
        }

        if (burning && (ui.asked > 0 || ui.phase == Phase.ASH)) {
            Text(
                stringResource(R.string.cover_hint),
                style = type.label,
                color = p.faint,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 28.dp),
            )
        }

        // After the smoke: the ticket goes into the box, and an ember into the pocket.
        AnimatedVisibility(
            visible = ui.phase == Phase.COVERED && !ui.silent,
            enter = fadeIn(tween(900)),
            exit = fadeOut(tween(600)),
        ) {
            Covered(ui, t, onDone = vm::backToCold)
        }
    }
}

/** Where stone [i] of [n] sits: an arc in front of the fire, wider the more people there are. */
private fun stoneAt(i: Int, n: Int, w: Dp, h: Dp): Pair<Dp, Dp> {
    val spread = min(150.0, 45.0 * (n - 1))
    val deg = if (n == 1) 90.0 else 90.0 + spread / 2 - spread * i / (n - 1)
    val a = Math.toRadians(deg)
    val x = w / 2 + w * Clearing.RING_X * cos(a).toFloat()
    val y = h * Clearing.FIRE + h * Clearing.RING_Y * sin(a).toFloat()
    return x to y
}

/** The sky's content for each moment of the evening. */
@Composable
private fun Sky(ui: EveningUi, ember: Ember, twoHeld: Boolean, onLetGo: () -> Unit, onWord: (String) -> Unit) {
    val p = LocalPalette.current
    val type = LocalType.current
    val ru = LocalContext.current.ru
    AnimatedContent(
        targetState = ui.question,
        transitionSpec = {
            // A spark rises from the fire and opens into words.
            (fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 2 }) togetherWith fadeOut(tween(300))
        },
        contentKey = { it?.id },
        label = "spark",
    ) { q ->
        when {
            q != null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .pointerInput(q.id) { detectTapGestures { onLetGo() } },
            ) {
                if (ui.questionIsEmber) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmberDot(time = 0.8f)
                        Text(stringResource(R.string.from_ember), style = type.label, color = p.flame, modifier = Modifier.padding(start = 6.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Text(q.text(ru), style = type.question, color = p.ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                val holder = ui.branch
                Text(
                    if (holder != null) stringResource(R.string.now_you, holder.name) else stringResource(R.string.branch_hint),
                    style = if (holder != null) type.body else type.label,
                    color = if (holder != null) p.flameCore else p.faint,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.let_go), style = type.label, color = p.faint.copy(alpha = 0.7f))
            }
            ui.phase == Phase.ASH -> AshWord(ui.ash, onWord)
            ui.phase == Phase.BURNING -> Hint(stringResource(R.string.tap_fire))
            ui.phase == Phase.COLD -> Hint(
                stringResource(if (ui.held.size == 1) R.string.one_more else R.string.hold_hint),
                strong = ui.held.size == 1 || twoHeld || ember == Ember.READY,
            )
            else -> Spacer(Modifier.height(1.dp))
        }
    }
}

@Composable
private fun Hint(text: String, strong: Boolean = false) {
    Text(
        text,
        style = LocalType.current.question.copy(fontSize = 19.sp, lineHeight = 26.sp),
        color = if (strong) LocalPalette.current.muted else LocalPalette.current.faint,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 300.dp).padding(bottom = 12.dp),
    )
}

/** One word for the day, written into the ash: a bare line in the dark, no box around it. */
@Composable
private fun AshWord(word: String, onWord: (String) -> Unit) {
    val p = LocalPalette.current
    val type = LocalType.current
    val keyboard = LocalSoftwareKeyboardController.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 8.dp)) {
        Text(stringResource(R.string.word_prompt), style = type.question.copy(fontSize = 21.sp), color = p.muted)
        Spacer(Modifier.height(18.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.width(240.dp)) {
            if (word.isEmpty()) {
                Text(stringResource(R.string.word_hint), style = type.question.copy(fontStyle = FontStyle.Italic), color = p.faint)
            }
            BasicTextField(
                value = word,
                onValueChange = onWord,
                singleLine = true,
                textStyle = type.question.copy(color = p.flameCore, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(p.flame),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(Modifier.width(200.dp).height(1.dp).background(p.smoke))
    }
}

/** The ticket going into the box, and the ember set aside — or the one still waiting. */
@Composable
private fun Covered(ui: EveningUi, t: Float, onDone: () -> Unit) {
    val p = LocalPalette.current
    val type = LocalType.current
    Box(
        Modifier.fillMaxSize().background(p.bg.copy(alpha = 0.82f)).pointerInput(Unit) { detectTapGestures { onDone() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(36.dp)) {
            Text(stringResource(R.string.covered), style = type.body, color = p.muted, textAlign = TextAlign.Center)
            if (ui.ash.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.in_ash, ui.ash), style = type.title, color = p.ink, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(44.dp))
            EmberDot(time = t, big = true)
            Spacer(Modifier.height(16.dp))
            if (ui.newEmber) {
                Text(stringResource(R.string.ember_title), style = type.question, color = p.ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.ember_sub), style = type.body, color = p.muted, textAlign = TextAlign.Center)
            } else {
                Text(stringResource(R.string.ember_still), style = type.body, color = p.muted, textAlign = TextAlign.Center)
            }
        }
    }
}
