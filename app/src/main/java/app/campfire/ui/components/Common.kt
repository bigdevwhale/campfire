package app.campfire.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.R
import app.campfire.data.Seat
import app.campfire.ui.fire.drawSilhouette
import app.campfire.ui.theme.LocalPalette
import app.campfire.ui.theme.LocalType

/** A quiet top bar for the screens away from the fire: back, and a serif title. */
@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), tint = p.muted) }
        Text(title, style = LocalType.current.title, color = p.ink, modifier = Modifier.padding(start = 4.dp))
    }
}

/** A seated head on its stone, for lists and tickets. */
@Composable
fun Head(glyph: Int, size: Dp, ink: Color? = null, onClick: (() -> Unit)? = null, label: String? = null) {
    val p = LocalPalette.current
    Canvas(
        Modifier
            .size(size)
            .clip(CircleShape)
            .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }
            .let { m -> if (label != null) m.semantics { contentDescription = label } else m },
    ) {
        drawCircle(Brush.verticalGradient(listOf(Color(0xFF3A3E45), Color(0xFF1E2126))))
        inset(this.size.width * 0.12f) { drawSilhouette(glyph, ink ?: p.muted) }
    }
}

/** One stone being named: tap the head to change it, type the name, cross it out to remove it. */
@Composable
fun SeatRow(seat: Seat, onChange: (Seat) -> Unit, onRemove: (() -> Unit)?) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Head(seat.glyph, 52.dp, ink = p.ink, onClick = { onChange(seat.copy(glyph = (seat.glyph + 1) % Seat.GLYPHS)) }, label = stringResource(R.string.tap_head))
        Spacer(Modifier.width(14.dp))
        OutlinedTextField(
            value = seat.name,
            onValueChange = { onChange(seat.copy(name = it.take(20))) },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.seat_name), color = p.faint) },
            textStyle = LocalType.current.body.copy(color = p.ink),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = p.flame, unfocusedBorderColor = p.line, cursorColor = p.flame),
            modifier = Modifier.weight(1f),
        )
        if (onRemove != null) {
            IconButton(onClick = onRemove) { Icon(Icons.Outlined.Close, stringResource(R.string.delete), tint = p.faint) }
        } else {
            Spacer(Modifier.width(48.dp))
        }
    }
}

/** The one warm button the app has: a pill of firelight. */
@Composable
fun FireButton(text: String, enabled: Boolean = true, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(if (enabled) p.flame else p.surface2)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = LocalType.current.body, color = if (enabled) Color(0xFF231509) else p.faint)
    }
}

/** A small rounded choice, for filters and the language. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) p.surface2 else Color.Transparent)
            .border(1.dp, if (selected) p.flame else p.line, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, style = LocalType.current.label, color = if (selected) p.ink else p.muted)
    }
}

@Composable
fun SectionTitle(text: String, sub: String? = null) {
    val p = LocalPalette.current
    Text(text, style = LocalType.current.question.copy(fontSize = LocalType.current.body.fontSize * 1.3f), color = p.ink, modifier = Modifier.padding(top = 20.dp))
    if (sub != null) Text(sub, style = LocalType.current.label, color = p.muted, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
}

val Gap = Arrangement.spacedBy(10.dp)

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))
