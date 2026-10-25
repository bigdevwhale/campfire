package app.campfire.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.campfire.BuildConfig
import app.campfire.R
import app.campfire.container
import app.campfire.data.Seat
import app.campfire.ui.components.Chip
import app.campfire.ui.components.CommunityLinks
import app.campfire.ui.components.SeatRow
import app.campfire.ui.components.SectionTitle
import app.campfire.ui.components.TopBar
import app.campfire.ui.theme.LocalPalette
import app.campfire.ui.theme.LocalType
import kotlinx.coroutines.delay

/** The stones by the fire, the language, and a word about the app. That is all there is to set. */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val p = LocalPalette.current
    val type = LocalType.current
    val saved by container.db.dao().seats().collectAsState(initial = emptyList())
    // Names being typed, kept here and written a moment after the typing stops.
    val editing = remember { mutableStateMapOf<Long, Seat>() }
    var removing by remember { mutableStateOf<Seat?>(null) }

    LaunchedEffect(editing.toMap()) {
        delay(400)
        editing.values.filter { it.name.isNotBlank() }.forEach { s -> container.write { db.dao().updateSeat(s.copy(name = s.name.trim())) } }
    }

    Column(Modifier.fillMaxSize().background(p.bg)) {
        TopBar(stringResource(R.string.settings), onBack)
        Column(
            Modifier.verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(horizontal = 20.dp),
        ) {
            SectionTitle(stringResource(R.string.seats), stringResource(R.string.seats_sub))
            saved.forEach { stored ->
                val seat = editing[stored.id] ?: stored
                SeatRow(seat, onChange = { editing[stored.id] = it }, onRemove = { removing = stored })
            }
            if (saved.size < 6) {
                TextButton(onClick = {
                    container.write { db.dao().addSeat(Seat(name = "", glyph = (saved.size * 2 + 1) % Seat.GLYPHS)) }
                }) { Text("+ " + stringResource(R.string.add_seat), style = type.body, color = p.flame) }
            } else {
                Text(stringResource(R.string.seats_full), style = type.label, color = p.faint, modifier = Modifier.padding(vertical = 8.dp))
            }

            SectionTitle(stringResource(R.string.language))
            val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                listOf("" to R.string.system, "ru" to R.string.lang_ru, "en" to R.string.lang_en).forEach { (tag, label) ->
                    Chip(stringResource(label), selected = current.startsWith(tag) && (tag.isNotEmpty() || current.isEmpty())) {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                    }
                }
            }

            SectionTitle(stringResource(R.string.community), stringResource(R.string.community_text))
            CommunityLinks()

            SectionTitle(stringResource(R.string.about))
            Text(stringResource(R.string.about_line), style = type.body, color = p.muted, modifier = Modifier.padding(top = 6.dp))
            Text("v${BuildConfig.VERSION_NAME}", style = type.label, color = p.faint, modifier = Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(32.dp))
        }
    }

    removing?.let { seat ->
        AlertDialog(
            onDismissRequest = { removing = null },
            text = { Text(stringResource(R.string.remove_seat, seat.name), style = type.body) },
            confirmButton = {
                TextButton(onClick = {
                    editing.remove(seat.id)
                    container.write { db.dao().removeSeat(seat) }
                    removing = null
                }) { Text(stringResource(R.string.delete), color = p.danger) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(R.string.cancel), color = p.muted) } },
            containerColor = p.surface,
        )
    }
}
