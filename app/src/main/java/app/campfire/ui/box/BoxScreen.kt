package app.campfire.ui.box

import android.media.MediaPlayer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.campfire.R
import app.campfire.container
import app.campfire.data.Evening
import app.campfire.domain.yearAgo
import app.campfire.ui.components.Chip
import app.campfire.ui.components.Head
import app.campfire.ui.components.TopBar
import app.campfire.ui.theme.LocalPalette
import app.campfire.ui.theme.LocalType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The box of tickets: one per evening, newest on top. Pick a person and the box shows only the
 * evenings with them, and above them the line of words you wrote into the ash together.
 */
@Composable
fun BoxScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val p = LocalPalette.current
    val type = LocalType.current
    val all by container.db.dao().evenings().collectAsState(initial = null)
    var who by remember { mutableStateOf<String?>(null) }
    var throwing by remember { mutableStateOf<Evening?>(null) }

    // One cassette plays at a time; leaving the box stops it.
    var playing by remember { mutableStateOf<Long?>(null) }
    val player = remember { arrayOfNulls<MediaPlayer>(1) }
    fun stop() {
        player[0]?.release()
        player[0] = null
        playing = null
    }
    fun play(e: Evening) {
        stop()
        val file = container.recorder.file(e.recording ?: return)
        if (!file.exists()) return
        val mp = MediaPlayer()
        try {
            mp.setDataSource(file.absolutePath)
            mp.setOnCompletionListener { stop() }
            mp.prepare()
            mp.start()
        } catch (_: Exception) {
            // A take cut short by a dying battery can't be played; the ticket stays.
            mp.release()
            return
        }
        player[0] = mp
        playing = e.id
    }
    DisposableEffect(Unit) { onDispose { stop() } }

    Column(Modifier.fillMaxSize().background(p.bg)) {
        TopBar(stringResource(R.string.box_title), onBack)
        val evenings = all ?: return@Column
        if (evenings.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.box_empty), style = type.question.copy(fontSize = 20.sp), color = p.faint, textAlign = TextAlign.Center)
            }
            return@Column
        }
        val people = evenings.flatMap { it.names }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }
        val shown = if (who == null) evenings else evenings.filter { who in it.names }
        val remembered = yearAgo(evenings, LocalDate.now()) { it.day }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.navigationBarsPadding(),
        ) {
            if (people.size > 1) item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Chip(stringResource(R.string.everyone), who == null) { who = null } }
                    items(people) { name -> Chip(name, who == name) { who = if (who == name) null else name } }
                }
            }
            who?.let { name ->
                val words = shown.map { it.word }.filter { it.isNotBlank() }
                if (words.isNotEmpty()) item {
                    Column(Modifier.padding(horizontal = 4.dp)) {
                        Text(stringResource(R.string.words_of, name), style = type.label, color = p.muted)
                        Spacer(Modifier.height(6.dp))
                        // Oldest first: the words read like a path walked together.
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            words.asReversed().forEach { Text(it, style = type.question.copy(fontSize = 19.sp), color = p.flameCore) }
                        }
                    }
                }
            }
            if (who == null && remembered != null) item { Recall(remembered) }
            items(shown, key = { it.id }) { e ->
                Ticket(
                    e,
                    playing = playing == e.id,
                    onPlay = { if (playing == e.id) stop() else play(e) },
                    onLongPress = { throwing = e },
                )
            }
        }
    }

    throwing?.let { e ->
        AlertDialog(
            onDismissRequest = { throwing = null },
            text = { Text(stringResource(R.string.delete_evening), style = type.body) },
            confirmButton = {
                TextButton(onClick = {
                    if (playing == e.id) stop()
                    container.write {
                        e.recording?.let { recorder.file(it).delete() }
                        db.dao().removeEvening(e)
                    }
                    throwing = null
                }) { Text(stringResource(R.string.delete), color = p.danger) }
            },
            dismissButton = { TextButton(onClick = { throwing = null }) { Text(stringResource(R.string.cancel), color = p.muted) } },
            containerColor = p.surface,
        )
    }
}

/** A year ago by this fire: the fire brings the old ticket's word back. */
@Composable
private fun Recall(e: Evening) {
    val p = LocalPalette.current
    val type = LocalType.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.surface2).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.recall_title), style = type.label, color = p.flame)
        Spacer(Modifier.height(6.dp))
        Text(e.word.ifBlank { "·" }, style = type.title, color = p.ink)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.recall_sub) + " · " + e.names.joinToString(" · "), style = type.label, color = p.muted, textAlign = TextAlign.Center)
    }
}

/** A ticket shape: torn-off notches either side where the stub would be. */
private class TicketOutline(private val notchY: Float, private val notchR: Float, private val corner: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val body = Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, androidx.compose.ui.geometry.CornerRadius(corner)))
        }
        val notches = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(Offset(0f, notchY), notchR))
            addOval(androidx.compose.ui.geometry.Rect(Offset(size.width, notchY), notchR))
        }
        return Outline.Generic(Path().apply { op(body, notches, PathOperation.Difference) })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Ticket(e: Evening, playing: Boolean, onPlay: () -> Unit, onLongPress: () -> Unit) {
    val p = LocalPalette.current
    val type = LocalType.current
    val date = remember(e.endedAt) { e.day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) }
    val stubH = 46.dp
    Column(
        Modifier
            .fillMaxWidth()
            .clip(TicketOutline(notchY = with(androidx.compose.ui.platform.LocalDensity.current) { stubH.toPx() }, notchR = 22f, corner = 14f))
            .background(p.paper)
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .drawBehind {
                // The perforation between the stub and the ticket.
                drawLine(
                    p.paperInk.copy(alpha = 0.3f),
                    Offset(30f, stubH.toPx()),
                    Offset(size.width - 30f, stubH.toPx()),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                )
            },
    ) {
        // The stub: date and how long the fire burned.
        Row(Modifier.fillMaxWidth().height(stubH).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(date, style = type.label.copy(fontSize = 13.sp), color = p.paperInk.copy(alpha = 0.75f), modifier = Modifier.weight(1f))
            Text(stringResource(R.string.minutes, e.minutes), style = type.label, color = p.paperInk.copy(alpha = 0.55f))
        }
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 18.dp)) {
            Text(
                e.word.ifBlank { "·" },
                style = type.title.copy(fontSize = 32.sp),
                color = p.paperInk,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            // Who sat by the fire: their heads as they were that night.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                e.glyphs.forEach { Head(it, 26.dp, ink = p.paper) }
                Spacer(Modifier.width(4.dp))
                Text(e.names.joinToString(" · "), style = type.body, color = p.paperInk, modifier = Modifier.weight(1f))
            }
            if (e.questionList.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                e.questionList.forEach {
                    Text("— $it", style = type.body.copy(fontStyle = FontStyle.Italic, fontSize = 14.sp), color = p.paperInk.copy(alpha = 0.7f), modifier = Modifier.padding(vertical = 1.dp))
                }
            }
            if (e.recording != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(18.dp)).background(p.paperInk.copy(alpha = 0.08f)).clickable(onClick = onPlay).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (playing) Icons.Outlined.Stop else Icons.Outlined.PlayArrow, null, tint = p.paperInk)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(if (playing) R.string.stop else R.string.play), style = type.label, color = p.paperInk)
                }
            }
        }
    }
}
