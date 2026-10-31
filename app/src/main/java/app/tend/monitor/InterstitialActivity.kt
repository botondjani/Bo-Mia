package app.tend.monitor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import app.tend.ui.PopupSheet
import app.tend.ui.PrimaryButton
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
import app.tend.core.Taglines
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

    /** Null until the activity sync has run; then what the sheet shows. */
    private var view by mutableStateOf<View?>(null)

    private data class View(val label: String, val locked: Boolean, val opensToday: Int, val delaySeconds: Int, val remaining: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val pkg = intent.getStringExtra(EXTRA_PKG) ?: return finish()
        val label = runCatching { packageManager.getApplicationInfo(pkg, 0).loadLabel(packageManager).toString() }.getOrDefault(pkg)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })
        val firstShow = savedInstanceState == null
        // The scrim covers the app right away; the sheet appears once we know the budget.
        setContent { TendTheme { view?.let { Sheet(pkg, it) } ?: Scrim() } }
        // Fresh steps or a finished workout may have just earned minutes: sync before deciding.
        lifecycleScope.launch {
            withTimeoutOrNull(SYNC_TIMEOUT_MS) { TaskSync.sync(this@InterstitialActivity) }
            val cfg = store.config
            val locked = Budget.isLocked(cfg, store.today())
            var day = store.today()
            val delaySeconds = Budget.openDelaySeconds(cfg, day)
            if (firstShow && !locked) day = store.update { it.copy(opensToday = it.opensToday + 1) }
            view = View(label, locked, day.opensToday, delaySeconds, Budget.remainingMinutes(cfg, day))
        }
    }

    @Composable
    private fun Scrim() = Box(Modifier.fillMaxSize().background(Brand.Ink.copy(alpha = 0.45f)))

    @Composable
    private fun Sheet(pkg: String, v: View) = PopupSheet(onDismiss = ::goHome) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CharacterView(store.avatar, if (v.locked) Mood.TIRED else Mood.SCROLLING, 84.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    if (v.locked) "Today's budget is used up" else "What are you looking for?",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    if (v.locked) "Opening ${v.label}" else "Opening ${v.label} · ${ordinal(v.opensToday)} time today",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (v.locked) Locked() else Delay(pkg, v)
    }

    @Composable
    private fun Delay(pkg: String, v: View) {
        var answer by rememberSaveable { mutableStateOf<String?>(null) }
        var left by rememberSaveable { mutableIntStateOf(v.delaySeconds) }
        LaunchedEffect(Unit) { while (left > 0) { delay(1000); left-- } }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Checking messages", "Something specific", "Just bored").forEach { a ->
                val on = answer == a
                Text(
                    a,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) Color.White else Brand.Moss,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) Brand.Moss else Brand.Card)
                        .clickable { if (answer == null) store.logAnswer("looking_for", pkg, a); answer = a }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
        Text(Taglines.forOpen(v.opensToday, store.avatar.displayName), style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
        Text("${v.remaining} min left today", style = MaterialTheme.typography.bodyMedium)
        PrimaryButton("I'll take a walk instead", ::goHome)
        OutlinedButton(
            onClick = {
                store.focusStartedAt = 0 // opening a paused app ends the reading session
                store.grantPass(pkg)
                packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
                finish()
            },
            enabled = left == 0,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(if (left > 0) "Open in $left s" else "Open ${v.label}") }
        Text(
            if (store.focusStartedAt > 0) "Opening it ends your reading session." else "Every extra open today waits 5 s longer.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    @Composable
    private fun Locked() {
        val next = Tasks.next(store.today())
        Text(
            next?.let { "${it.title} brings back ${it.rewardMinutes} min. Want to do it together?" }
                ?: "That's all for today. Tomorrow's a fresh start.",
            style = MaterialTheme.typography.bodyLarge,
        )
        PrimaryButton("Let's earn some", {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
        })
        OutlinedButton(
            onClick = ::goHome, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Not now") }
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
