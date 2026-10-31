package app.tend

import android.app.AppOpsManager
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.tend.core.TaskDef
import app.tend.core.Tasks
import app.tend.health.HealthConnect
import app.tend.health.TaskSync
import app.tend.ui.Celebration
import androidx.health.connect.client.PermissionController
import app.tend.data.TendStore
import app.tend.monitor.MonitorService
import app.tend.ui.AppCatalog
import app.tend.ui.FocusTimerScreen
import app.tend.ui.HomeScreen
import app.tend.ui.Onboarding
import app.tend.ui.PermissionActions
import app.tend.ui.Permissions
import app.tend.ui.PermissionsScreen
import app.tend.ui.SettingsScreen
import app.tend.ui.TendTheme
import app.tend.widget.TendWidget
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val store by lazy { TendStore(this) }
    private val handler = Handler(Looper.getMainLooper())

    /** Bumped whenever we come back to the app, so the UI re-reads the store. */
    private var refresh by mutableIntStateOf(0)

    /** The user went to a settings page and came back without the permission. */
    private var usageAttempted = false
    private var overlayAttempted = false

    private enum class Screen { HOME, SETTINGS, PERMISSIONS }

    private val healthPermissions =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { refresh++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val catalog = AppCatalog(this)
        if (!store.onboarded && store.blockedPackages.isEmpty()) {
            store.blockedPackages = catalog.suggested.map { it.pkg }.toSet()
        }
        setContent {
            TendTheme {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) { Content(catalog) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacksAndMessages(null)
        refresh++
        if (store.onboarded && permissions().allGranted) MonitorService.start(this)
    }

    @Composable
    private fun Content(catalog: AppCatalog) {
        refresh // read so recomposition follows onResume
        var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var health by remember { mutableStateOf<HealthConnect.Status?>(null) }
        var celebrate by remember { mutableStateOf<List<TaskDef>>(emptyList()) }
        var synced by remember { mutableIntStateOf(0) }
        synced // read so recomposition follows a sync

        suspend fun sync() {
            health = HealthConnect(this@MainActivity).status()
            val done = TaskSync.sync(this@MainActivity)
            if (done.isNotEmpty()) celebrate = celebrate + done
            synced++
        }
        LaunchedEffect(refresh) { if (store.onboarded) sync() }
        LaunchedEffect(Unit) {
            var seconds = 0
            while (true) {
                delay(1000)
                now = System.currentTimeMillis()
                // Steps every minute while tend is open; the reading timer the moment it ends.
                if (store.onboarded && (++seconds % 60 == 0 || TaskSync.focusSecondsLeft(store, now) == 0L)) sync()
            }
        }
        if (celebrate.isNotEmpty()) Celebration(store.avatar, celebrate) { celebrate = emptyList() }

        val toggleApp: (String) -> Unit = { pkg ->
            val b = store.blockedPackages
            store.blockedPackages = if (pkg in b) b - pkg else b + pkg
            refresh++
        }

        when {
            !store.onboarded -> Onboarding(
                catalog = catalog,
                avatar = store.avatar,
                cfg = store.config,
                blocked = store.blockedPackages,
                perms = permissionActions(),
                onAvatar = { store.avatar = it; refresh++ },
                onConfig = { store.config = it; refresh++ },
                onToggleApp = toggleApp,
                onAddWidget = widgetPinner(),
                onFinish = { store.onboarded = true; MonitorService.start(this); refresh++ },
            )

            screen == Screen.SETTINGS -> SettingsScreen(
                catalog = catalog,
                avatar = store.avatar,
                cfg = store.config,
                blocked = store.blockedPackages,
                onAvatar = { store.avatar = it; refresh++ },
                onConfig = { store.config = it; refresh++ },
                onToggleApp = toggleApp,
                onDone = { screen = Screen.HOME },
            )

            screen == Screen.PERMISSIONS -> PermissionsScreen(store.avatar, permissionActions()) {
                MonitorService.start(this)
                screen = Screen.HOME
            }

            (TaskSync.focusSecondsLeft(store, now) ?: 0) > 0 -> FocusTimerScreen(
                store.avatar, Tasks.READ, TaskSync.focusSecondsLeft(store, now)!!,
                onCancel = { store.focusStartedAt = 0; refresh++ },
            )

            else -> HomeScreen(
                avatar = store.avatar,
                cfg = store.config,
                day = store.today(),
                gardenMinutes = store.gardenMinutes,
                missCount = store.missCount(),
                permissionsMissing = !permissions().allGranted,
                health = health,
                focusSecondsLeft = TaskSync.focusSecondsLeft(store, now),
                onFixPermissions = { screen = Screen.PERMISSIONS },
                onConnectHealth = {
                    if (health == HealthConnect.Status.NEEDS_INSTALL) {
                        open(
                            HealthConnect.installIntent(),
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${HealthConnect.PROVIDER}")),
                        )
                    } else {
                        healthPermissions.launch(HealthConnect.PERMISSIONS)
                    }
                },
                onStartFocus = {
                    if (store.focusStartedAt == 0L) store.focusStartedAt = System.currentTimeMillis()
                    refresh++
                },
                onOpenSettings = { screen = Screen.SETTINGS },
            )
        }
    }

    private fun permissionActions(): PermissionActions {
        val p = permissions()
        return PermissionActions(
            permissions = p,
            usageBlocked = usageAttempted && !p.usageAccess,
            overlayBlocked = overlayAttempted && !p.overlay,
            requestUsage = {
                usageAttempted = true
                returnWhenGranted { permissions().usageAccess }
                // Straight to tend's own switch where the phone supports it, else the list.
                open(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName")), Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            },
            requestOverlay = {
                overlayAttempted = true
                returnWhenGranted { Settings.canDrawOverlays(this) }
                open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            },
            openAppInfo = { open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) },
        )
    }

    /**
     * While the user is on a system settings page, watch for the switch and bring tend back
     * as soon as it flips, so they don't have to find their way back.
     */
    private fun returnWhenGranted(granted: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 120_000
        handler.removeCallbacksAndMessages(null)
        handler.post(object : Runnable {
            override fun run() = when {
                granted() -> startActivity(
                    Intent(this@MainActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                )
                SystemClock.uptimeMillis() < deadline -> { handler.postDelayed(this, 500); Unit }
                else -> Unit
            }
        })
    }

    private fun open(vararg intents: Intent) {
        for (intent in intents) {
            try { startActivity(intent); return } catch (_: ActivityNotFoundException) { }
        }
    }

    /** Null when the launcher can't pin widgets from inside the app. */
    private fun widgetPinner(): (() -> Unit)? {
        val manager = AppWidgetManager.getInstance(this)
        if (!manager.isRequestPinAppWidgetSupported) return null
        return { manager.requestPinAppWidget(ComponentName(this, TendWidget::class.java), null, null) }
    }

    private fun permissions(): Permissions {
        val ops = getSystemService(AppOpsManager::class.java)
        val usage = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        } == AppOpsManager.MODE_ALLOWED
        return Permissions(usage, Settings.canDrawOverlays(this))
    }
}
