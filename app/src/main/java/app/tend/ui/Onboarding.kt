package app.tend.ui

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tend.core.Avatar
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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

private enum class Step { INTRO, COMPANION, USAGE, APPS, ACTIVITIES, PAUSE, ACCOUNT, CAMP }

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
    /** The camp at the end: what's in it, the acorns, getting the campfire lit, and buying a first thing. */
    placed: Map<Int, String> = emptyMap(),
    acorns: Int = 0,
    onCampReady: () -> Unit = {},
    onBuy: (String) -> Boolean = { false },
    onFinish: () -> Unit,
) {
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = Step.entries[stepIndex]
    val next = { stepIndex = (stepIndex + 1).coerceAtMost(Step.entries.lastIndex) }
    // System back goes one step back instead of leaving the app mid-setup.
    androidx.activity.compose.BackHandler(enabled = stepIndex > 0) { stepIndex-- }

    Column(Modifier.fillMaxSize().background(Brand.Sage)) {
        if (step != Step.INTRO) {
            Progress(stepIndex, Step.entries.size) { stepIndex-- }
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                val motion = androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntOffset>(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
                slideInHorizontally(motion) { it * dir } togetherWith slideOutHorizontally(motion) { -it * dir }
            },
            modifier = Modifier.weight(1f),
            label = "onboarding",
        ) { s ->
            when (s) {
                Step.INTRO -> IntroSlides(avatar, cfg.freeMinutes, onDone = next)

                Step.COMPANION -> StepPage(cta = "Continue with ${avatar.displayName}", onCta = next) {
                    Title("Who's keeping you company?", "Same heart, different hair. You can switch later.")
                    CompanionPicker(avatar, onAvatar)
                }

                Step.USAGE -> StepPage(cta = "Continue", ctaEnabled = perms.permissions.usageAccess, onCta = next) {
                    Title(
                        "Which apps take your time?",
                        "Allow usage access and ${avatar.displayName} will suggest the apps you use most, with minutes a day. It's read on your phone and never leaves it.",
                    )
                    if (perms.lock != PermissionLock.NONE && !perms.permissions.usageAccess) UnlockCard(perms)
                    PermissionCard(
                        number = if (perms.lock != PermissionLock.NONE) 2 else 1, title = "Usage access", why = "Minutes a day for each app, from the last two weeks.",
                        granted = perms.permissions.usageAccess, active = !perms.permissions.usageAccess, blocked = perms.usageBlocked,
                        onAllow = perms.requestUsage, onAppInfo = perms.openAppInfo,
                    )
                    if (!perms.permissions.usageAccess) LinkText("Skip, I'll pick the apps myself", next)
                }

                Step.APPS -> StepPage(
                    cta = if (blocked.isEmpty()) "Pick at least one" else "Continue with ${blocked.size} app${if (blocked.size == 1) "" else "s"}",
                    ctaEnabled = blocked.isNotEmpty(),
                    onCta = next,
                ) {
                    Title(
                        "Which apps pull you in?",
                        if (catalog.dailyMinutes.isNotEmpty()) "Picked for you: social apps and everything you use ${AppCatalog.HEAVY_MINUTES}+ minutes a day. Tap to change."
                        else "We picked the usual suspects. Tap to change.",
                    )
                    AppPicker(catalog, blocked, onToggleApp)
                }

                Step.ACTIVITIES -> {
                    LaunchedEffect(Unit) { onDefaultTasks(false) }
                    StepPage(
                        cta = if (tasks.isEmpty()) "Pick at least one" else "Continue with ${tasks.size}",
                        ctaEnabled = tasks.isNotEmpty(),
                        onCta = next,
                    ) {
                        Title("What would you rather do?", "We picked three to start. Tap to change them, or add your own. Each one gives screen time back.")
                        ActivityPicker(tasks, onToggleTask)
                        LinkText("Skip, use the defaults", { onDefaultTasks(true); next() })
                    }
                }

                Step.PAUSE -> StepPage(
                    cta = if (perms.permissions.allGranted) "Continue" else "A few taps to go",
                    ctaEnabled = perms.permissions.allGranted,
                    onCta = next,
                ) {
                    var askSkip by rememberSaveable { mutableStateOf(false) }
                    if (askSkip) LimitedModeDialog(perms.permissions, onConfirm = { askSkip = false; next() }, onDismiss = { askSkip = false })
                    val first = blocked.firstOrNull()?.let(catalog::label) ?: "an app"
                    Title(
                        "Let ${avatar.displayName} notice",
                        "${if (perms.permissions.usageAccess) "One more switch" else "Two quick switches"} so ${avatar.displayName} can see when you open $first and give you a breath. Nothing leaves your phone.",
                    )
                    PermissionSteps(perms)
                    if (!perms.permissions.allGranted) LinkText("Continue without", { askSkip = true })
                }

                Step.ACCOUNT -> StepPage(cta = if (signedIn) "Continue" else "Skip for now", onCta = next) {
                    Title(
                        "Keep your camp safe",
                        "With an account your camp is backed up, and friends can visit it. Optional: tend works fully without one.",
                    )
                    account(next)
                }

                // The end: the camp, its fire already lit, and the first thing bought right here.
                Step.CAMP -> {
                    LaunchedEffect(Unit) { onCampReady() }
                    val firsts = listOf("wildflowers", "lantern", "woodpile")
                    val picked = placed.values.any { it in firsts }
                    StepPage(cta = if (picked) "Start" else "Pick your first thing", ctaEnabled = picked, onCta = onFinish) {
                        Title(
                            "Your camp",
                            "${avatar.displayName} lives here. Activities and \"Not now\" earn acorns; spend them on things, and ${avatar.displayName} uses every one.",
                        )
                        val appear = rememberAppear(placed)
                        Box(Modifier.fillMaxWidth().aspectRatio(1.35f).clip(RoundedCornerShape(26.dp)).campEntrance()) {
                            Canvas(Modifier.fillMaxSize()) { drawClearing(avatar, placed, Mood.HAPPY, appear = appear) }
                            AcornBadge(acorns, Modifier.align(Alignment.TopEnd).padding(10.dp))
                        }
                        Text(if (picked) "IN YOUR CAMP" else "PICK YOUR FIRST THING", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            firsts.mapNotNull { app.tend.core.Things.byId(it) }.forEach { t ->
                                ShopTile(t, owned = t.id in placed.values, affordable = !picked && acorns >= t.price, Modifier.weight(1f), background = Brand.Card) { if (!picked) onBuy(t.id) }
                            }
                        }
                        Text(
                            if (picked) "Nice. You have $acorns acorns left for the next one, and every activity adds more."
                            else "The campfire is on us. You have $acorns acorns: pick one of these to start.",
                            style = MaterialTheme.typography.bodyMedium, color = Brand.Muted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Progress(step: Int, total: Int, onBack: () -> Unit) =
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButtonSoft(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
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
    content: @Composable ColumnScope.() -> Unit,
) = Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
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
                    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Check, null, tint = Brand.Moss, modifier = Modifier.size(18.dp))
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
        if (catalog.dailyMinutes.isNotEmpty()) Text("Your most used apps and social media, most used first.", style = MaterialTheme.typography.bodyMedium)
    } else {
        Text("None of the usual suspects are installed. Search below.", style = MaterialTheme.typography.bodyMedium)
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
            Text("Pick all I use a lot", style = MaterialTheme.typography.titleSmall, color = Brand.Moss)
            Text(
                "${toAdd.size} more app${if (toAdd.size == 1) "" else "s"} you use ${AppCatalog.HEAVY_MINUTES}+ minutes a day",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted,
            )
        }
        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Add, null, tint = Brand.Moss)
    }

    SecondaryButton(if (showOthers) "Hide other apps" else "Add another app", { showOthers = !showOthers })
    AnimatedVisibility(showOthers) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Search apps") }, singleLine = true,
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
        apps.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { AppTile(catalog, it, it.pkg in blocked, onToggle, Modifier.weight(1f).fillMaxHeight()) }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }

@Composable
private fun AppTile(catalog: AppCatalog, app: InstalledApp, selected: Boolean, onToggle: (String) -> Unit, modifier: Modifier) = Column(
    modifier.clip(RoundedCornerShape(20.dp))
        .background(if (selected) Brand.Mint.copy(alpha = 0.35f) else Brand.Card)
        .border(2.dp, if (selected) Brand.Moss else Color.Transparent, RoundedCornerShape(20.dp))
        .clickable { onToggle(app.pkg) }.padding(vertical = 12.dp, horizontal = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Box {
        AppIcon(catalog, app.pkg, 44.dp, Modifier.graphicsLayer { alpha = if (selected) 1f else 0.45f })
        if (selected) Box(
            Modifier.align(Alignment.BottomEnd).size(18.dp).clip(CircleShape).background(Brand.Moss),
            contentAlignment = Alignment.Center,
        ) { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        app.label, style = MaterialTheme.typography.bodyMedium, color = Brand.Ink, maxLines = 1, textAlign = TextAlign.Center,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
    )
    catalog.dailyMinutes[app.pkg]?.let { min ->
        Text(
            if (min >= 60) "${min / 60} h ${min % 60} min a day" else "$min min a day",
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
            if (selected) androidx.compose.material.icons.Icons.Filled.Check else androidx.compose.material.icons.Icons.Filled.Add,
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

/** Free and maximum minutes in one card, each a compact stepper. */
@Composable
fun BudgetPicker(cfg: BudgetConfig, onConfig: (BudgetConfig) -> Unit) = Card {
    Stepper("Free every day", "No effort needed", listOf(15, 30, 45, 60), cfg.freeMinutes) {
        onConfig(cfg.copy(freeMinutes = it, totalMinutes = maxOf(cfg.totalMinutes, it)))
    }
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(Brand.Sage))
    Stepper("Max per day", "With earned minutes", listOf(90, 120, 180, 240), cfg.totalMinutes) {
        onConfig(cfg.copy(totalMinutes = it, freeMinutes = minOf(cfg.freeMinutes, it)))
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
            "$value min", style = MaterialTheme.typography.titleMedium, color = Brand.Moss, textAlign = TextAlign.Center,
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
        number = n + 1, title = "Display over other apps", why = "So the little pause can appear when you open a picked app.",
        granted = p.overlay, active = !unlock && !p.overlay, blocked = perms.overlayBlocked,
        onAllow = perms.requestOverlay, onAppInfo = perms.openAppInfo,
    )
    PermissionCard(
        number = n + 2, title = "Usage access", why = "So screen time is measured on your phone, not guessed.",
        granted = p.usageAccess, active = !unlock && p.overlay && !p.usageAccess, blocked = perms.usageBlocked,
        onAllow = perms.requestUsage, onAppInfo = perms.openAppInfo,
    )
    if (!p.allGranted) Text(
        "After each step tend brings you back here and shows what's next.",
        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Step 1 for side-loaded installs on Android 13+. The unlock option in App info only exists after
 * Android has refused a switch once, so it's two short moves, each one button.
 */
@Composable
private fun UnlockCard(perms: PermissionActions) = Column(
    Modifier.fillMaxWidth().surface(radius = 22.dp)
        .border(2.dp, Brand.Moss, RoundedCornerShape(22.dp)).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
) {
    val tripped = perms.lock == PermissionLock.TRIPPED
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Brand.Sage), contentAlignment = Alignment.Center) {
            Text("1", color = Brand.Moss, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Unlock tend's switches", style = MaterialTheme.typography.titleMedium)
            Text("Once. Android locks them for apps from outside the Play Store.", style = MaterialTheme.typography.bodyMedium)
        }
    }
    UnlockMove(1, "Tap tend's switch. Android says it's restricted: tap OK and come back", done = tripped, active = !tripped)
    UnlockMove(2, "In App info: ⋮ top right → Allow restricted settings → confirm, then come back", done = false, active = tripped)
    if (tripped) PrimaryButton("Open App info", perms.unlockAppInfo)
    else PrimaryButton("Start", perms.startUnlock)
    LinkText("Already unlocked? Skip", perms.skipUnlock)
}

@Composable
private fun UnlockMove(n: Int, text: String, done: Boolean, active: Boolean) = Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (active) Brand.Mint.copy(alpha = 0.18f) else Color.Transparent)
        .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Box(
        Modifier.size(24.dp).clip(CircleShape).background(if (done) Brand.Moss else Brand.Sage),
        contentAlignment = Alignment.Center,
    ) {
        if (done) androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
        else Text("$n", color = Brand.Moss, style = MaterialTheme.typography.labelLarge)
    }
    Spacer(Modifier.width(10.dp))
    Text(text, style = MaterialTheme.typography.bodyLarge, color = if (done) Brand.Muted else Brand.Ink)
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
            if (granted) androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
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
        PrimaryButton(if (blocked) "Try again" else "Allow", onAllow)
        if (!blocked) Text("Find tend and switch it on. We'll bring you right back.", style = MaterialTheme.typography.bodyMedium)
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
    Text("Android said no? That's expected.", style = MaterialTheme.typography.titleMedium)
    Text(
        "Phones block this switch for apps not installed from the Play Store. A one-time fix:",
        style = MaterialTheme.typography.bodyMedium, color = Brand.Ink,
    )
    Text("1. Open app info\n2. Tap ⋮ in the top right corner\n3. Choose \"Allow restricted settings\"\n4. Come back and tap Try again", style = MaterialTheme.typography.bodyLarge)
    TextButton(onClick = onAppInfo) { Text("Open app info →") }
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
        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Check, null, tint = Brand.Moss)
        Spacer(Modifier.width(Space.s))
        Text("${avatar.displayName} is on your home screen", style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
    }
    if (isPlaced) Text(
        "Tapping ${avatar.displayName} opens tend, so the tend icon can go: ${avatar.displayName} takes its place.",
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    ) else {
        SecondaryButton("Put ${avatar.displayName} on your home screen", { if (!onAddWidget()) guide = true })
        // The bigger ones (with minutes, the camp) are in the phone's own widget list.
        Text(
            "Other sizes in your phone's widget list", style = MaterialTheme.typography.labelLarge, color = Brand.Moss,
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
            Text("Put ${avatar.displayName} on your home screen", style = MaterialTheme.typography.titleLarge)
            Text("The 1×1 one is just ${avatar.displayName}: put it where the tend icon is, and you see how they're doing each time you look. There's also one with your minutes, and the camp.", style = MaterialTheme.typography.bodyMedium)
            listOf(
                "Long-press an empty spot on your home screen",
                "Tap \"Widgets\" and find tend",
                "Drag the one you like onto the screen",
                "Swap it in for the tend icon: tapping ${avatar.displayName} opens tend",
            ).forEachIndexed { i, step ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(Brand.Mint.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
                    }
                    Spacer(Modifier.width(Space.m))
                    Text(step, style = MaterialTheme.typography.bodyLarge)
                }
            }
            PrimaryButton("Go to home screen", {
                onDone()
                context.startActivity(
                    android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            })
            SecondaryButton("Later", onDone)
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
            Text("Continue in limited mode?", style = MaterialTheme.typography.titleLarge)
            if (!p.usageAccess) Text(
                "Without usage access, tend can't see screen time or app opens. Activities, the reading timer and the camp still work.",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (p.usageAccess && !p.overlay) Text(
                "Without \"display over other apps\", there's no pause before scrolling. tend still counts your minutes and sends a gentle notification instead.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text("You can turn the switches on any time from the home screen.", style = MaterialTheme.typography.bodyMedium)
            PrimaryButton("Continue anyway", onConfirm)
            SecondaryButton("Set it up now", onDismiss)
        }
    }
