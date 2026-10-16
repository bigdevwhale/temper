package app.temper.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Hardware
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.temper.BuildConfig
import app.temper.R
import app.temper.container
import app.temper.data.Kind
import app.temper.data.Program
import app.temper.data.Session
import app.temper.data.Settings
import app.temper.domain.Badge
import app.temper.domain.Badges
import app.temper.domain.Levels
import app.temper.domain.Stats
import app.temper.domain.TrialState
import app.temper.domain.Trials
import app.temper.notify.Alarms
import app.temper.run.Engine
import app.temper.ui.blade.BladeTab
import app.temper.ui.blade.JournalScreen
import app.temper.ui.blade.SessionEditScreen
import app.temper.ui.components.CelebrateDialog
import app.temper.ui.components.rememberNow
import app.temper.ui.forge.ForgeTab
import app.temper.ui.forge.TrialsScreen
import app.temper.ui.learn.ArticleScreen
import app.temper.ui.learn.LearnTab
import app.temper.ui.me.MeTab
import app.temper.ui.me.SettingsScreen
import app.temper.ui.onboarding.OnboardingScreen
import app.temper.ui.session.FinishScreen
import app.temper.ui.session.ProgramScreen
import app.temper.ui.session.RunScreen
import app.temper.ui.session.SetupScreen
import app.temper.ui.theme.Inter
import app.temper.ui.theme.U
import app.temper.ui.theme.temperColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId

enum class Tab { FORGE, BLADE, LEARN, ME }

/** Everything stored, plus what is derived from it, recomputed once per minute or change. */
@Immutable
class TemperData(
    val sessions: List<Session>,
    val programs: List<Program>,
    val settings: Settings,
    val now: Long,
) {
    val stats = Stats(sessions, now, ZoneId.systemDefault(), settings.coldGoal, settings.heatGoal)
    val level = Levels.level(stats.xp)
    val badges: List<Badge> = Badges.unlocked(stats, settings)
    val trial: TrialState? = Trials.state(settings, stats)
}

/** Everything a tab can open. */
class Go(private val nav: NavHostController) {
    fun setup(kind: Kind) = nav.navigate("setup/${kind.name}")
    fun run() = nav.navigate("run") { launchSingleTop = true }
    fun program(kind: Kind, id: Long = 0) = nav.navigate("program/${kind.name}/$id")
    fun session(id: Long) = nav.navigate("session/$id")
    fun journal() = nav.navigate("journal")
    fun trials() = nav.navigate("trials")
    fun article(id: String) = nav.navigate("article/$id")
    fun settings() = nav.navigate("settings")
    fun back() = nav.popBackStack()
}

@Composable
fun TemperRoot(openRoute: MutableStateFlow<String?>, settings: Settings) {
    val context = LocalContext.current
    val container = context.container
    val nav = rememberNavController()
    val go = remember(nav) { Go(nav) }
    var tab by rememberSaveable { mutableStateOf(Tab.FORGE) }
    val start = remember { if (settings.onboarded) "main" else "welcome" }

    val dao = container.db.dao()
    val sessions by dao.observeSessions().collectAsStateWithLifecycle(initialValue = null)
    val programs by dao.observePrograms().collectAsStateWithLifecycle(initialValue = emptyList())
    val minute = rememberNow(60_000)
    val data = remember(sessions, programs, settings, minute) {
        TemperData(sessions.orEmpty(), programs, settings, System.currentTimeMillis())
    }

    val run by Engine.run.collectAsStateWithLifecycle()
    val finished by Engine.finished.collectAsStateWithLifecycle()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    // A session in progress always comes to the front; a finished one opens its summary.
    LaunchedEffect(run != null, route) {
        if (run != null && route != null && route != "run" && settings.onboarded) go.run()
    }
    LaunchedEffect(finished) {
        val id = finished ?: return@LaunchedEffect
        nav.navigate("finish/$id") { popUpTo("main") }
        Engine.finished.value = null
    }

    // Onboarded elsewhere (a restored backup, the demo seed): leave the welcome screens.
    LaunchedEffect(settings.onboarded, route) {
        if (settings.onboarded && route == "welcome") nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
    }

    val requested by openRoute.collectAsStateWithLifecycle()
    LaunchedEffect(requested, settings.onboarded) {
        if (requested == null || !settings.onboarded) return@LaunchedEffect
        if (Engine.run.value == null && route == "run") nav.popBackStack("main", inclusive = false)
        openRoute.value = null
    }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        container.scope.launch { container.changed() }
    }
    LaunchedEffect(settings.onboarded) {
        if (settings.onboarded && Build.VERSION.SDK_INT >= 33 && !Alarms.canNotify(context) && !BuildConfig.DEMO) {
            askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    NavHost(
        navController = nav,
        startDestination = start,
        modifier = Modifier.background(U.c.bg),
        enterTransition = { fadeIn(tween(240)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(240)) },
        popExitTransition = { fadeOut(tween(180)) },
    ) {
        composable("welcome") {
            OnboardingScreen { nav.navigate("main") { popUpTo("welcome") { inclusive = true } } }
        }
        composable("main") { MainTabs(tab, { tab = it }, go, data) }
        composable("setup/{kind}", arguments = listOf(navArgument("kind") { type = NavType.StringType })) { e ->
            SetupScreen(Kind.valueOf(e.arguments?.getString("kind") ?: Kind.COLD.name), data, go)
        }
        composable("run") {
            val r = run
            val saving by Engine.saving.collectAsStateWithLifecycle()
            // Discarded or too short to keep: back out, but never pop the finish screen that replaced this one.
            if (r == null) LaunchedEffect(saving) {
                if (!saving && Engine.finished.value == null && nav.currentDestination?.route == "run") nav.popBackStack()
            }
            else RunScreen(r, data.settings, onLeave = { go.back() })
        }
        composable("finish/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            FinishScreen(e.arguments?.getLong("id") ?: 0L, data, onDone = { go.back() })
        }
        composable("program/{kind}/{id}", arguments = listOf(navArgument("kind") { type = NavType.StringType }, navArgument("id") { type = NavType.LongType })) { e ->
            ProgramScreen(Kind.valueOf(e.arguments?.getString("kind") ?: Kind.CONTRAST.name), e.arguments?.getLong("id") ?: 0L, onDone = { go.back() })
        }
        composable("session/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            SessionEditScreen(e.arguments?.getLong("id") ?: 0L, data, onDone = { go.back() })
        }
        composable("journal") { JournalScreen(data, go) }
        composable("trials") { TrialsScreen(data, onBack = { go.back() }) }
        composable("article/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
            ArticleScreen(e.arguments?.getString("id").orEmpty(), go)
        }
        composable("settings") { SettingsScreen(settings, onBack = { go.back() }) }
    }

    if (settings.onboarded && sessions != null && run == null && route != "run" && route != "welcome" && route?.startsWith("finish") != true) Celebrations(data)
}

/** New marks, a new temper and a finished trial, each celebrated once. */
@Composable
private fun Celebrations(data: TemperData) {
    val container = LocalContext.current.container
    val s = data.settings
    val fresh = data.badges.filter { it.id !in s.seenBadges }
    val trial = data.trial
    when {
        trial != null && trial.finished -> CelebrateDialog(
            icon = trialIcon(trial.trial), color = trialColor(trial.trial).first, soft = trialColor(trial.trial).second,
            eyebrow = stringResource(R.string.trial_done),
            title = trialTitle(trial.trial),
            text = stringResource(R.string.trial_done_text),
        ) { container.scope.launch { container.settings.update { it.copy(trial = "", trialsDone = it.trialsDone + trial.trial.id) } } }
        fresh.isNotEmpty() -> {
            val b = fresh.last()
            val (color, soft) = badgeColor(b.id)
            CelebrateDialog(
                icon = badgeIcon(b.id), color = color, soft = soft,
                eyebrow = stringResource(R.string.mark_unlocked),
                title = badgeTitle(b.id),
                text = badgeText(b.id) + if (fresh.size > 1) "\n" + stringResource(R.string.mark_more, fresh.size - 1) else "",
            ) { container.scope.launch { container.settings.update { it.copy(seenBadges = it.seenBadges + fresh.map { f -> f.id }) } } }
        }
        data.level > s.levelSeen -> CelebrateDialog(
            icon = Icons.Rounded.LocalFireDepartment, color = temperColor(data.level), soft = U.c.surface2,
            eyebrow = stringResource(R.string.level_up),
            title = levelName(data.level),
            text = stringResource(R.string.level_up_text),
        ) { container.scope.launch { container.settings.update { it.copy(levelSeen = data.level) } } }
    }
}

@Composable
private fun MainTabs(tab: Tab, onTab: (Tab) -> Unit, go: Go, data: TemperData) {
    Column(Modifier.fillMaxSize().background(U.c.bg)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.FORGE -> ForgeTab(data, go, onTab)
                Tab.BLADE -> BladeTab(data, go)
                Tab.LEARN -> LearnTab(go)
                Tab.ME -> MeTab(data, go)
            }
        }
        HorizontalDivider(color = U.c.line)
        Row(
            Modifier.fillMaxWidth().background(U.c.surface).navigationBarsPadding().height(66.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Icons.Rounded.LocalFireDepartment, stringResource(R.string.tab_forge), tab == Tab.FORGE) { onTab(Tab.FORGE) }
            NavItem(Icons.Rounded.Hardware, stringResource(R.string.tab_blade), tab == Tab.BLADE) { onTab(Tab.BLADE) }
            NavItem(Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.tab_learn), tab == Tab.LEARN) { onTab(Tab.LEARN) }
            NavItem(Icons.Rounded.Person, stringResource(R.string.tab_me), tab == Tab.ME) { onTab(Tab.ME) }
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) U.c.accent else U.c.faint
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(if (selected) U.c.accentSoft else Color.Transparent).padding(horizontal = 18.dp, vertical = 4.dp),
        ) { Icon(icon, null, Modifier.size(22.dp), tint = color) }
        Spacer(Modifier.height(3.dp))
        Text(label, fontFamily = Inter, fontSize = 11.5.sp, color = color, maxLines = 1)
    }
}
