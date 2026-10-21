package app.campfire.ui.fire

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import app.campfire.domain.Stage
import app.campfire.ui.theme.LocalPalette
import app.campfire.ui.theme.Palette
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Where things sit in the clearing, as fractions of the screen, shared by the art and the stones. */
object Clearing {
    const val HORIZON = 0.47f
    /** The foot of the fire, where the logs cross. */
    const val FIRE = 0.6f
    /** The ellipse the stones sit on, around the fire's foot. */
    const val RING_X = 0.36f
    const val RING_Y = 0.19f
}

/** What the fire is doing, for the art: nothing yet, burning at a stage, smoking out, or held to cover. */
enum class Look { COLD, BURNING, SMOKE }

/**
 * The whole clearing, painted in code: night sky with slow stars, dark grass, and a fire that lives
 * through the evening — flaring, settling, breaking into embers, then smoke. Nothing is an asset;
 * every wobble is a sine of [time]. While the cassette listens, [voice] lets the flames lean into
 * the voices. [hold] is a ring of intent (0..1): two stones lighting it, or two palms covering it.
 */
@Composable
fun FireArt(
    look: Look,
    stage: Stage,
    time: Float,
    voice: Float,
    hold: Float,
    holdIsCover: Boolean,
    modifier: Modifier = Modifier,
    dim: Float = 1f,
) {
    val p = LocalPalette.current
    val stars = remember {
        val r = Random(7)
        List(56) { Triple(r.nextFloat(), r.nextFloat(), 0.5f + r.nextFloat()) }
    }
    val trees = remember {
        val r = Random(11)
        List(14) { i -> (i / 13f) to (0.04f + r.nextFloat() * 0.07f) }
    }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val horizon = h * Clearing.HORIZON
        val cx = w / 2f
        val base = h * Clearing.FIRE
        // The fire is the whole point of the screen: everything about it is drawn a size up.
        val unit = w / 100f * 1.25f
        val glow = when {
            look != Look.BURNING -> 0f
            stage == Stage.FLAME -> 1f
            stage == Stage.LOGS -> 0.7f
            else -> 0.4f
        } * dim

        // Sky, stars, and a dark line of forest on the horizon.
        drawRect(Brush.verticalGradient(listOf(p.skyTop, p.skyBottom), endY = horizon), size = Size(w, horizon))
        stars.forEach { (fx, fy, r) ->
            val twinkle = 0.35f + 0.65f * abs(sin(time * (0.25f + r * 0.2f) + fx * 31f))
            // Fewer stars near the forest, where the haze is.
            val y = fy * fy * horizon * 0.92f
            drawCircle(p.ink.copy(alpha = 0.45f * twinkle * dim), radius = r * unit * 0.22f, center = Offset(fx * w, y))
        }
        val forest = Path().apply {
            moveTo(0f, horizon)
            trees.forEach { (fx, th) ->
                val x = fx * w
                lineTo(x - w * 0.035f, horizon - h * th * 0.35f)
                lineTo(x, horizon - h * th)
                lineTo(x + w * 0.035f, horizon - h * th * 0.35f)
            }
            lineTo(w, horizon)
            close()
        }
        drawPath(forest, Color(0xFF070A0D))


        // Ground: as dark as the forest where they meet, so the trees stand on it rather than float.
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF070A0D), p.ground, p.ground2), startY = horizon, endY = h),
            topLeft = Offset(0f, horizon),
            size = Size(w, h - horizon),
        )
        // A low mist along the treeline.
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, p.skyBottom.copy(alpha = 0.55f), Color.Transparent), startY = horizon - h * 0.05f, endY = horizon + h * 0.04f),
            topLeft = Offset(0f, horizon - h * 0.05f),
            size = Size(w, h * 0.09f),
        )
        // Firelight spilling over the grass.
        if (glow > 0f) {
            val r = w * (0.35f + 0.2f * glow) * (1f + 0.03f * sin(time * 2.3f) + voice * 0.08f)
            // A round glow squashed onto the ground, so it fades out evenly with no edge.
            scale(1f, 0.45f, pivot = Offset(cx, base)) {
                drawCircle(
                    Brush.radialGradient(listOf(p.flame.copy(alpha = 0.24f * glow), p.flame.copy(alpha = 0.08f * glow), Color.Transparent), center = Offset(cx, base), radius = r),
                    radius = r,
                    center = Offset(cx, base),
                )
            }
            // A faint warmth on the sky too, over the trees.
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, p.flameDeep.copy(alpha = 0.07f * glow)), startY = horizon * 0.5f, endY = horizon),
                size = Size(w, horizon),
            )
        }

        // The ring of stones' little shadows is drawn by the stones themselves; here, the logs.
        logs(cx, base, unit, look, p)

        when (look) {
            Look.COLD -> ash(cx, base, unit, time, p)
            Look.SMOKE -> smoke(cx, base, unit, time, p)
            Look.BURNING -> burning(stage, cx, base, unit, time, voice, p, dim)
        }

        if (hold > 0f) {
            val r = unit * 17f
            val c = Offset(cx, base - unit * 7f)
            val color = if (holdIsCover) p.smoke else p.flame
            drawCircle(color.copy(alpha = 0.2f), radius = r, center = c, style = Stroke(width = unit * 1.1f))
            drawArc(
                color, startAngle = -90f, sweepAngle = 360f * hold, useCenter = false,
                topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                style = Stroke(width = unit * 1.1f, cap = StrokeCap.Round),
            )
        }
    }
}

private fun DrawScope.logs(cx: Float, base: Float, u: Float, look: Look, p: Palette) {
    val bark = Color(0xFF4A3220)
    val cut = if (look == Look.BURNING) p.ember else Color(0xFF8A6A48)
    val a1 = Offset(cx - u * 15f, base + u * 2.4f)
    val b1 = Offset(cx + u * 13f, base - u * 1.2f)
    val a2 = Offset(cx - u * 13f, base - u * 1.2f)
    val b2 = Offset(cx + u * 15f, base + u * 2.4f)
    drawLine(bark, a1, b1, strokeWidth = u * 3.2f, cap = StrokeCap.Round)
    drawLine(bark, a2, b2, strokeWidth = u * 3.2f, cap = StrokeCap.Round)
    // Cut ends: pale when cold, glowing when the fire has reached them.
    drawCircle(cut, radius = u * 1.3f, center = a1)
    drawCircle(cut, radius = u * 1.3f, center = b2)
    // Stones ringing the fire pit.
    for (i in 0 until 9) {
        val a = Math.PI * (0.05 + 0.9 * i / 8.0)
        val x = cx + cos(a).toFloat() * u * 20f
        val y = base + u * 1f + sin(a).toFloat() * u * 4.5f
        drawOval(Color(0xFF2B2E33), topLeft = Offset(x - u * 2.2f, y - u * 1.2f), size = Size(u * 4.4f, u * 2.4f))
    }
}

private fun DrawScope.burning(stage: Stage, cx: Float, base: Float, u: Float, time: Float, voice: Float, p: Palette, dim: Float) {
    val tall = u * when (stage) {
        Stage.FLAME -> 25f
        Stage.LOGS -> 16f
        Stage.EMBERS -> 6f
    } * (1f + voice * 0.4f)
    val wide = u * when (stage) {
        Stage.FLAME -> 10f
        Stage.LOGS -> 8f
        Stage.EMBERS -> 5f
    }

    // The ember bed always breathes under whatever flame is left.
    for (i in 0 until 9) {
        val fx = cx + (i - 4f) * u * 2.4f
        val g = 0.35f + 0.65f * abs(sin(time * 1.7f + i * 2.1f))
        drawCircle(p.ember.copy(alpha = 0.8f * g * dim), radius = u * (0.6f + 0.45f * g), center = Offset(fx, base + u * 0.6f))
    }

    // Sparks that keep rising while there is heat.
    val sparks = if (stage == Stage.EMBERS) 2 else 5
    repeat(sparks) { i ->
        val life = (time * (0.35f + i * 0.04f) + i * 0.37f) % 1f
        val sx = cx + sin(time * 1.3f + i * 2.4f) * wide * (0.4f + life)
        val sy = base - tall * (0.5f + life * 1.6f)
        drawCircle(p.flameCore.copy(alpha = (1f - life) * 0.85f * dim), radius = u * 0.45f, center = Offset(sx, sy))
    }

    if (stage == Stage.EMBERS) {
        // One small tongue left over the embers, half asleep.
        val flicker = abs(sin(time * 1.1f))
        if (flicker > 0.2f) tongue(cx, base, wide * 0.6f, tall * (0.7f + flicker), sin(time * 3f) * 0.2f, p.flame.copy(alpha = 0.85f * dim))
        return
    }
    // Three tongues — deep, middle, core — each swaying on its own sine, plus two side licks.
    tongue(cx - wide * 0.95f, base, wide * 0.45f, tall * 0.6f, -0.35f + sin(time * 3.1f + 0.7f) * 0.2f, p.flameDeep.copy(alpha = dim))
    tongue(cx + wide * 1.0f, base, wide * 0.4f, tall * 0.5f, 0.35f + sin(time * 2.8f + 2.1f) * 0.2f, p.flameDeep.copy(alpha = dim))
    tongue(cx, base, wide * 1.0f, tall, sin(time * 2.6f) * 0.12f, p.flameDeep.copy(alpha = dim))
    tongue(cx - wide * 0.35f, base, wide * 0.55f, tall * 0.72f, -0.1f + sin(time * 3.7f + 0.4f) * 0.18f, p.flame.copy(alpha = dim))
    tongue(cx + wide * 0.3f, base, wide * 0.6f, tall * 0.82f, 0.08f + sin(time * 3.4f + 1.2f) * 0.16f, p.flame.copy(alpha = dim))
    tongue(cx, base, wide * 0.42f, tall * 0.45f, sin(time * 4.2f + 2.5f) * 0.2f, p.flameCore.copy(alpha = dim))
}

private fun DrawScope.tongue(cx: Float, base: Float, halfW: Float, tall: Float, sway: Float, color: Color) {
    val tip = Offset(cx + halfW * sway * 2f, base - tall)
    val path = Path().apply {
        moveTo(cx - halfW, base)
        cubicTo(cx - halfW * 1.1f, base - tall * 0.45f, tip.x - halfW * 0.4f, base - tall * 0.6f, tip.x, tip.y)
        cubicTo(tip.x + halfW * 0.4f, base - tall * 0.6f, cx + halfW * 1.1f, base - tall * 0.45f, cx + halfW, base)
        close()
    }
    drawPath(path, color)
}

/** Last night's ash: a soft grey heap with one coal that still remembers being warm. */
private fun DrawScope.ash(cx: Float, base: Float, u: Float, time: Float, p: Palette) {
    drawOval(p.smoke.copy(alpha = 0.55f), topLeft = Offset(cx - u * 12f, base - u * 2.2f), size = Size(u * 24f, u * 4.6f))
    drawOval(Color(0xFF6A6E76).copy(alpha = 0.35f), topLeft = Offset(cx - u * 7f, base - u * 2.8f), size = Size(u * 14f, u * 2.6f))
    val g = 0.3f + 0.3f * abs(sin(time * 0.7f))
    drawCircle(p.ember.copy(alpha = g), radius = u * 0.9f, center = Offset(cx + u * 3f, base - u * 0.6f))
}

private fun DrawScope.smoke(cx: Float, base: Float, u: Float, time: Float, p: Palette) {
    ash(cx, base, u, 0f, p)
    repeat(6) { i ->
        val life = (time * 0.45f + i / 6f) % 1f
        val y = base - u * 3f - life * u * 55f
        val r = u * (2.5f + life * 9f)
        drawCircle(p.smoke.copy(alpha = (1f - life) * 0.5f), radius = r, center = Offset(cx + sin(life * 5f + i) * u * 5f, y))
    }
}
