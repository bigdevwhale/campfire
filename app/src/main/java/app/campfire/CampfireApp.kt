package app.campfire

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.campfire.data.CampfireDatabase
import app.campfire.data.DemoSeed
import app.campfire.data.SettingsStore
import app.campfire.sound.Recorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(val context: Context) {
    val db = CampfireDatabase.create(context)
    val settings = SettingsStore(context)
    val recorder = Recorder(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }
}

class CampfireApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scope.launch {
            DemoSeed.seedIfEmpty(container.db, container.settings, ru)
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as CampfireApp).container

/** Resources in the in-app language, if the app speaks one on this Android. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}

/** Is this context in Russian? The deck holds both languages and picks by this. */
val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
