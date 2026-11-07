package app.tend.monitor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import app.tend.ui.PopupSheet
import app.tend.ui.blurBehind
import app.tend.ui.PrimaryButton
import app.tend.ui.SecondaryButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
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
        /** The chosen minutes ran out ("Your 5 minutes are up"). */
        val timeUp: Boolean = false,
        /** A scheduled lock is on (bedtime, work hours): only "Not now" and something better. */
        val scheduled: app.tend.core.LockSchedule? = null,
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
        val evening = Budget.isEvening(if (store.eveningPause) store.eveningFrom else -1, java.time.LocalTime.now().hour)
        val delaySeconds = Budget.openDelaySeconds(cfg, day, evening)
        val scheduled = app.tend.core.LockSchedule.activeOf(store.lockSchedules)
        // A scheduled lock isn't an open: it doesn't make the next pause longer.
        if (savedInstanceState == null && !locked && scheduled == null) day = store.update { it.copy(opensToday = it.opensToday + 1) }
        view = View(
            label, locked, day.opensToday, delaySeconds, Budget.remainingMinutes(cfg, day), evening,
            timeUp = intent.getBooleanExtra(EXTRA_TIME_UP, false),
            scheduled = scheduled,
        )
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
        when {
            v.scheduled != null -> Scheduled(v, v.scheduled)
            v.locked -> Locked(v)
            else -> Delay(pkg, v)
        }
    }

    @Composable
    private fun Header(v: View) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CharacterView(store.avatar, if (v.locked || v.evening || v.scheduled != null) Mood.TIRED else Mood.SCROLLING, 76.dp, lightweight = true)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    when {
                        v.scheduled != null -> "${v.label} · ${v.scheduled.label.lowercase()}"
                        v.locked -> "${v.label} · out of minutes"
                        v.timeUp -> "${v.label} · time's up"
                        v.evening -> "${v.label} · evening mode"
                        else -> "${v.label} · ${ordinal(v.opensToday)} open today"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    app.tend.ui.brandTend(when {
                        v.scheduled != null -> "Closed until ${"%d:00".format(v.scheduled.to)}. That's the deal we made."
                        v.locked -> "Today's budget is used up."
                        v.timeUp -> "Those were your minutes. Still worth it?"
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
        // One row of four short labels, equal widths. The logged answer keeps the full wording.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LOOKING_FOR.forEach { (label, a) ->
                val on = answer == a
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    color = if (on) Color.White else Brand.Moss,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (on) Brand.Moss else Brand.Card)
                        .clickable { if (answer == null) store.logAnswer("looking_for", pkg, a); answer = a }
                        .padding(horizontal = 2.dp, vertical = 9.dp),
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
        Text(
            if (store.focusStartedAt > 0) "${v.remaining} min left today · opening ends your reading session" else "${v.remaining} min left today",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        val openFor = { minutes: Int ->
            decide("open_$minutes")
            TaskSync.stopTimer(this@InterstitialActivity) // opening a paused app ends a running timer
            store.grantPass(pkg, minutes)
            packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
            finish()
        }
        val instead = insteadSuggestion(v.evening)
        if (instead != null) {
            // The pause turns into the thing you'd rather do, one tap away.
            PrimaryButton(instead.first, { decide("instead"); instead.second() })
            SecondaryButton("Not now", ::notNow)
            OpenLinks(v, left, openFor)
        } else {
            PrimaryButton("Not now", ::notNow)
            OpenLinks(v, left, openFor)
        }
        }
    }

    /**
     * Ways in, side by side: 1 min right away (five a day), 5 or 15 min after the countdown.
     * When the time is up, tend checks in again.
     */
    @Composable
    private fun OpenLinks(v: View, left: Int, openFor: (Int) -> Unit) {
        val quick = store.quickLooksLeft()
        Text(
            "Open ${v.label} for",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 1 min: right away, no wait (five a day). Filled, so it reads as "this one works now".
            if (quick > 0) OpenChip(
                "1 min", if (quick <= 2) "now · $quick left" else "now",
                enabled = true, filled = true, modifier = Modifier.weight(1f).fillMaxHeight(),
            ) { store.useQuickLook(); openFor(1) }
            app.tend.core.TimeBox.CHOICES.forEach { minutes ->
                OpenChip(
                    "$minutes min", if (left > 0) "in $left s" else "open",
                    enabled = left == 0, filled = false, modifier = Modifier.weight(1f).fillMaxHeight(),
                ) { openFor(minutes) }
            }
        }
    }

    @Composable
    private fun OpenChip(title: String, caption: String, enabled: Boolean, filled: Boolean, modifier: Modifier, onClick: () -> Unit) = Column(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(if (filled) Brand.Mint.copy(alpha = 0.35f) else Color.Transparent)
            .border(1.5.dp, if (enabled) Brand.Mint else Brand.Mint.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = if (enabled) Brand.Moss else Brand.Muted, maxLines = 1)
        Text(caption, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = Brand.Muted, maxLines = 1)
    }

    /** A scheduled lock: no way in until it ends, only something better or "Not now". */
    @Composable
    private fun Scheduled(v: View, lock: app.tend.core.LockSchedule) {
        val instead = insteadSuggestion(v.evening)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Header(v)
            Text("You can change ${lock.label.lowercase()} in tend's settings.", style = MaterialTheme.typography.bodyMedium)
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
            if (instead != null) PrimaryButton(instead.first, { decide("instead"); instead.second() })
            SecondaryButton("Not now", ::notNow)
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

    /** Instead of the app: back to tend's activities, to pick one with the companion. */
    private fun insteadSuggestion(@Suppress("UNUSED_PARAMETER") evening: Boolean): Pair<String, () -> Unit>? {
        if (store.selectedTasks.isEmpty()) return null
        return "Do something with ${store.avatar.displayName} instead" to {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
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
        private const val EXTRA_TIME_UP = "time_up"

        /** Short label on the chip to the answer that's logged. */
        private val LOOKING_FOR = listOf(
            "Reply" to "Messages",
            "Look up" to "Looking something up",
            "Bored" to "Just bored",
            "Habit" to "Out of habit",
        )

        fun intent(context: Context, pkg: String, timeUp: Boolean = false) = Intent(context, InterstitialActivity::class.java)
            .putExtra(EXTRA_PKG, pkg)
            .putExtra(EXTRA_TIME_UP, timeUp)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    }
}
