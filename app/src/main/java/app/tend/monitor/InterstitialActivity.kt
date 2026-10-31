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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import app.tend.ui.PopupSheet
import app.tend.ui.blurBehind
import androidx.compose.material3.ButtonDefaults
import app.tend.ui.PrimaryButton
import app.tend.ui.SecondaryButton
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

    /** What the card shows; refreshed once the background sync has run. */
    private var view by mutableStateOf<View?>(null)

    private data class View(
        val label: String, val locked: Boolean, val opensToday: Int, val delaySeconds: Int, val remaining: Int, val evening: Boolean,
    )

    /** How this pause ended: open, not_now, instead or earn. Logged once; it's what the test measures. */
    private var decided = false
    private var pkgName = ""

    private fun decide(what: String) {
        if (decided) return
        decided = true
        store.logAnswer("pause", pkgName, what)
    }

    override fun onStop() {
        super.onStop()
        // Left without a choice (home key, a call): the app wasn't opened from here.
        if (!isChangingConfigurations) {
            decide("left")
            // Never come back with an old countdown: the next open starts a fresh pause.
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        blurBehind()
        val pkg = intent.getStringExtra(EXTRA_PKG) ?: return finish()
        pkgName = pkg
        val label = runCatching { packageManager.getApplicationInfo(pkg, 0).loadLabel(packageManager).toString() }.getOrDefault(pkg)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = notNow()
        })
        // Show the card in the very first frame from what we already know...
        val cfg = store.config
        var day = store.today()
        val locked = Budget.isLocked(cfg, day)
        val evening = Budget.isEvening(store.eveningFrom, java.time.LocalTime.now().hour)
        val delaySeconds = Budget.openDelaySeconds(cfg, day, evening)
        if (savedInstanceState == null && !locked) day = store.update { it.copy(opensToday = it.opensToday + 1) }
        view = View(label, locked, day.opensToday, delaySeconds, Budget.remainingMinutes(cfg, day), evening)
        val worn = store.accessories().first
        setContent {
            TendTheme {
                androidx.compose.runtime.CompositionLocalProvider(app.tend.ui.LocalAccessories provides worn) { view?.let { Sheet(pkg, it) } }
            }
        }
        // ...then catch up quietly: fresh steps or a finished workout may have just earned minutes.
        lifecycleScope.launch {
            withTimeoutOrNull(SYNC_TIMEOUT_MS) { TaskSync.sync(this@InterstitialActivity) }
            val now = store.today()
            view = view?.copy(locked = Budget.isLocked(cfg, now), remaining = Budget.remainingMinutes(cfg, now))
        }
    }

    @Composable
    private fun Sheet(pkg: String, v: View) = PopupSheet(onDismiss = ::notNow, centered = true) {
        if (v.locked) Locked(v) else Delay(pkg, v)
    }

    @Composable
    private fun Header(v: View) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CharacterView(store.avatar, if (v.locked || v.evening) Mood.TIRED else Mood.SCROLLING, 76.dp, lightweight = true)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    when {
                        v.locked -> "${v.label} · out of minutes"
                        v.evening -> "${v.label} · evening mode"
                        else -> "${v.label} · ${ordinal(v.opensToday)} open today"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    app.tend.ui.brandTend(when {
                        v.locked -> "Today's budget is used up."
                        v.evening -> "It's late. We tend to regret the late-night scrolls the most."
                        else -> Taglines.forOpen(v.opensToday, store.avatar.displayName)
                    }),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }

    @Composable
    private fun Delay(pkg: String, v: View) {
        var answer by rememberSaveable { mutableStateOf<String?>(null) }
        var left by rememberSaveable { mutableIntStateOf(v.delaySeconds) }
        LaunchedEffect(Unit) { while (left > 0) { delay(1000); left-- } }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Header(v)
        Text("WHAT ARE YOU LOOKING FOR?", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
        // One row: short labels, equal widths. The logged answer keeps the original wording.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Messages" to "Messages", "Something" to "Something specific", "Just bored" to "Just bored").forEach { (label, a) ->
                val on = answer == a
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) Color.White else Brand.Moss,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (on) Brand.Moss else Brand.Card)
                        .clickable { if (answer == null) store.logAnswer("looking_for", pkg, a); answer = a }
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                )
            }
        }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
        Text(
            if (store.focusStartedAt > 0) "${v.remaining} min left today · opening ends your reading session" else "${v.remaining} min left today",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        val open = {
            decide("open")
            TaskSync.stopTimer(this@InterstitialActivity) // opening a paused app ends a running timer
            store.grantPass(pkg)
            packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
            finish()
        }
        val instead = insteadSuggestion(v.evening)
        if (instead != null) {
            // The pause turns into the thing you'd rather do, one tap away.
            PrimaryButton(instead.first, { decide("instead"); instead.second() })
            SecondaryButton("Not now", ::notNow)
            Text(
                if (left > 0) "Open ${v.label} in $left s" else "Open ${v.label}",
                style = MaterialTheme.typography.labelLarge,
                color = if (left > 0) Brand.Muted else Brand.Moss,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = left == 0, onClick = open).padding(vertical = 10.dp),
            )
        } else {
            PrimaryButton("Not now", ::notNow)
            SecondaryButton(if (left > 0) "Open ${v.label} in $left s" else "Open ${v.label}", onClick = open, enabled = left == 0)
        }
        }
    }

    @Composable
    private fun Locked(v: View) {
        val next = Tasks.next(store.today(), store.selectedTasks)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Header(v)
        Text(
            next?.let { "${it.title} brings back ${it.rewardMinutes} min. Want to do it together?" }
                ?: "That's all for today. Tomorrow's a fresh start.",
            style = MaterialTheme.typography.bodyLarge,
        )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
        PrimaryButton("Let's earn some", {
            decide("earn")
            startActivity(Intent(this@InterstitialActivity, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
        })
        SecondaryButton("Not now", ::notNow)
        }
    }

    private fun notNow() { decide("not_now"); goHome() }

    /**
     * What to offer instead of the app: the first picked activity not done yet. Timers start right
     * away in tend; a walk or a workout just sends you on your way.
     */
    private fun insteadSuggestion(evening: Boolean): Pair<String, () -> Unit>? {
        val day = store.today()
        val name = store.avatar.displayName
        val open = store.selectedTasks.filter { it.id !in day.completedTasks && (it != app.tend.core.Tasks.SLEEP || evening) }
        val task = open.firstOrNull { it.verification.timed } ?: open.firstOrNull() ?: return null
        return when {
            task.verification.timed -> "${task.title} with $name instead" to {
                TaskSync.startTimer(this, task)
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                finish()
            }
            task == app.tend.core.Tasks.SLEEP -> "Off to bed instead" to ::goHome
            task == app.tend.core.Tasks.OUTSIDE -> "A short walk instead" to ::goHome
            task == app.tend.core.Tasks.MOVE -> "Move a little instead" to ::goHome
            else -> "${task.title} instead" to {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                finish()
            }
        }
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
        private const val SYNC_TIMEOUT_MS = 3000L
        fun intent(context: Context, pkg: String) = Intent(context, InterstitialActivity::class.java)
            .putExtra(EXTRA_PKG, pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    }
}
