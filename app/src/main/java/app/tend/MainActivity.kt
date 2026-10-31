package app.tend

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import app.tend.core.Tasks
import app.tend.data.TendStore
import app.tend.monitor.MonitorService
import app.tend.ui.FocusTimerScreen
import app.tend.ui.HomeScreen
import app.tend.ui.InstalledApp
import app.tend.ui.Permissions
import app.tend.ui.SetupScreen
import app.tend.ui.TendTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val store by lazy { TendStore(this) }

    /** Bumped whenever we come back to the app, so the UI re-reads the store. */
    private var refresh by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val apps = launchableApps()
        setContent {
            TendTheme {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) { Content(apps) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh++
        if (setupComplete()) MonitorService.start(this)
    }

    @androidx.compose.runtime.Composable
    private fun Content(apps: List<InstalledApp>) {
        refresh // read so recomposition follows onResume
        var showSetup by rememberSaveable { mutableStateOf(!setupComplete()) }
        var focusStartedAt by rememberSaveable { mutableLongStateOf(0L) }
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }

        when {
            showSetup -> SetupScreen(
                avatar = store.avatar,
                cfg = store.config,
                apps = apps,
                blocked = store.blockedPackages,
                permissions = permissions(),
                onAvatar = { store.avatar = it; refresh++ },
                onConfig = { store.config = it; refresh++ },
                onToggleApp = { pkg ->
                    val b = store.blockedPackages
                    store.blockedPackages = if (pkg in b) b - pkg else b + pkg
                    refresh++
                },
                onRequestUsage = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                onRequestOverlay = {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                },
                onRequestNotifications = {
                    if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                },
                onDone = { showSetup = false; MonitorService.start(this) },
            )

            focusStartedAt > 0 -> {
                val task = Tasks.READ
                val left = task.target * 60L - (now - focusStartedAt) / 1000
                if (left <= 0) {
                    LaunchedEffect(Unit) {
                        store.credit(task.id, task.rewardMinutes, manual = false)
                        focusStartedAt = 0
                    }
                } else {
                    FocusTimerScreen(store.avatar, task, left, onCancel = { focusStartedAt = 0 })
                }
            }

            else -> HomeScreen(
                avatar = store.avatar,
                cfg = store.config,
                day = store.today(),
                gardenMinutes = store.gardenMinutes,
                missCount = store.missCount(),
                onStartFocus = { focusStartedAt = System.currentTimeMillis() },
                onOpenSettings = { showSetup = true },
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refresh++
    }

    private fun setupComplete() = store.blockedPackages.isNotEmpty() && permissions().allGranted

    private fun permissions(): Permissions {
        val ops = getSystemService(AppOpsManager::class.java)
        val usage = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        } == AppOpsManager.MODE_ALLOWED
        val notifications = Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return Permissions(usage, Settings.canDrawOverlays(this), notifications)
    }

    private fun launchableApps(): List<InstalledApp> =
        packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(packageManager).toString()) }
            .filter { it.pkg != packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
}
