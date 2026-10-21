package app.campfire.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.BuildConfig
import app.campfire.R
import app.campfire.container
import app.campfire.data.Seat
import app.campfire.domain.Stage
import app.campfire.ui.components.FireButton
import app.campfire.ui.components.SeatRow
import app.campfire.ui.fire.FireArt
import app.campfire.ui.fire.Look
import app.campfire.ui.theme.LocalPalette
import app.campfire.ui.theme.LocalType
import kotlinx.coroutines.isActive

/**
 * Three short steps before the first evening: what this is, how the evening goes, and who sits by
 * the fire. The fire burns behind all of it, so the first thing anyone sees is the thing itself.
 */
@Composable
fun Welcome() {
    val context = LocalContext.current
    val p = LocalPalette.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var t by remember { mutableFloatStateOf(1.3f) }
    LaunchedEffect(Unit) {
        if (!BuildConfig.SHOTS) while (isActive) withFrameNanos { t = it / 1_000_000_000f }
    }
    val seats = remember { mutableStateListOf(Seat(name = "", glyph = 3), Seat(name = "", glyph = 0)) }

    Box(Modifier.fillMaxSize().background(p.bg)) {
        FireArt(
            look = Look.BURNING,
            stage = if (step == 2) Stage.LOGS else Stage.FLAME,
            time = t,
            voice = 0f,
            hold = 0f,
            holdIsCover = false,
            dim = if (step == 2) 0.45f else 1f,
            modifier = Modifier.fillMaxSize(),
        )
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "welcome",
            modifier = Modifier.fillMaxSize(),
        ) { s ->
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (s) {
                    0 -> Intro()
                    1 -> How()
                    else -> Seats(seats)
                }
                Spacer(Modifier.weight(1f))
                Dots(step)
                Spacer(Modifier.height(18.dp))
                val named = seats.filter { it.name.isNotBlank() }
                FireButton(
                    text = stringResource(if (s < 2) R.string.next else R.string.start),
                    enabled = s < 2 || named.isNotEmpty(),
                    onClick = {
                        if (s < 2) {
                            step = s + 1
                        } else {
                            context.container.write {
                                named.forEach { db.dao().addSeat(it.copy(name = it.name.trim())) }
                                settings.seeWelcome()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun Intro() {
    val p = LocalPalette.current
    val type = LocalType.current
    Spacer(Modifier.height(72.dp))
    Text(stringResource(R.string.welcome_title), style = type.title, color = p.ink, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.welcome_sub), style = type.body, color = p.muted, textAlign = TextAlign.Center)
}

@Composable
private fun How() {
    val p = LocalPalette.current
    val type = LocalType.current
    Spacer(Modifier.height(48.dp))
    listOf(
        R.string.how1_title to R.string.how1_sub,
        R.string.how2_title to R.string.how2_sub,
        R.string.how3_title to R.string.how3_sub,
    ).forEachIndexed { i, (title, sub) ->
        // A dark card, so the words stay legible over the fire behind them.
        Column(
            Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                .background(p.surface.copy(alpha = 0.82f)).padding(18.dp),
        ) {
            Text("${i + 1}. " + stringResource(title), style = type.question.copy(fontSize = type.body.fontSize * 1.25f), color = p.ink)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(sub), style = type.body, color = p.muted)
        }
    }
}

@Composable
private fun Seats(seats: MutableList<Seat>) {
    val p = LocalPalette.current
    val type = LocalType.current
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(36.dp))
        Text(stringResource(R.string.seats_title), style = type.title, color = p.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.seats_onboard_sub), style = type.body, color = p.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))
        Column(Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).background(p.surface.copy(alpha = 0.88f)).padding(14.dp)) {
            seats.forEachIndexed { i, seat ->
                SeatRow(seat, onChange = { seats[i] = it }, onRemove = if (seats.size > 1) { { seats.removeAt(i) } } else null)
            }
            if (seats.size < 6) {
                TextButton(onClick = { seats.add(Seat(name = "", glyph = (seats.size * 2) % Seat.GLYPHS)) }) {
                    Text("+ " + stringResource(R.string.add_seat), style = type.body, color = p.flame)
                }
            }
            Text(stringResource(R.string.tap_head), style = type.label, color = p.faint, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun Dots(step: Int) {
    val p = LocalPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { i ->
            Box(Modifier.size(if (i == step) 8.dp else 6.dp).clip(CircleShape).background(if (i == step) p.flame else p.line))
        }
    }
    Spacer(Modifier.width(1.dp))
}
