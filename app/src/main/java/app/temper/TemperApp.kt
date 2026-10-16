package app.temper

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.temper.data.DemoSeed
import app.temper.data.SettingsStore
import app.temper.data.TemperDatabase
import app.temper.notify.Alarms
import app.temper.widget.ForgeWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(private val context: Context) {
    val db = TemperDatabase.create(context)
    val settings = SettingsStore(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Fire-and-forget so a write survives the screen being closed right after tapping save. */
    fun write(block: suspend TemperDatabase.() -> Unit) {
        scope.launch {
            db.block()
            changed()
        }
    }

    /** Re-arm the reminder and redraw the home-screen widget. */
    suspend fun changed() {
        Alarms.reschedule(context)
        ForgeWidget.refresh(context)
    }
}

class TemperApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Alarms.ensureChannels(this)
        // Re-arm on every start: OEM task killers (HyperOS & co.) silently drop alarms.
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(container.db, container.settings)
            container.changed()
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as TemperApp).container

/** Resources in the in-app language, for services and receivers that AppCompat doesn't localise before Android 13. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}
