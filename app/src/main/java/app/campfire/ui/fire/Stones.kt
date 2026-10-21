package app.campfire.ui.fire

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.data.Seat
import app.campfire.ui.theme.LocalPalette
import app.campfire.ui.theme.LocalType

val STONE_SIZE = 60.dp

/**
 * A stone by the fire with someone on it. In the cold it answers a press-and-hold — that's how two
 * people light the fire, each on their own stone. While the fire burns, a tap hands that person
 * the burning branch. No photos, no accounts: a drawn head and a name.
 */
@Composable
fun Stone(
    seat: Seat,
    held: Boolean = false,
    warm: Boolean = false,
    branch: Boolean = false,
    onHold: ((Boolean) -> Unit)? = null,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.width(STONE_SIZE + 20.dp)) {
        // The branch floats over whoever answers now.
        Box(Modifier.height(20.dp), contentAlignment = Alignment.Center) {
            if (branch) Canvas(Modifier.size(width = 34.dp, height = 18.dp)) { drawBranch(p.flame, p.flameCore) }
        }
        Box(
            Modifier
                .size(STONE_SIZE)
                .clip(CircleShape)
                .semantics {
                    contentDescription = seat.name
                    role = Role.Button
                }
                .let { m ->
                    when {
                        onHold != null -> m.pointerInput(seat.id) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false).consume()
                                onHold(true)
                                try {
                                    waitForUpOrCancellation()
                                } finally {
                                    onHold(false)
                                }
                            }
                        }
                        onTap != null -> m.pointerInput(seat.id) { detectTapGestures { onTap() } }
                        else -> m
                    }
                },
        ) {
            Canvas(Modifier.size(STONE_SIZE)) {
                // The stone itself, a rounded grey with a lighter top.
                drawCircle(Brush.verticalGradient(listOf(Color(0xFF3A3E45), Color(0xFF1E2126))))
                if (warm || held) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(p.flame.copy(alpha = if (held) 0.45f else 0.22f), Color.Transparent),
                            center = Offset(size.width / 2, size.height * 0.15f),
                            radius = size.width * 0.8f,
                        ),
                    )
                }
                inset(size.width * 0.12f) {
                    val tone = if (held) p.flameCore else if (warm) Color(0xFF151012) else p.muted.copy(alpha = 0.55f)
                    drawSilhouette(seat.glyph, tone, lit = if (warm || held) p.flame else null)
                }
                val ring = when {
                    held -> p.flame
                    branch -> p.flameCore
                    else -> p.line
                }
                drawCircle(ring, style = Stroke(width = if (held || branch) 2.5.dp.toPx() else 1.dp.toPx()))
            }
        }
        Text(
            seat.name,
            style = LocalType.current.label,
            color = if (held || warm) p.ink else p.faint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** A little burning branch: a twig, an offshoot, and fire at the tip. */
fun DrawScope.drawBranch(flame: Color, core: Color) {
    val w = size.width
    val h = size.height
    val wood = Color(0xFF8A6B43)
    drawLine(wood, Offset(w * 0.05f, h * 0.8f), Offset(w * 0.75f, h * 0.45f), strokeWidth = h * 0.16f, cap = StrokeCap.Round)
    drawLine(wood, Offset(w * 0.42f, h * 0.62f), Offset(w * 0.5f, h * 0.3f), strokeWidth = h * 0.1f, cap = StrokeCap.Round)
    drawCircle(flame, radius = h * 0.28f, center = Offset(w * 0.82f, h * 0.36f))
    drawCircle(core, radius = h * 0.14f, center = Offset(w * 0.84f, h * 0.38f))
}

/**
 * The drawn heads: shoulders, a head, and one telling detail — braid, cap, bun, kerchief, hood,
 * or nothing at all. Facing the fire, a person is a dark shape with a warm rim; [lit] is that rim.
 */
fun DrawScope.drawSilhouette(glyph: Int, color: Color, lit: Color? = null) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val headY = h * 0.38f
    val headR = w * 0.19f
    fun oval(c: Offset, r: Float) = Path().apply { addOval(Rect(c, r)) }

    val parts = mutableListOf(
        Path().apply { // Shoulders.
            moveTo(cx - w * 0.36f, h)
            cubicTo(cx - w * 0.36f, h * 0.7f, cx - w * 0.2f, h * 0.62f, cx, h * 0.62f)
            cubicTo(cx + w * 0.2f, h * 0.62f, cx + w * 0.36f, h * 0.7f, cx + w * 0.36f, h)
            close()
        },
        oval(Offset(cx, headY), headR),
    )
    when (glyph % Seat.GLYPHS) {
        0 -> { // A braid down the side.
            parts += oval(Offset(cx - headR * 1.0f, headY + headR * 0.6f), headR * 0.28f)
            parts += oval(Offset(cx - headR * 1.08f, headY + headR * 1.08f), headR * 0.22f)
        }
        1 -> parts += Path().apply { // A cap's brim.
            addRect(Rect(cx - headR * 1.5f, headY - headR * 0.3f, cx + headR * 0.5f, headY - headR * 0.02f))
        }
        2 -> parts += oval(Offset(cx, headY - headR * 1.05f), headR * 0.45f) // A bun on top.
        3 -> parts += Path().apply { // A kerchief knotted under the chin.
            moveTo(cx - headR * 1.2f, headY + headR * 0.25f)
            lineTo(cx, headY - headR * 1.25f)
            lineTo(cx + headR * 1.2f, headY + headR * 0.25f)
            lineTo(cx + headR * 0.35f, headY + headR * 1.4f)
            lineTo(cx - headR * 0.35f, headY + headR * 1.4f)
            close()
        }
        4 -> parts += oval(Offset(cx, headY + headR * 0.1f), headR * 1.35f) // A hood.
        else -> {}
    }
    // One outline, so the warm rim goes round the person and not through them.
    val shape = parts.reduce { a, b -> Path().apply { op(a, b, PathOperation.Union) } }
    drawPath(shape, color)
    if (lit != null) drawPath(shape, lit.copy(alpha = 0.75f), style = Stroke(width = w * 0.035f))
}
