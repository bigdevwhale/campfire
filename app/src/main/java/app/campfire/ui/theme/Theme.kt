package app.campfire.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.campfire.R

/**
 * A clearing at night: deep blue-black sky, warm firelight on the grass, and paper tickets.
 * Never bright, never blue-white — it is for the last hour of the day.
 */
@Immutable
data class Palette(
    val bg: Color,
    val skyTop: Color,
    val skyBottom: Color,
    val ground: Color,
    val ground2: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val flame: Color,
    val flameDeep: Color,
    val flameCore: Color,
    val ember: Color,
    val smoke: Color,
    val paper: Color,
    val paperInk: Color,
    val danger: Color,
)

val Night = Palette(
    bg = Color(0xFF0A0C12),
    skyTop = Color(0xFF05070D),
    skyBottom = Color(0xFF10141F),
    ground = Color(0xFF121610),
    ground2 = Color(0xFF0D100C),
    surface = Color(0xFF141826),
    surface2 = Color(0xFF1B2030),
    line = Color(0xFF282E42),
    ink = Color(0xFFF2E7D5),
    muted = Color(0xFFB9AE99),
    faint = Color(0xFF7C7566),
    flame = Color(0xFFF2A44A),
    flameDeep = Color(0xFFD96B2F),
    flameCore = Color(0xFFFBE3A3),
    ember = Color(0xFFC2543A),
    smoke = Color(0xFF4A5262),
    paper = Color(0xFFEFE3C8),
    paperInk = Color(0xFF3A3226),
    danger = Color(0xFFD0654F),
)

val LocalPalette = staticCompositionLocalOf { Night }

/** Alegreya for the words the fire speaks, Alegreya Sans for the few buttons there are. */
private val Alegreya = FontFamily(
    Font(R.font.alegreya, FontWeight.Normal),
    Font(R.font.alegreya_italic, FontWeight.Normal, style = FontStyle.Italic),
)

private val AlegreyaSans = FontFamily(
    Font(R.font.alegreya_sans, FontWeight.Normal),
    Font(R.font.alegreya_sans_medium, FontWeight.Medium),
    Font(R.font.alegreya_sans_bold, FontWeight.Bold),
)

@Immutable
data class Type(
    val question: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val paperWord: TextStyle,
)

private val Types = Type(
    question = TextStyle(fontFamily = Alegreya, fontSize = 26.sp, lineHeight = 34.sp),
    title = TextStyle(fontFamily = Alegreya, fontSize = 30.sp, lineHeight = 38.sp),
    body = TextStyle(fontFamily = AlegreyaSans, fontSize = 16.sp, lineHeight = 23.sp),
    label = TextStyle(fontFamily = AlegreyaSans, fontSize = 13.sp, lineHeight = 17.sp, letterSpacing = 0.4.sp),
    paperWord = TextStyle(fontFamily = Alegreya, fontSize = 20.sp, color = Night.paperInk),
)

val LocalType = staticCompositionLocalOf { Types }

/** Tickets in the box: slightly worn corners, like card in a tin. */
val TicketShape = RoundedCornerShape(6.dp)

@Composable
fun CampfireTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPalette provides Night, LocalType provides Types) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Night.flame,
                onPrimary = Color(0xFF231509),
                background = Night.bg,
                surface = Night.surface,
                surfaceVariant = Night.surface2,
                outline = Night.line,
                onBackground = Night.ink,
                onSurface = Night.ink,
                onSurfaceVariant = Night.muted,
            ),
            content = content,
        )
    }
}
