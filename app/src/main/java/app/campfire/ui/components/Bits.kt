package app.campfire.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.campfire.ui.theme.LocalPalette
import app.campfire.ui.theme.LocalType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * A pocket cassette: an honest thing, no characters. Laid by the fire before lighting, its reels
 * turn while the evening records — anyone can see it, and a tap on it in the cold takes it away.
 */
@Composable
fun Cassette(on: Boolean, turning: Boolean, time: Float, label: String, onClick: (() -> Unit)?) {
    val p = LocalPalette.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .semantics {
                contentDescription = label
                role = Role.Switch
            }
            .let { m -> if (onClick != null) m.pointerInput(Unit) { detectTapGestures { onClick() } } else m }
            .padding(8.dp),
    ) {
        Canvas(Modifier.size(width = 56.dp, height = 36.dp)) {
            val w = size.width
            val h = size.height
            val r = h * 0.14f
            val alpha = if (on) 1f else 0.45f
            drawRoundRect(Color(0xFF2A2E3C).copy(alpha = alpha), cornerRadius = CornerRadius(r, r))
            drawRoundRect(p.line, cornerRadius = CornerRadius(r, r), style = Stroke(width = h * 0.04f))
            // The label strip, paper like the tickets.
            drawRoundRect(p.paper.copy(alpha = 0.75f * alpha), topLeft = Offset(w * 0.1f, h * 0.1f), size = Size(w * 0.8f, h * 0.2f), cornerRadius = CornerRadius(r * 0.4f))
            // The window with two reels.
            drawRoundRect(Color(0xFF10131C), topLeft = Offset(w * 0.18f, h * 0.38f), size = Size(w * 0.64f, h * 0.36f), cornerRadius = CornerRadius(r * 0.6f))
            val spin = if (turning) time * 3f else 0f
            listOf(0.33f, 0.67f).forEach { fx ->
                val c = Offset(w * fx, h * 0.56f)
                val rr = h * 0.13f
                drawCircle(Color(0xFF8B93A8).copy(alpha = alpha), radius = rr, center = c)
                for (k in 0 until 3) {
                    val a = spin + k * 2.094f
                    drawLine(Color(0xFF2A2E3C), c, Offset(c.x + rr * cos(a), c.y + rr * sin(a)), strokeWidth = h * 0.05f)
                }
            }
            if (turning) {
                // The small warm light that says "listening".
                drawCircle(p.danger.copy(alpha = 0.5f + 0.4f * abs(sin(time * 2f))), radius = h * 0.06f, center = Offset(w * 0.5f, h * 0.88f))
            }
        }
        Text(label, style = LocalType.current.label, color = if (on) p.muted else p.faint, modifier = Modifier.padding(top = 4.dp))
    }
}

/** The sealed ember: a small coal that breathes, waiting for the next lighting. */
@Composable
fun EmberDot(time: Float, big: Boolean = false, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Canvas(modifier.size(if (big) 56.dp else 14.dp)) {
        val breathe = 0.55f + 0.45f * abs(sin(time * 1.6f))
        val r = size.minDimension / 4f * (0.8f + 0.2f * breathe)
        drawCircle(p.ember.copy(alpha = 0.25f * breathe), radius = r * 2f)
        drawCircle(p.ember, radius = r)
        drawCircle(p.flame.copy(alpha = 0.7f * breathe), radius = r * 0.5f)
    }
}

/**
 * The fire's own gestures in one place: a short one-finger tap asks for a spark, and [onFingers]
 * reports how many fingers rest on it, so the screen can time two palms covering it. A still finger
 * may send no events at all, which is why the hold is timed by the screen, not here. Once a second
 * finger has come down, letting go is never a tap.
 */
suspend fun PointerInputScope.fireGestures(onTap: () -> Unit, onFingers: (Int) -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var most = 1
        var last = 1
        var moved = false
        onFingers(1)
        while (true) {
            val event = awaitPointerEvent()
            val count = event.changes.count { it.pressed }
            most = maxOf(most, count)
            event.changes.firstOrNull { it.id == down.id }?.let {
                if ((it.position - down.position).getDistance() > viewConfiguration.touchSlop) moved = true
            }
            if (count != last) {
                last = count
                onFingers(count)
            }
            if (count == 0) {
                val quick = event.changes.first().uptimeMillis - down.uptimeMillis < 600
                if (most == 1 && quick && !moved) onTap()
                break
            }
        }
    }
}
