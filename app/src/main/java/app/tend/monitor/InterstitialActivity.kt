package app.tend.monitor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tend.MainActivity
import app.tend.core.Budget
import app.tend.health.TaskSync
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import app.tend.core.Mood
import app.tend.core.Tasks
import app.tend.data.TendStore
import app.tend.ui.Brand
import app.tend.ui.Card
import app.tend.ui.CharacterView
import app.tend.ui.TendTheme
import kotlinx.coroutines.delay

/** The delay screen shown when a blocked app is opened. */
class InterstitialActivity : ComponentActivity() {
    private val store by lazy { TendStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val pkg = intent.getStringExtra(EXTRA_PKG) ?: return finish()
        val label = runCatching { packageManager.getApplicationInfo(pkg, 0).loadLabel(packageManager).toString() }.getOrDefault(pkg)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })
        val firstShow = savedInstanceState == null
        // Fresh steps or a finished workout may have just earned minutes: sync before deciding.
        lifecycleScope.launch {
            withTimeoutOrNull(SYNC_TIMEOUT_MS) { TaskSync.sync(this@InterstitialActivity) }
            show(pkg, label, firstShow)
        }
    }

    private fun show(pkg: String, label: String, firstShow: Boolean) {
        val cfg = store.config
        val locked = Budget.isLocked(cfg, store.today())

        var day = store.today()
        val delaySeconds = Budget.openDelaySeconds(cfg, day)
        if (firstShow && !locked) day = store.update { it.copy(opensToday = it.opensToday + 1) }

        setContent {
            TendTheme {
                Column(
                    Modifier.fillMaxSize().background(Brand.Sage).safeDrawingPadding().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(if (locked) "Opening $label" else "Opening $label · ${ordinal(day.opensToday)} time today", style = MaterialTheme.typography.bodyMedium)
                    CharacterView(store.avatar, if (locked) Mood.TIRED else Mood.SCROLLING, 150.dp)
                    if (locked) Locked() else Delay(pkg, delaySeconds, Budget.remainingMinutes(cfg, day))
                }
            }
        }
    }

    @Composable
    private fun Delay(pkg: String, delaySeconds: Int, remaining: Int) {
        var answer by rememberSaveable { mutableStateOf<String?>(null) }
        var left by rememberSaveable { mutableIntStateOf(delaySeconds) }
        LaunchedEffect(Unit) { while (left > 0) { delay(1000); left-- } }

        Text("What are you looking for?", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text("${store.avatar.displayName}'s doing it too. Not sure why either.", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Checking messages", "Something specific", "Just bored").forEach { a ->
                FilterChip(
                    selected = answer == a,
                    onClick = { if (answer == null) store.logAnswer("looking_for", pkg, a); answer = a },
                    label = { Text(a) },
                )
            }
        }
        Card { Text("Today's budget · $remaining min left", style = MaterialTheme.typography.bodyLarge) }
        Spacer(Modifier.height(8.dp))
        Button(onClick = ::goHome, modifier = Modifier.fillMaxWidth()) { Text("I'll take a walk instead") }
        OutlinedButton(
            onClick = {
                store.focusStartedAt = 0 // opening a paused app ends the reading session
                store.grantPass(pkg)
                packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
                finish()
            },
            enabled = left == 0,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (left > 0) "Open in $left s" else "Open") }
        Text(
            if (store.focusStartedAt > 0) "Opening it ends your reading session." else "Every extra open today waits 5 s longer.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    @Composable
    private fun Locked() {
        val next = Tasks.next(store.today())
        Text("Today's budget is used up", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(
            next?.let { "${it.title} brings back ${it.rewardMinutes} min. Want to do it together?" }
                ?: "That's all for today. Tomorrow's a fresh start.",
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                finish()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Let's earn some") }
        OutlinedButton(onClick = ::goHome, modifier = Modifier.fillMaxWidth()) { Text("Not now") }
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    private fun ordinal(n: Int) = n.toString() + when {
        n % 100 in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else -> "th"
    }

    companion object {
        private const val EXTRA_PKG = "pkg"
        private const val SYNC_TIMEOUT_MS = 1500L
        fun intent(context: Context, pkg: String) = Intent(context, InterstitialActivity::class.java)
            .putExtra(EXTRA_PKG, pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    }
}
