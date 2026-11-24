package app.tend.ui

import app.tend.core.tr

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tend.core.Avatar
import app.tend.core.AppKinds
import app.tend.core.BudgetConfig
import app.tend.core.Budget
import app.tend.core.DayState
import app.tend.core.Mood
import java.time.LocalDate
import app.tend.core.TaskDef

data class Permissions(val usageAccess: Boolean, val overlay: Boolean) {
    val allGranted get() = usageAccess && overlay
}

/** What the permission step needs from the activity. */
class PermissionActions(
    val permissions: Permissions,
    /** The user tried to grant it but came back without it: likely Android's restricted settings. */
    val usageBlocked: Boolean,
    val overlayBlocked: Boolean,
    val requestUsage: () -> Unit,
    val requestOverlay: () -> Unit,
    val openAppInfo: () -> Unit,
    /** Android 13+ "restricted settings" for side-loaded installs. */
    val lock: PermissionLock = PermissionLock.NONE,
    /** Lets Android refuse a switch once, which makes "Allow restricted settings" appear. */
    val startUnlock: () -> Unit = {},
    /** Opens App info for "Allow restricted settings". */
    val unlockAppInfo: () -> Unit = openAppInfo,
    val skipUnlock: () -> Unit = {},
)

/** Where Android's restricted-settings lock stands: none, locked, or refused once (the unlock option now shows). */
enum class PermissionLock { NONE, LOCKED, TRIPPED }

// Short on purpose: the town is asked later in the camp, and an account is offered on home (or right here, for those who have one).
private const val STEP_MS = 650L

/** How big the companion is at the top of setup (the same on every page). */
private val COMPANION_SIZE = 130.dp

private enum class Step { SIGN_IN, WELCOME, INTRO, PERMS, APPS, MINUTES, EARN, CAMP }

@Composable
fun Onboarding(
    catalog: AppCatalog,
    avatar: Avatar,
    cfg: BudgetConfig,
    blocked: Set<String>,
    perms: PermissionActions,
    onAvatar: (Avatar) -> Unit,
    onConfig: (BudgetConfig) -> Unit,
    onToggleApp: (String) -> Unit,
    signedIn: Boolean = false,
    /** The activities: picked so far, toggling one, and taking the defaults (also preselected when nothing is picked yet). */
    tasks: List<TaskDef> = emptyList(),
    onToggleTask: (TaskDef) -> Unit = {},
    onDefaultTasks: (reset: Boolean) -> Unit = {},
    account: @Composable (onDone: () -> Unit) -> Unit = {},
    /** The camp at the end: what's in it, the acorns, buying the campfire, and the companion using it. */
    placed: Map<Int, String> = emptyMap(),
    acorns: Int = 0,
    onBuy: (String) -> Boolean = { false },
    onUsed: (String) -> Unit = {},
    /** A little about you (all optional): your town for the weather, an age range, a gender. */
    onFinish: () -> Unit,
) {
    // Already signed in (back after a restart, say): straight past the sign-in page.
    var stepIndex by rememberSaveable { mutableIntStateOf(if (signedIn) Step.WELCOME.ordinal else 0) }
    val step = Step.entries[stepIndex]
    // A second tap on "Continue" while the page slides in lands on the next page's button: steps move at most
    // once per transition, so none is skipped.
    var movedAt by remember { mutableStateOf(0L) }
    val next = {
        val now = android.os.SystemClock.uptimeMillis()
        if (movedAt == 0L || now - movedAt > STEP_MS) { movedAt = now; stepIndex = (stepIndex + 1).coerceAtMost(Step.entries.lastIndex) }
    }
    // Where people leave the setup, if they share counts: each step once, as it's reached.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(step) { app.tend.data.Stats.log(ctx, "onboarding_step", mapOf("step" to step.name.lowercase(), "n" to step.ordinal + 1)) }
    // System back goes one step back instead of leaving the app mid-setup.
    androidx.activity.compose.BackHandler(enabled = stepIndex > 0) { movedAt = android.os.SystemClock.uptimeMillis(); stepIndex-- }

    Column(Modifier.fillMaxSize().background(Brand.Sage)) {
        Progress(stepIndex + 1, Step.entries.size + 1) { if (stepIndex > 0) { movedAt = android.os.SystemClock.uptimeMillis(); stepIndex-- } }
        // Once picked, the companion stays put at the top, big, while only what's under them changes. Not
        // before (no Bo or Mia until one is chosen), and not at the camp, where they live in the picture.
        androidx.compose.animation.AnimatedVisibility(
            step != Step.SIGN_IN && step != Step.WELCOME && step != Step.CAMP,
            enter = androidx.compose.animation.expandVertically(tween(420)) + androidx.compose.animation.fadeIn(tween(420)),
            exit = androidx.compose.animation.shrinkVertically(tween(380)) + androidx.compose.animation.fadeOut(tween(260)),
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                BobbingCharacter(avatar, Mood.HAPPY, COMPANION_SIZE)
                // The speech bubble's point, up towards them: it stays, the bubble under it changes.
                androidx.compose.foundation.Canvas(Modifier.size(18.dp, 9.dp)) {
                    drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(0f, size.height); lineTo(size.width / 2, 0f); lineTo(size.width, size.height); close() }, Brand.Card)
                }
            }
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                // Under the companion, the page slides all the way across: the old one out to the side, the new one in
                // from the other, together, with a soft ease (and a touch of fade at the far ends).
                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                val ease = androidx.compose.animation.core.CubicBezierEasing(0.3f, 0f, 0.1f, 1f)
                (slideInHorizontally(tween(480, easing = ease)) { it * dir } + androidx.compose.animation.fadeIn(tween(480, easing = ease), initialAlpha = 0.3f)) togetherWith
                    (slideOutHorizontally(tween(480, easing = ease)) { -it * dir } + androidx.compose.animation.fadeOut(tween(480, easing = ease), targetAlpha = 0.3f))
            },
            modifier = Modifier.weight(1f),
            label = "onboarding",
        ) { s ->
            when (s) {
                // The chosen companion says hello, big: what tend is, in a sentence.
                // Hello, and how it works, on one page: who they are, then three lines, nothing to set.
                Step.INTRO -> StepPage(cta = tr("Got it", "Értem"), onCta = next, scroll = false) {
                    Says(avatar, tr("Hi, I'm ${avatar.displayName}!", "Szia, ${avatar.displayName} vagyok!"), tr("I'll help you scroll less. Here's how:", "Segítek kevesebbet görgetni. Így működik:"), figure = 0.dp)
                    Point("sparkle", tr("You pick the apps that take your time.", "Kiválasztod az appokat, amik elviszik az idődet."))
                    Point("sun", tr("A few minutes a day come free. When they're gone, those apps close.", "Naponta egy kevés idő ingyen jár rájuk. Ha elfogy, bezárnak."))
                    Point("heart", tr("Most minutes you unlock yourself: with Phone down, or by doing your daily tasks.", "A percek nagy részét te oldod fel: a Telefon le funkcióval, vagy a napi feladataid elvégzésével."))
                }

                // The very first page: sign in (an old camp comes back by itself) or start without an account.
                Step.SIGN_IN -> Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Title(tr("Welcome to tend", "Üdv a tendben"), tr("Less scrolling, more of your day. An account is optional.", "Kevesebb görgetés, több a napodból. Fiók nem kötelező."))
                        // Google or an e-mail code; with a backup on this account, the old camp is restored and setup ends there.
                        account { next() }
                        LanguagePicker()
                    }
                    // Starting without an account is a real choice, as big as signing in: a full button, and what it means.
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            tr("Start without an account", "Kezdés regisztráció nélkül"), style = MaterialTheme.typography.titleMedium, color = Brand.Moss, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).border(2.dp, Brand.Moss, RoundedCornerShape(18.dp)).tappable(onClick = next).padding(vertical = 16.dp),
                        )
                        Text(
                            tr("Everything works without one. You can sign up later and keep your camp.", "Minden működik nélküle is. Később is regisztrálhatsz, és a táborod megmarad."),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted, textAlign = TextAlign.Center,
                        )
                    }
                }

                // First: the language, and who keeps you company.
                Step.WELCOME -> StepPage(cta = tr("Continue with ${avatar.displayName}", "Tovább, ${avatar.displayName} jön velem"), onCta = next, scroll = false) {
                    // The language is on the sign-in page; here only when that page was skipped (signed in already).
                    Title(tr("Who keeps you company?", "Ki legyen veled?"), tr("You can switch any time.", "Bármikor válthatsz."))
                    CompanionPicker(avatar, onAvatar)
                    if (signedIn) LanguagePicker()
                    Spacer(Modifier.weight(1f))
                }

                // The switches tend needs, one at a time: only what's still missing shows.
                Step.PERMS -> StepPage(
                    cta = if (perms.permissions.allGranted) tr("Continue", "Tovább") else tr("One more step", "Még egy lépés"),
                    ctaEnabled = perms.permissions.allGranted, onCta = next, scroll = false,
                ) {
                    var askSkip by rememberSaveable { mutableStateOf(false) }
                    if (askSkip) LimitedModeDialog(perms.permissions, onConfirm = { askSkip = false; next() }, onDismiss = { askSkip = false })
                    val p = perms.permissions
                    Says(avatar, tr("I need two switches to work.", "Két engedély kell, hogy működjek."), if (p.allGranted) tr("All set, thank you!", "Megvan, köszönöm!") else tr("Both on this page. It all stays on your phone.", "Mindkettő itt van. Minden a telefonodon marad."), figure = 0.dp)
                    // Both turned on here: a moment to see the two ticks, then on by itself. Not when they were on
                    // already (come back to with Back): that would bounce straight forward again.
                    val grantedBefore = rememberSaveable { p.allGranted }
                    // Precise mode (the Play build's accessibility service) is offered here, optional: while it's
                    // on offer the page waits for "Continue" instead of moving on by itself.
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    val precise = remember(p) { app.tend.monitor.ShortsGuard.enabled(ctx) }
                    val offerPrecise = app.tend.BuildConfig.PLAY && p.allGranted && !precise
                    LaunchedEffect(p.allGranted) { if (p.allGranted && !grantedBefore && !offerPrecise) { kotlinx.coroutines.delay(900); next() } }
                    if (perms.lock != PermissionLock.NONE && !p.allGranted) UnlockCard(perms)
                    else {
                        // Both at once, in order: the one to do next is the green one; done ones say so.
                        OneSwitch(
                            tr("Show over other apps", "Megjelenítés más appok fölött"), tr("So the apps can close when your minutes are gone.", "Hogy az appok bezárhassanak, ha elfogyott a perced."),
                            done = p.overlay, next = !p.overlay, blocked = perms.overlayBlocked, onAllow = perms.requestOverlay, onAppInfo = perms.openAppInfo,
                        )
                        OneSwitch(
                            tr("Usage access", "Használati hozzáférés"), tr("So I can see how long each app is open.", "Hogy lássam, mennyi ideig van nyitva egy app."),
                            done = p.usageAccess, next = p.overlay && !p.usageAccess, blocked = perms.usageBlocked, onAllow = perms.requestUsage, onAppInfo = perms.openAppInfo,
                        )
                    }
                    // What a tap does, said once: where it goes, what to switch, and that it comes back by itself.
                    if (!p.allGranted && perms.lock == PermissionLock.NONE) Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(horizontal = 4.dp)) {
                        TendIcons.Glyph("sparkle", 16.dp, tint = Brand.Muted)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            tr("A tap opens Android's settings: switch tend on there, and you're brought straight back.", "Egy koppintás megnyitja az Android beállítását: ott kapcsold be a tendet, és utána magától visszahoz."),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = Brand.Muted,
                        )
                    }
                    if (offerPrecise) Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brand.Card).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(tr("Precise mode (optional)", "Pontos mód (nem kötelező)"), style = MaterialTheme.typography.titleMedium)
                        Text(
                            tr("A faster pause, no \"displaying over other apps\" note, and closing only Reels and Shorts if you like. It reads only which app and screen is open, nothing you type.",
                                "Gyorsabb Megállító, nincs „displaying over other apps” értesítés, és ha szeretnéd, csak a Reels és a Shorts zár be. Csak azt látja, melyik app és képernyő van nyitva, semmit abból, amit írsz."),
                            style = MaterialTheme.typography.bodyMedium, color = Brand.Muted,
                        )
                        SecondaryButton(tr("Turn on", "Bekapcsolom"), {
                            ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                        })
                    }
                    Spacer(Modifier.weight(1f))
                    if (!p.allGranted) LinkText(tr("Continue without", "Folytatom nélküle"), { askSkip = true })
                }

                // The apps: the few that matter most, as tiles; anything else from a search.
                Step.APPS -> StepPage(
                    cta = if (blocked.isEmpty()) tr("Pick at least one", "Válassz legalább egyet") else tr("Continue with ${blocked.size} app${if (blocked.size == 1) "" else "s"}", "Tovább ${blocked.size} appal"),
                    ctaEnabled = blocked.isNotEmpty(), onCta = next, scroll = false,
                ) {
                    Says(avatar, tr("Which apps take your time?", "Mely appok viszik el az idődet?"), tr("Their time comes off your daily minutes.", "Ezek ideje fogy a napi keretedből."), figure = 0.dp)
                    // The most used, in that order, and nothing else: picking doesn't move them (ones found by search are counted below).
                    val tiles = catalog.suggested.take(8)
                    if (tiles.isNotEmpty()) AppGrid(catalog, tiles, blocked, onToggleApp)
                    // Picked from the search: shown under the tiles with their icons (tap to take one off).
                    val hidden = blocked.filter { b -> tiles.none { it.pkg == b } }
                    if (hidden.isNotEmpty()) androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        hidden.forEach { pkg ->
                            Row(
                                Modifier.clip(RoundedCornerShape(50)).background(Brand.Mint.copy(alpha = 0.35f)).border(1.5.dp, Brand.Moss, RoundedCornerShape(50))
                                    .clickable { onToggleApp(pkg) }.padding(start = 6.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppIcon(catalog, pkg, 24.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(catalog.label(pkg), style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), maxLines = 1)
                                Spacer(Modifier.width(6.dp))
                                Text("\u00D7", style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
                            }
                        }
                    }
                    var searching by rememberSaveable { mutableStateOf(false) }
                    LinkText(tr("Another app…", "Másik app…"), { searching = true })
                    if (searching) AppSearch(catalog, blocked, onToggleApp) { searching = false }
                }

                // How much a day: one number.
                Step.MINUTES -> StepPage(cta = tr("Continue", "Tovább"), onCta = next, scroll = false) {
                    val now = blocked.sumOf { catalog.dailyMinutes[it] ?: 0 }
                    Says(avatar,
                        tr("How much time a day for them?", "Mennyi idő jár rájuk naponta?"),
                        (if (now > 0) tr("They take about $now min a day now. ", "Most nagyjából napi $now percet visznek el. ") else "") +
                            tr("A little comes free; the rest you unlock.", "Egy kevés ingyen jár, a többit te oldod fel."),
                        figure = 0.dp,
                    )
                    Card {
                        Stepper(tr("Free every day", "Ingyen naponta"), tr("For the picked apps", "A kiválasztott appokra"), listOf(15, 30, 45, 60), cfg.freeMinutes) {
                            onConfig(cfg.copy(freeMinutes = it, totalMinutes = maxOf(cfg.totalMinutes, it)))
                        }
                        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(Brand.Sage))
                        Stepper(tr("At most a day", "Naponta legfeljebb"), tr("With the minutes you unlock", "A feloldott percekkel együtt"), listOf(90, 120, 180, 240), cfg.totalMinutes) {
                            onConfig(cfg.copy(totalMinutes = it, freeMinutes = minOf(cfg.freeMinutes, it)))
                        }
                    }
                }

                // Two ways to unlock minutes: putting the phone down (always there), and tasks of your own choosing.
                Step.EARN -> StepPage(cta = if (tasks.isEmpty()) tr("Just Phone down for now", "Most csak a Telefon le") else tr("Continue", "Tovább"), onCta = next, scroll = tasks.size > 4) {
                    var adding by rememberSaveable { mutableStateOf(false) }
                    if (adding) OwnActivityDialog(onDismiss = { adding = false }) { t -> adding = false; onToggleTask(t) }
                    // The more tasks, the smaller the companion (gone from 5 on), so the page fits without scrolling.
                    Says(avatar, tr("Two ways to unlock minutes", "Két módon oldhatsz fel perceket"), figure = 0.dp)
                    // 1: phone down, the big one.
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brand.Moss).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { TendIcons.Badge("hourglass", 36.dp) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr("Phone down", "Telefon le"), style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(tr("Set a timer and put it away: each minute away unlocks a minute.", "Indíts egy időzítőt, és tedd félre: minden letett perc felold egy percet."), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                        }
                    }
                    // 2: daily tasks: yours first, then writing one, and ideas at the bottom.
                    Text(tr("…or daily tasks", "…vagy napi feladatok"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
                    tasks.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { t -> TaskTile(t.title, t.icon, picked = true, Modifier.weight(1f)) { onToggleTask(t) } }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    val full = tasks.size >= app.tend.core.Tasks.MAX_SELECTED
                    if (full) Text(tr("That's ${app.tend.core.Tasks.MAX_SELECTED}, the most there can be. Tap one to take it off.", "Ez ${app.tend.core.Tasks.MAX_SELECTED}, ennyi fér bele. Koppints egyre, ha levennéd."), style = MaterialTheme.typography.bodyMedium, color = Brand.Muted)
                    else Row(
                        Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(16.dp)).background(Brand.Mint.copy(alpha = 0.3f))
                            .border(2.dp, Brand.Moss, RoundedCornerShape(16.dp)).clickable { adding = true }.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TendIcons.Badge("pen", 30.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(tr("Write your own", "Írd be a sajátod"), style = MaterialTheme.typography.titleMedium, color = Brand.Moss, modifier = Modifier.weight(1f))
                        Text("+", style = MaterialTheme.typography.headlineSmall, color = Brand.Moss)
                    }
                    Spacer(Modifier.weight(1f))
                    val ideas = app.tend.core.Tasks.presets.filter { p -> tasks.none { it.id == p.id } }.take((6 - (tasks.size / 2) * 2).coerceAtLeast(0))
                    if (ideas.isNotEmpty() && !full) {
                        Text(tr("Or pick an idea", "Vagy válassz egy ötletet"), style = MaterialTheme.typography.labelSmall, color = Brand.Muted)
                        ideas.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { t -> TaskTile(t.title, t.icon, picked = false, Modifier.weight(1f)) { onToggleTask(t) } }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                // The end, guided: an empty camp; get the campfire, then tap it and watch.
                Step.CAMP -> {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val fire = app.tend.core.Things.byId("campfire")!!
                    val lit = placed[0] != null
                    // Tapping the fire: the companion walks over and roasts a marshmallow.
                    var roastStart by remember { mutableStateOf(-1L) }
                    var frame by remember { mutableStateOf(0L) }
                    val roasting = roastStart >= 0
                    LaunchedEffect(roasting) {
                        if (!roasting) return@LaunchedEffect
                        while (true) androidx.compose.runtime.withFrameNanos { frame = it; if (roastStart == 0L) roastStart = it }
                    }
                    StepPage(cta = if (!lit) tr("Get the campfire first", "Előbb szerezd meg a tábortüzet") else tr("Start", "Indítás"), ctaEnabled = lit, onCta = onFinish) {
                        Says(avatar,
                            tr("Your camp", "A táborod"),
                            when {
                                !lit -> tr("${avatar.displayName} will live here. Start with a campfire.", "Itt fog élni ${avatar.displayName}. Kezdd egy tábortűzzel.")
                                !roasting -> tr("Lovely! Now tap the fire.", "Szép! Most koppints a tűzre.")
                                else -> tr("${avatar.displayName} uses everything you get.", "${avatar.displayName} mindent használ, amit szerzel.")
                            },
                            figure = 0.dp, smallLines = 2,
                        )
                        val appear = rememberAppear(placed)
                        val fireBuild = rememberFireBuild(placed)
                        val ambient = rememberAmbientClock()
                        Box(Modifier.fillMaxWidth().aspectRatio(1.35f).clip(RoundedCornerShape(26.dp)).campEntrance()) {
                            Canvas(
                                // When setup ends, this same camp glides up into home's.
                                Modifier.fillMaxSize().then(sharedCamp() ?: Modifier).pointerInput(lit) {
                                    detectTapGestures { tap ->
                                        val p = SpotPositions.getValue(0)
                                        val near = kotlin.math.abs(tap.x / size.width - p.x) < 0.18f && kotlin.math.abs(tap.y / size.height - p.y + 0.05f) < 0.2f
                                        if (lit && near && !roasting && fireBuild == null) {
                                            roastStart = 0L
                                            Sounds.forUse(context, "campfire", (Uses.WALK * 1000).toLong())
                                            onUsed("campfire")
                                        }
                                    }
                                },
                            ) {
                                val use = if (roasting && roastStart > 0) InUse(0, "campfire", (frame - roastStart) / 1e9f, Uses.WALK) else null
                                drawClearing(avatar, placed, Mood.HAPPY, appear = appear, use = use, ambient = ambient.value, fireBuild = fireBuild)
                            }
                            AcornBadge(acorns, Modifier.align(Alignment.TopEnd).padding(10.dp))
                        }
                        // What acorns are for and where they come from: shown once Bo starts on the fire (something to read while it's made).
                        androidx.compose.animation.AnimatedVisibility(lit, enter = androidx.compose.animation.fadeIn(tween(700)) + androidx.compose.animation.expandVertically(tween(500))) { AcornWays() }
                        if (!lit) Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            ShopTile(fire, owned = false, affordable = acorns >= fire.price, Modifier.weight(1f), background = Brand.Card) { onBuy(fire.id) }
                            Text(
                                tr("Your first thing: the campfire. Tap it to buy it.", "Az első tárgyad: a tábortűz. Koppints rá a vásárláshoz."),
                                style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(2f),
                            )
                        }
                        // Once the fire's going: the welcome gift, said plainly, with when it ends.
                        if (roasting) Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brand.Plus.copy(alpha = 0.14f)).padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                val onSale = app.tend.data.TendStore.PLUS_ON_SALE
                                Text(if (onSale) tr("A welcome gift: 14 days of tend Plus", "Üdvözlő ajándék: 14 nap tend Plus") else tr("A welcome gift: tend Plus, on us", "Üdvözlő ajándék: tend Plus, a vendégünk vagy"), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    tr("Every week of your time, a limit for each app, strict mode, and the season's own things. ", "A heteid részletesen, saját limit minden appnak, a Szigorú fokozat és az évszak saját tárgyai. ") +
                                        if (onSale) tr("Then it's your choice: Plus, or the free tend.", "Utána te döntesz: Plus, vagy az ingyenes tend.") else tr("All yours, free, while tend is new.", "Mind a tiéd, ingyen, amíg a tend új."),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = Brand.Muted,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            PlusMark()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Progress(step: Int, total: Int, onBack: () -> Unit) =
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        // On the first page there's nowhere to go back to: an empty space of the same size, so nothing moves.
        Box(Modifier.alpha(if (step > 1) 1f else 0f)) { IconButtonSoft(TendIcons.vector("back"), tr("Back", "Vissza"), if (step > 1) onBack else ({})) }
        Spacer(Modifier.width(Space.m))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 1 until total) {
                val fill by animateFloatAsState(if (i <= step) 1f else 0f, label = "progress")
                Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(Brand.Mint.copy(alpha = 0.3f))) {
                    Box(Modifier.fillMaxWidth(fill).height(6.dp).clip(CircleShape).background(Brand.Moss))
                }
            }
        }
        Spacer(Modifier.width(Space.l))
    }

@Composable
private fun StepPage(
    cta: String,
    onCta: () -> Unit,
    ctaEnabled: Boolean = true,
    centered: Boolean = false,
    /** False for pages made to fit: nothing to scroll. */
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) = Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
    Column(
        Modifier.weight(1f).fillMaxWidth().then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = if (centered) Arrangement.Center else Arrangement.spacedBy(12.dp),
        content = content,
    )
    PrimaryButton(cta, onCta, ctaEnabled, Modifier.padding(vertical = 20.dp))
}

@Composable
private fun Title(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 8.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Brand.Muted)
    Spacer(Modifier.height(4.dp))
}

@Composable
fun BobbingCharacter(avatar: Avatar, mood: Mood, size: Dp) {
    val t = rememberInfiniteTransition(label = "bob")
    val y by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y")
    CharacterView(
        avatar, mood, size,
        Modifier.graphicsLayer {
            translationY = -10f * y * density
            scaleX = 1f + 0.03f * (1 - y); scaleY = 1f - 0.03f * (1 - y)
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
        },
    )
}

/** English or Magyar, each in its own words; picking the other one starts the app again in it. */
@Composable
fun LanguagePicker(modifier: Modifier = Modifier, plain: Color = Brand.Card) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("en" to "English", "hu" to "Magyar").forEach { (code, label) ->
            val on = code == app.tend.core.Lang.code
            Text(
                label, style = MaterialTheme.typography.labelLarge, color = if (on) Color.White else Brand.Moss, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (on) Brand.Moss else plain)
                    .clickable { app.tend.TendApp.setLanguage(context, code) }.padding(vertical = 10.dp),
            )
        }
    }
}

@Composable
fun CompanionPicker(avatar: Avatar, onAvatar: (Avatar) -> Unit, compact: Boolean = false) =
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Avatar.entries.forEach { a ->
            val selected = a == avatar
            // Settings: a small face and the name side by side.
            if (compact) Row(
                Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(if (selected) Brand.Picked else Brand.Card)
                    .border(if (selected) 2.dp else 1.dp, if (selected) Brand.Moss else Brand.Mint.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .clickable { onAvatar(a) }.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CharacterView(a, Mood.HAPPY, 48.dp)
                Spacer(Modifier.width(8.dp))
                Text(a.displayName, style = MaterialTheme.typography.titleMedium, color = if (selected) Brand.Moss else Brand.Ink)
                if (selected) {
                    Spacer(Modifier.width(6.dp))
                    androidx.compose.material3.Icon(TendIcons.vector("check"), null, tint = Brand.Moss, modifier = Modifier.size(18.dp))
                }
            } else Column(
                Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(Brand.Card)
                    .border(3.dp, if (selected) Brand.Moss else Color.Transparent, RoundedCornerShape(24.dp))
                    .clickable { onAvatar(a) }.padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (selected) BobbingCharacter(a, Mood.HAPPY, 110.dp) else CharacterView(a, Mood.HAPPY, 110.dp)
                Text(a.displayName, style = MaterialTheme.typography.titleLarge)
            }
        }
    }

@Composable
fun AppPicker(catalog: AppCatalog, blocked: Set<String>, onToggle: (String) -> Unit) {
    var showOthers by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    if (catalog.suggested.isNotEmpty()) {
        if (catalog.dailyMinutes.isNotEmpty()) Text(tr("Your most used apps and social media, most used first.", "A legtöbbet használt appjaid és a közösségi média, a legtöbbet használt elöl."), style = MaterialTheme.typography.bodyMedium)
    } else {
        Text(tr("None of the usual suspects are installed. Search below.", "A szokásos gyanúsítottak közül egy sincs telepítve. Keress lent."), style = MaterialTheme.typography.bodyMedium)
    }
    // Picked from the full list: they join the big tiles, next to the others.
    val extra = catalog.others.filter { it.pkg in blocked }
    val tiles = catalog.suggested + extra
    if (tiles.isNotEmpty()) AppGrid(catalog, tiles, blocked, onToggle)

    // One tap for everything that takes real time: apps used 20+ minutes a day over the last two weeks.
    val toAdd = AppCatalog.heavyToAdd(catalog.dailyMinutes, catalog.all.map { it.pkg }, blocked)
    if (toAdd.isNotEmpty()) Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brand.Mint.copy(alpha = 0.3f))
            .border(1.5.dp, Brand.Moss.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .clickable { toAdd.forEach(onToggle) }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(tr("Pick all I use a lot", "Mindet, amit sokat használok"), style = MaterialTheme.typography.titleSmall, color = Brand.Moss)
            Text(
                tr("${toAdd.size} more app${if (toAdd.size == 1) "" else "s"} you use ${AppCatalog.HEAVY_MINUTES}+ minutes a day", "Még ${toAdd.size} app, amit napi ${AppCatalog.HEAVY_MINUTES}+ percet használsz"),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted,
            )
        }
        androidx.compose.material3.Icon(TendIcons.vector("plus"), null, tint = Brand.Moss)
    }

    SecondaryButton(if (showOthers) tr("Hide other apps", "Többi app elrejtése") else tr("Add another app", "Másik app hozzáadása"), { showOthers = !showOthers })
    AnimatedVisibility(showOthers) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text(tr("Search apps", "Keresés az appok között")) }, singleLine = true,
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
            )
            val matches = remember(query) {
                catalog.others.filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }
            }
            matches.forEach { app -> AppRow(catalog, app, app.pkg in blocked, onToggle) }
        }
    }
}

/** Three equal columns; a short last row keeps the same tile width. */
@Composable
private fun AppGrid(catalog: AppCatalog, apps: List<InstalledApp>, blocked: Set<String>, onToggle: (String) -> Unit) =
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Four to a row, two rows: the eight used most.
        apps.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { AppTile(catalog, it, it.pkg in blocked, onToggle, Modifier.weight(1f).fillMaxHeight()) }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }

@Composable
private fun AppTile(catalog: AppCatalog, app: InstalledApp, selected: Boolean, onToggle: (String) -> Unit, modifier: Modifier) = Column(
    modifier.clip(RoundedCornerShape(20.dp))
        .background(if (selected) Brand.Mint.copy(alpha = 0.35f) else Brand.Card)
        .border(2.dp, if (selected) Brand.Moss else Color.Transparent, RoundedCornerShape(20.dp))
        .clickable { onToggle(app.pkg) }.padding(vertical = 10.dp, horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Box {
        AppIcon(catalog, app.pkg, 44.dp, Modifier.graphicsLayer { alpha = if (selected) 1f else 0.45f })
        if (selected) Box(
            Modifier.align(Alignment.BottomEnd).size(18.dp).clip(CircleShape).background(Brand.Moss),
            contentAlignment = Alignment.Center,
        ) { androidx.compose.material3.Icon(TendIcons.vector("check"), null, tint = Color.White, modifier = Modifier.size(12.dp)) }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        app.label, style = MaterialTheme.typography.bodyMedium, color = Brand.Ink, maxLines = 1, textAlign = TextAlign.Center,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
    )
    catalog.dailyMinutes[app.pkg]?.let { min ->
        Text(
            if (min >= 60) tr("${min / 60} h ${min % 60} min a day", "napi ${min / 60} ó ${min % 60} p") else tr("$min min a day", "napi $min perc"),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = Brand.Muted, maxLines = 1, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AppRow(catalog: AppCatalog, app: InstalledApp, selected: Boolean, onToggle: (String) -> Unit) = Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(if (selected) Brand.Mint.copy(alpha = 0.35f) else Brand.Card)
        .border(2.dp, if (selected) Brand.Moss else Color.Transparent, RoundedCornerShape(16.dp))
        .clickable { onToggle(app.pkg) }.padding(10.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    AppIcon(catalog, app.pkg, 32.dp)
    Spacer(Modifier.width(12.dp))
    Text(app.label, style = MaterialTheme.typography.bodyLarge, color = if (selected) Brand.Moss else Brand.Ink, modifier = Modifier.weight(1f))
    Box(
        Modifier.size(26.dp).clip(CircleShape).background(if (selected) Brand.Moss else Brand.Mint.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            if (selected) TendIcons.vector("check") else TendIcons.vector("plus"),
            null, tint = if (selected) Color.White else Brand.Moss, modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun AppIcon(catalog: AppCatalog, pkg: String, size: Dp, modifier: Modifier = Modifier) {
    val icon = remember(pkg) { catalog.icon(pkg) }
    if (icon != null) Image(icon, null, modifier.size(size))
    else Box(modifier.size(size).clip(RoundedCornerShape(10.dp)).background(Brand.Mint))
}


/** The two steppers on their own, for a card that holds more. */
@Composable
fun BudgetSteppers(cfg: BudgetConfig, onConfig: (BudgetConfig) -> Unit) {
    Stepper(tr("Free every day", "Minden nap jár"), tr("No effort needed", "Semmit nem kell tenned érte"), listOf(15, 30, 45, 60), cfg.freeMinutes) {
        onConfig(cfg.copy(freeMinutes = it, totalMinutes = maxOf(cfg.totalMinutes, it)))
    }
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(Brand.Sage))
    Stepper(tr("Max per day", "Napi maximum"), tr("With earned minutes", "A megszerzett percekkel együtt"), listOf(90, 120, 180, 240), cfg.totalMinutes) {
        onConfig(cfg.copy(totalMinutes = it, freeMinutes = minOf(cfg.freeMinutes, it)))
    }
    Text(tr("At 0 the picked apps stay closed until you earn more with an activity. Every day starts fresh.", "Nullánál a kiválasztott appok zárva maradnak, amíg egy tevékenységgel nem szerzel többet. Minden nap tiszta lappal indul."), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
}

/** Other apps of these kinds (not picked) count against the minutes too, with no Megállító. */
@Composable
fun CountedKinds(cfg: BudgetConfig, onConfig: (BudgetConfig) -> Unit) {
    Text(tr("The rest count too", "A többi is számít"), style = MaterialTheme.typography.titleMedium)
    Text(tr("Other social, video and game apps you didn't pick: no pause, their time just comes off the same minutes", "A nem kiválasztott közösségi, videós és játékappok: Megállító nélkül, csak az idejük fogy ugyanabból a keretből"), style = MaterialTheme.typography.bodyMedium)
    Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(AppKinds.SOCIAL to tr("Social", "Közösségi"), AppKinds.VIDEO to tr("Video", "Videó"), AppKinds.GAMES to tr("Games", "Játékok")).forEach { (kind, label) ->
            val on = kind in cfg.counted
            Text(
                label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), textAlign = TextAlign.Center,
                color = if (on) Color.White else Brand.Moss,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (on) Brand.Moss else Brand.Sage)
                    .border(1.dp, if (on) Brand.Moss else Brand.Mint, RoundedCornerShape(14.dp))
                    .clickable { onConfig(cfg.copy(counted = if (on) cfg.counted - kind else cfg.counted + kind)) }.padding(vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun Stepper(title: String, hint: String, options: List<Int>, value: Int, onPick: (Int) -> Unit) =
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(hint, style = MaterialTheme.typography.bodyMedium)
        }
        val i = options.indexOf(value).let { if (it < 0) options.indexOfFirst { o -> o >= value }.coerceAtLeast(0) else it }
        StepButton("−", i > 0) { onPick(options[i - 1]) }
        Text(
            tr("$value min", "$value perc"), style = MaterialTheme.typography.titleMedium, color = Brand.Moss, textAlign = TextAlign.Center,
            modifier = Modifier.width(70.dp),
        )
        StepButton("+", i < options.lastIndex) { onPick(options[i + 1]) }
    }

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) = Box(
    Modifier.size(36.dp).clip(CircleShape).background(if (enabled) Brand.Mint.copy(alpha = 0.25f) else Brand.Sage)
        .clickable(enabled = enabled, onClick = onClick),
    contentAlignment = Alignment.Center,
) { Text(label, style = MaterialTheme.typography.titleLarge, color = if (enabled) Brand.Moss else Brand.Muted.copy(alpha = 0.4f)) }

@Composable
fun PermissionSteps(perms: PermissionActions) {
    val p = perms.permissions
    val unlock = perms.lock != PermissionLock.NONE
    if (unlock) UnlockCard(perms)
    val n = if (unlock) 1 else 0
    // Overlay first: once it's granted, tend can bring itself back to the front after each switch.
    PermissionCard(
        number = n + 1, title = tr("Display over other apps", "Megjelenítés más appok fölött"), why = tr("So the apps can close when your minutes run out (and a pause can show, if you ask for one).", "Hogy az appok bezárhassanak, ha elfogyott a perced, és megjelenhessen a Megállító."),
        granted = p.overlay, active = !unlock && !p.overlay, blocked = perms.overlayBlocked,
        onAllow = perms.requestOverlay, onAppInfo = perms.openAppInfo,
    )
    PermissionCard(
        number = n + 2, title = tr("Usage access", "Használati hozzáférés"), why = tr("So screen time is measured on your phone, not guessed.", "Hogy a képernyőidőt a telefonod mérje, ne csak becsüljük."),
        granted = p.usageAccess, active = !unlock && p.overlay && !p.usageAccess, blocked = perms.usageBlocked,
        onAllow = perms.requestUsage, onAppInfo = perms.openAppInfo,
    )
    if (!p.allGranted) Text(
        tr("After each step tend brings you back here and shows what's next.", "Minden lépés után a tend visszahoz ide, és megmutatja, mi jön."),
        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Step 1 for side-loaded installs on Android 13+. The unlock option in App info only exists after
 * Android has refused a switch once, so it's two short moves, each one button.
 */
@Composable
private fun UnlockCard(perms: PermissionActions) = Column(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brand.Card).border(2.dp, Brand.Moss, RoundedCornerShape(22.dp)).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    // Android keeps these switches shut for apps from outside the Play Store, until it's been asked once.
    val tripped = perms.lock == PermissionLock.TRIPPED
    Text(tr("First, let Android allow it", "Előbb engedje az Android"), style = MaterialTheme.typography.titleMedium)
    Text(
        if (tripped) tr("In App info, tap ⋮ (top right) → Allow restricted settings, then come back.", "Az Alkalmazásinfóban: ⋮ (jobb fent) → Korlátozott beállítások engedélyezése, aztán gyere vissza.")
        else tr("Tap below. If Android says it's restricted, tap OK and come back.", "Koppints lent. Ha az Android azt írja, hogy korlátozott, nyomj OK-t, és gyere vissza."),
        style = MaterialTheme.typography.bodyMedium, color = Brand.Muted,
    )
    if (tripped) PrimaryButton(tr("Open App info", "Alkalmazásinfó megnyitása"), perms.unlockAppInfo)
    else PrimaryButton(tr("Start", "Indítás"), perms.startUnlock)
    LinkText(tr("Already done? Skip", "Már megvan? Kihagyom"), perms.skipUnlock)
}


@Composable
private fun PermissionCard(
    number: Int, title: String, why: String,
    granted: Boolean, active: Boolean, blocked: Boolean,
    onAllow: () -> Unit, onAppInfo: () -> Unit,
) = Column(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brand.Card)
        .border(2.dp, if (active) Brand.Moss else Color.Transparent, RoundedCornerShape(22.dp))
        .padding(16.dp)
        .graphicsLayer { alpha = if (granted || active) 1f else 0.55f },
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(if (granted) Brand.Moss else Brand.Sage),
            contentAlignment = Alignment.Center,
        ) {
            if (granted) androidx.compose.material3.Icon(TendIcons.vector("check"), null, tint = Color.White, modifier = Modifier.size(18.dp))
            else Text("$number", color = Brand.Moss, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(why, style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (active) {
        if (blocked) RestrictedHelp(onAppInfo)
        PrimaryButton(if (blocked) tr("Try again", "Újra") else tr("Allow", "Engedélyezem"), onAllow)
        if (!blocked) Text(tr("Find tend and switch it on. We'll bring you right back.", "Keresd meg a tendet, és kapcsold be. Rögtön visszahozunk."), style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Android 13+ blocks these switches for apps installed outside an app store ("restricted settings").
 * The user can lift it once from App info.
 */
@Composable
private fun RestrictedHelp(onAppInfo: () -> Unit) = Column(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.Sunny.copy(alpha = 0.35f)).padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
) {
    Text(tr("Android said no? That's expected.", "Az Android nemet mondott? Ez várható."), style = MaterialTheme.typography.titleMedium)
    Text(
        tr("Phones block this switch for apps not installed from the Play Store. A one-time fix:", "A telefonok letiltják ezt a kapcsolót a Play Áruházon kívülről telepített appoknál. Egyszer kell megoldani:"),
        style = MaterialTheme.typography.bodyMedium, color = Brand.Ink,
    )
    Text(tr("1. Open app info\n2. Tap ⋮ in the top right corner\n3. Choose \"Allow restricted settings\"\n4. Come back and tap Try again", "1. Nyisd meg az alkalmazásinfót\n2. Koppints jobb fent a ⋮ ikonra\n3. Válaszd: „Korlátozott beállítások engedélyezése”\n4. Gyere vissza, és koppints az Újra gombra"), style = MaterialTheme.typography.bodyLarge)
    TextButton(onClick = onAppInfo) { Text(tr("Open app info →", "Alkalmazásinfó megnyitása →")) }
}

/**
 * Asks the launcher to pin the widget. Some launchers say yes and then show nothing, so if the
 * widget isn't on the home screen a few seconds later, a short guide takes over. Once it's
 * placed, the button turns into a confirmation.
 */
@Composable
fun WidgetButton(avatar: Avatar, onAddWidget: () -> Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isPlaced by remember { mutableStateOf(WidgetKind.anyPlaced(context)) }
    var guide by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (!isPlaced) { kotlinx.coroutines.delay(1500); isPlaced = WidgetKind.anyPlaced(context) } }

    if (isPlaced) Row(
        Modifier.fillMaxWidth().surface(radius = 18.dp).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        androidx.compose.material3.Icon(TendIcons.vector("check"), null, tint = Brand.Moss)
        Spacer(Modifier.width(Space.s))
        Text(tr("${avatar.displayName} is on your home screen", "${avatar.displayName} kint van a kezdőképernyődön"), style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
    }
    if (isPlaced) Text(
        tr("Tapping ${avatar.displayName} opens tend, so the tend icon can go: ${avatar.displayName} takes its place.", "Ha rákoppintasz, megnyílik a tend, úgyhogy a tend ikon mehet: ${avatar.displayName} átveszi a helyét."),
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    ) else {
        SecondaryButton(tr("Put ${avatar.displayName} on your home screen", "Tedd ki a kezdőképernyődre: ${avatar.displayName}"), { if (!onAddWidget()) guide = true })
        // The bigger ones (with minutes, the camp) are in the phone's own widget list.
        Text(
            tr("Other sizes in your phone's widget list", "Más méretek a telefonod widgetlistájában"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { guide = true }.padding(vertical = 8.dp),
        )
    }

    if (guide) WidgetGuideDialog(avatar) { guide = false }
}

/** The three home screen widgets (1x1 face, 2x1 with minutes, 2x2 camp). */
enum class WidgetKind(val provider: Class<*>) {
    FACE(app.tend.widget.TendWidgetFace::class.java),
    WIDE(app.tend.widget.TendWidget::class.java),
    GARDEN(app.tend.widget.TendWidgetGarden::class.java);

    companion object {
        fun anyPlaced(context: android.content.Context): Boolean {
            val m = android.appwidget.AppWidgetManager.getInstance(context)
            return entries.any { m.getAppWidgetIds(android.content.ComponentName(context, it.provider)).isNotEmpty() }
        }
    }
}

/** Three steps to add the widget by hand: most launchers don't let apps place it themselves. */
@Composable
fun WidgetGuideDialog(avatar: Avatar, onDone: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onDone) {
        Column(
            Modifier.fillMaxWidth().surface(radius = 28.dp).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            Text(tr("Put ${avatar.displayName} on your home screen", "Tedd ki a kezdőképernyődre: ${avatar.displayName}"), style = MaterialTheme.typography.titleLarge)
            Text(tr("The 1×1 one is just ${avatar.displayName}: put it where the tend icon is, and you see how they're doing each time you look. There's also one with your minutes, and the camp.", "Az 1×1-es csak ${avatar.displayName} maga: tedd a tend ikon helyére, és valahányszor odanézel, látod, hogy van. Van olyan is, ami a perceidet mutatja, és olyan is, ami a tábort."), style = MaterialTheme.typography.bodyMedium)
            listOf(
                tr("Long-press an empty spot on your home screen", "Nyomj hosszan egy üres helyre a kezdőképernyőn"),
                tr("Tap \"Widgets\" and find tend", "Koppints a „Widgetek” gombra, és keresd meg a tendet"),
                tr("Drag the one you like onto the screen", "Húzd a képernyőre azt, amelyik tetszik"),
                tr("Swap it in for the tend icon: tapping ${avatar.displayName} opens tend", "Cseréld le vele a tend ikont: ha rákoppintasz, megnyílik a tend"),
            ).forEachIndexed { i, step ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(Brand.Mint.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
                    }
                    Spacer(Modifier.width(Space.m))
                    Text(step, style = MaterialTheme.typography.bodyLarge)
                }
            }
            PrimaryButton(tr("Go to home screen", "Ugrás a kezdőképernyőre"), {
                onDone()
                context.startActivity(
                    android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            })
            SecondaryButton(tr("Later", "Később"), onDone)
        }
    }
}

@Composable
fun LinkText(text: String, onClick: () -> Unit) = Text(
    text, style = MaterialTheme.typography.labelLarge, color = Brand.Moss,
    modifier = Modifier.tappable(onClick = onClick).padding(vertical = Space.s),
)

/** What still works if a switch stays off, so skipping is an informed choice, not a dead end. */
@Composable
fun LimitedModeDialog(p: Permissions, onConfirm: () -> Unit, onDismiss: () -> Unit) =
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().surface(radius = 28.dp).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            Text(tr("Continue in limited mode?", "Folytatod korlátozott módban?"), style = MaterialTheme.typography.titleLarge)
            if (!p.usageAccess) Text(
                tr("Without usage access, tend can't see screen time or app opens. Activities, the reading timer and the camp still work.", "Használati hozzáférés nélkül a tend nem látja a képernyőidőt és az appmegnyitásokat. A tevékenységek, az olvasásidőzítő és a tábor így is működnek."),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (p.usageAccess && !p.overlay) Text(
                tr("Without \"display over other apps\", there's no pause before scrolling. tend still counts your minutes and sends a gentle notification instead.", "A „megjelenítés más appok fölött” engedély nélkül nincs Megállító görgetés előtt. A tend így is számolja a perceidet, és helyette egy halk értesítést küld."),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(tr("You can turn the switches on any time from the home screen.", "A kapcsolókat a főoldalról bármikor bekapcsolhatod."), style = MaterialTheme.typography.bodyMedium)
            PrimaryButton(tr("Continue anyway", "Folytatom így"), onConfirm)
            SecondaryButton(tr("Set it up now", "Beállítom most"), onDismiss)
        }
    }

/** The companion, big, saying it: a line (and a smaller one under it) in a speech bubble. */
@Composable
private fun Says(avatar: Avatar, line: String, small: String? = null, figure: Dp = 110.dp, smallLines: Int = 1) = Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    // No figure (the camp shows the companion itself): just the bubble.
    if (figure > 0.dp) {
        BobbingCharacter(avatar, Mood.HAPPY, figure)
        // The bubble's little point, up towards the companion.
        androidx.compose.foundation.Canvas(Modifier.size(18.dp, 9.dp)) {
            drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(0f, size.height); lineTo(size.width / 2, 0f); lineTo(size.width, size.height); close() }, Brand.Card)
        }
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brand.Card).padding(horizontal = 18.dp, vertical = 14.dp)) {
        Text(line, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        small?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Brand.Muted, textAlign = TextAlign.Center, minLines = smallLines, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) }
    }
}

/** Where acorns come from, at a glance: three small tiles, each a picture and what it gives. */
@Composable
private fun AcornWays() = Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AcornIcon(18.dp)
        Spacer(Modifier.width(8.dp))
        Text(tr("You collect acorns and buy things for the camp with them.", "Makkot gyűjtesz, és tárgyakat veszel belőle a táborba."), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple("workout", "+${app.tend.core.Acorns.PER_ACTIVITY}", tr("for an activity", "tevékenységért")),
            Triple("hourglass", "+${app.tend.core.Acorns.WITHIN_BUDGET}", tr("a day within your minutes", "ha a kereten belül maradsz")),
            Triple("face_yes", "+${app.tend.core.Acorns.PER_NOT_NOW}", tr("for each \"Not now\"", "minden „Most nem”-ért")),
        ).forEach { (icon, plus, what) ->
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Brand.Card).padding(vertical = 10.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TendIcons.Badge(icon, 26.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(plus, style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
                }
                Text(what, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 15.sp), color = Brand.Muted, textAlign = TextAlign.Center, minLines = 2, maxLines = 2)
            }
        }
    }
}

/** One line of "how it works": a small picture and a sentence. */
@Composable
private fun Point(icon: String, text: String) = Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brand.Card).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
    TendIcons.Badge(icon, 36.dp)
    Spacer(Modifier.width(12.dp))
    Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
}

/** One switch to turn on: what it's for, and one button (or, when Android keeps it closed, the way to App info). */
@Composable
private fun OneSwitch(title: String, why: String, done: Boolean, next: Boolean, blocked: Boolean, onAllow: () -> Unit, onAppInfo: () -> Unit) = Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(if (done) Brand.Mint.copy(alpha = 0.25f) else Brand.Card)
        .border(2.dp, if (next) Brand.Moss else Color.Transparent, RoundedCornerShape(22.dp)).padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(why, style = MaterialTheme.typography.bodyMedium, color = Brand.Muted)
    }
    Spacer(Modifier.width(12.dp))
    androidx.compose.animation.AnimatedContent(done, label = "switch") { on ->
        if (on) Box(Modifier.size(40.dp).clip(CircleShape).background(Brand.Moss), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(TendIcons.vector("check"), tr("On", "Bekapcsolva"), tint = Color.White, modifier = Modifier.size(22.dp))
        }
        else ChipButton(if (blocked) tr("App info", "Alkalmazásinfó") else tr("Allow", "Engedélyezem"), if (blocked) onAppInfo else onAllow, filled = next)
    }
}

/** Any app on the phone, by name: a search in a small window. */
@Composable
private fun AppSearch(catalog: AppCatalog, blocked: Set<String>, onToggle: (String) -> Unit, onClose: () -> Unit) = androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
    var query by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().then(Modifier.height(520.dp)).clip(RoundedCornerShape(26.dp)).background(Brand.Sage).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(query, { query = it }, placeholder = { Text(tr("Search apps", "Keresés az appok között")) }, singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
        val matches = remember(query) { catalog.all.filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) } }
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            matches.forEach { app -> AppRow(catalog, app, app.pkg in blocked, onToggle) }
        }
        PrimaryButton(tr("Done", "Kész"), onClose)
    }
}

/** A task as an even tile in the grid: picked ones filled with a tick; "add your own" with a plus. */
@Composable
private fun TaskTile(label: String, icon: String, picked: Boolean, modifier: Modifier, plus: Boolean = false, onClick: () -> Unit) = Row(
    modifier.height(52.dp).clip(RoundedCornerShape(14.dp)).background(if (picked) Brand.Mint.copy(alpha = 0.4f) else Brand.Card)
        .border(1.5.dp, if (picked) Brand.Moss else Color.Transparent, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    TendIcons.Badge(icon, 26.dp)
    Spacer(Modifier.width(8.dp))
    Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, lineHeight = 15.sp), maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    if (picked) androidx.compose.material3.Icon(TendIcons.vector("check"), null, tint = Brand.Moss, modifier = Modifier.size(16.dp))
    if (plus) Text("+", style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
}
