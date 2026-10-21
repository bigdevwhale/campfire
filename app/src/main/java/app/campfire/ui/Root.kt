package app.campfire.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.campfire.data.FireSettings
import app.campfire.ui.box.BoxScreen
import app.campfire.ui.fire.FireScreen
import app.campfire.ui.night.NightScreen
import app.campfire.ui.onboarding.Welcome
import app.campfire.ui.settings.SettingsScreen

/** The fire is home; everything else is a short walk away from it and straight back. */
private enum class Place { FIRE, BOX, SETTINGS, NIGHT }

@Composable
fun Root(settings: FireSettings) {
    if (!settings.onboarded) {
        Welcome()
        return
    }
    var place by rememberSaveable { mutableStateOf(Place.FIRE) }
    BackHandler(enabled = place != Place.FIRE) { place = Place.FIRE }
    Crossfade(targetState = place, label = "place") { here ->
        when (here) {
            Place.FIRE -> FireScreen(
                onBox = { place = Place.BOX },
                onSettings = { place = Place.SETTINGS },
                onNight = { place = Place.NIGHT },
            )
            Place.BOX -> BoxScreen(onBack = { place = Place.FIRE })
            Place.SETTINGS -> SettingsScreen(onBack = { place = Place.FIRE })
            Place.NIGHT -> NightScreen(onLeave = { place = Place.FIRE })
        }
    }
}
