package app.temper

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.temper.run.Engine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.temper.data.Settings
import app.temper.notify.Alarms
import app.temper.ui.TemperRoot
import app.temper.ui.theme.TemperTheme
import app.temper.ui.theme.isDark
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : AppCompatActivity() {
    /** Screen requested from outside: a notification or the widget. */
    private val openRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A session in progress keeps the screen awake while the app is in front.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Engine.run.map { it != null }.distinctUntilChanged().collect { running ->
                    if (running) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
        if (savedInstanceState == null) handle(intent)
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val dark = isDark((settings ?: Settings()).theme)
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            TemperTheme(dark) {
                // Wait for settings so the theme doesn't flash light-then-dark.
                if (settings != null) TemperRoot(openRoute = openRoute, settings = settings!!)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent?.getStringExtra(Alarms.EXTRA_ROUTE)?.let { openRoute.value = it }
    }
}
