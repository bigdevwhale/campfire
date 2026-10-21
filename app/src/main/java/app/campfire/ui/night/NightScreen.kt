package app.campfire.ui.night

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.BuildConfig
import app.campfire.R
import app.campfire.domain.Stage
import app.campfire.ui.fire.FireArt
import app.campfire.ui.fire.Look
import app.campfire.ui.theme.LocalPalette
import app.campfire.ui.theme.LocalType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** How long the night fire glows before it dozes off and lets the screen sleep. */
private const val GLOWS_MS = 40 * 60 * 1000L
private const val DOZES_MS = 3 * 60 * 1000L

/**
 * Spending the night: embers low on the screen as a night-light, no questions, no buttons. It
 * keeps the screen on for a while, then fades out over a few minutes and lets the phone sleep.
 */
@Composable
fun NightScreen(onLeave: () -> Unit) {
    val p = LocalPalette.current
    val type = LocalType.current
    val view = LocalView.current
    var t by remember { mutableFloatStateOf(1.3f) }
    var dim by remember { mutableFloatStateOf(0.75f) }
    var words by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (!BuildConfig.SHOTS) while (isActive) withFrameNanos { t = it / 1_000_000_000f }
    }
    LaunchedEffect(Unit) {
        if (BuildConfig.SHOTS) return@LaunchedEffect
        delay(6_000)
        words = false
        delay(GLOWS_MS)
        val start = withFrameNanos { it }
        while (dim > 0f) withFrameNanos { dim = 0.75f * (1f - (it - start) / 1e6f / DOZES_MS).coerceAtLeast(0f) }
        view.keepScreenOn = false
    }
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Box(Modifier.fillMaxSize().background(p.bg).pointerInput(Unit) { detectTapGestures { onLeave() } }) {
        FireArt(look = Look.BURNING, stage = Stage.EMBERS, time = t * 0.6f, voice = 0f, hold = 0f, holdIsCover = false, dim = dim, modifier = Modifier.fillMaxSize())
        // The clearing beyond the embers goes darker than the fire screen's: it's for sleeping.
        Box(Modifier.fillMaxSize().background(p.bg.copy(alpha = 0.25f)))
        AnimatedVisibility(
            visible = words,
            enter = fadeIn(),
            exit = fadeOut(tween(2500)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 80.dp, start = 32.dp, end = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.night), style = type.title, color = p.muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.night_sub), style = type.body, color = p.faint, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.night_leave), style = type.label, color = p.faint, textAlign = TextAlign.Center)
            }
        }
    }
}
