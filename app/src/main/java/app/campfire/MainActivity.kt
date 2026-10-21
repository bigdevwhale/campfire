package app.campfire

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.campfire.data.FireSettings
import app.campfire.ui.Root
import app.campfire.ui.theme.CampfireTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A dark clearing: light icons on dark bars.
        val style = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        setContent {
            val settings: FireSettings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            CampfireTheme {
                // Wait for settings so the welcome doesn't flash for someone already here.
                settings?.let { Root(it) }
            }
        }
    }
}
