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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import app.tend.core.Tasks

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
    /** Side-loaded on Android 13+: Android locks both switches until "Allow restricted settings". */
    val restricted: Boolean = false,
    /** The user already let Android say no once, so the unlock option now exists in App info. */
    val unlockTried: Boolean = false,
    val tryUnlock: () -> Unit = {},
    val skipUnlock: () -> Unit = {},
)

private enum class Step { WELCOME, COMPANION, HOW, APPS, ACTIVITIES, BUDGET, PERMISSIONS, DONE }

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
    tasks: List<TaskDef>,
    onToggleTask: (TaskDef) -> Unit,
    onAddWidget: () -> Boolean,
    onFinish: () -> Unit,
) {
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = Step.entries[stepIndex]
    val next = { stepIndex = (stepIndex + 1).coerceAtMost(Step.entries.lastIndex) }

    Column(Modifier.fillMaxSize().background(Brand.Sage)) {
        if (step != Step.WELCOME && step != Step.DONE) {
            Progress(stepIndex, Step.entries.size - 1) { stepIndex-- }
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally { it * dir / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it * dir / 3 } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "onboarding",
        ) { s ->
            when (s) {
                Step.WELCOME -> StepPage(cta = "Let's go", onCta = next, centered = true) {
                    BobbingCharacter(avatar, Mood.HAPPY, 180.dp)
                    Spacer(Modifier.height(24.dp))
                    Text("tend", style = MaterialTheme.typography.headlineLarge.copy(fontSize = MaterialTheme.typography.headlineLarge.fontSize * 1.6f), color = Brand.Moss)
                    Text("Take care of you. Offline.", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("A tiny companion that grows when you live your real day.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = Brand.Muted)
                }

                Step.COMPANION -> StepPage(cta = "Continue with ${avatar.displayName}", onCta = next) {
                    Title("Who's keeping you company?", "Same heart, different hair. You can switch later.")
                    CompanionPicker(avatar, onAvatar)
                }

                Step.HOW -> StepPage(cta = "Sounds good", onCta = next) {
                    Title("How it works", "No bans, no lectures. Just a small trade.")
                    HowRow("sun", "${cfg.freeMinutes} free minutes a day", "For the apps you pick. The rest you earn.")
                    HowRow("sprout", "Earn more by living", "Do the things you'd rather do, and get minutes back. You pick them next.")
                    HowRow("flower", "Unused minutes grow a garden", "Tonight, what you don't scroll becomes ${avatar.displayName}'s garden.")
                    HowRow("hourglass", "A little pause before you open", "5 seconds at first, a second more each time.")
                }

                Step.APPS -> StepPage(
                    cta = if (blocked.isEmpty()) "Pick at least one" else "Continue with ${blocked.size} app${if (blocked.size == 1) "" else "s"}",
                    ctaEnabled = blocked.isNotEmpty(),
                    onCta = next,
                ) {
                    Title("Which apps pull you in?", "We picked the usual suspects. Tap to change.")
                    AppPicker(catalog, blocked, onToggleApp)
                }

                Step.ACTIVITIES -> StepPage(
                    cta = if (tasks.isEmpty()) "Pick at least one" else "Continue with ${tasks.size}",
                    ctaEnabled = tasks.isNotEmpty(),
                    onCta = next,
                ) {
                    Title("What would you rather do?", "Pick up to ${Tasks.MAX_SELECTED} things that make your day better. Each one gives screen time back.")
                    ActivityPicker(tasks, onToggleTask)
                }

                Step.BUDGET -> StepPage(cta = "Continue", onCta = next) {
                    Title("How much feels okay?", "Be kind to yourself. You can tune it any time.")
                    BudgetPicker(cfg, onConfig)
                }

                Step.PERMISSIONS -> StepPage(
                    cta = if (perms.permissions.allGranted) "Continue" else "A few taps to go",
                    ctaEnabled = perms.permissions.allGranted,
                    onCta = next,
                ) {
                    var askSkip by rememberSaveable { mutableStateOf(false) }
                    if (askSkip) LimitedModeDialog(perms.permissions, onConfirm = { askSkip = false; next() }, onDismiss = { askSkip = false })
                    val first = blocked.firstOrNull()?.let(catalog::label) ?: "an app"
                    Title(
                        "Let ${avatar.displayName} notice",
                        "Two quick switches so ${avatar.displayName} can see when you open $first and give you a breath. Nothing leaves your phone.",
                    )
                    PermissionSteps(perms)
                    if (!perms.permissions.allGranted) LinkText("Continue without", { askSkip = true })
                }

                Step.DONE -> StepPage(cta = "Start my day", onCta = onFinish, centered = true) {
                    BobbingCharacter(avatar, Mood.OUTSIDE, 180.dp)
                    Spacer(Modifier.height(20.dp))
                    Text("You're all set!", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "${cfg.freeMinutes} free minutes today, up to ${Budget.earnableFrom(cfg, DayState(LocalDate.now()), tasks)} more to earn.",
                        style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = Brand.Muted,
                    )
                    Spacer(Modifier.height(16.dp))
                    WidgetButton(avatar, onAddWidget)
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
private fun HowRow(icon: String, title: String, body: String) = Row(
    Modifier.fillMaxWidth().surface(radius = 20.dp).padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    TendIcons.Badge(icon, 48.dp)
    Spacer(Modifier.width(14.dp))
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
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
fun CompanionPicker(avatar: Avatar, onAvatar: (Avatar) -> Unit) =
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Avatar.entries.forEach { a ->
            val selected = a == avatar
            Column(
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
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            catalog.suggested.forEach { AppTile(catalog, it, it.pkg in blocked, onToggle) }
        }
    } else {
        Text("None of the usual suspects are installed. Search below.", style = MaterialTheme.typography.bodyMedium)
    }
    // Picked from "other apps" earlier: keep them visible.
    val extra = catalog.others.filter { it.pkg in blocked }
    if (extra.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            extra.forEach { AppTile(catalog, it, true, onToggle) }
        }
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

@Composable
private fun AppTile(catalog: AppCatalog, app: InstalledApp, selected: Boolean, onToggle: (String) -> Unit) = Column(
    Modifier.width(96.dp).clip(RoundedCornerShape(20.dp))
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
    Text(app.label, style = MaterialTheme.typography.bodyMedium, color = Brand.Ink, maxLines = 1, textAlign = TextAlign.Center)
}

@Composable
private fun AppRow(catalog: AppCatalog, app: InstalledApp, selected: Boolean, onToggle: (String) -> Unit) = Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.Card).clickable { onToggle(app.pkg) }.padding(10.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    AppIcon(catalog, app.pkg, 32.dp)
    Spacer(Modifier.width(12.dp))
    Text(app.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    androidx.compose.material3.Icon(
        if (selected) androidx.compose.material.icons.Icons.Filled.Check else androidx.compose.material.icons.Icons.Filled.Add,
        null, tint = Brand.Moss,
    )
}

@Composable
private fun AppIcon(catalog: AppCatalog, pkg: String, size: Dp, modifier: Modifier = Modifier) {
    val icon = remember(pkg) { catalog.icon(pkg) }
    if (icon != null) Image(icon, null, modifier.size(size))
    else Box(modifier.size(size).clip(RoundedCornerShape(10.dp)).background(Brand.Mint))
}

@Composable
fun BudgetPicker(cfg: BudgetConfig, onConfig: (BudgetConfig) -> Unit) {
    Card {
        Text("FREE EVERY DAY", style = MaterialTheme.typography.labelSmall)
        Choices(listOf(15, 30, 45, 60), cfg.freeMinutes) { onConfig(cfg.copy(freeMinutes = it, totalMinutes = maxOf(cfg.totalMinutes, it))) }
        Text("No effort needed. Short and honest works best.", style = MaterialTheme.typography.bodyMedium)
    }
    Card {
        Text("MAX PER DAY, WITH EARNED MINUTES", style = MaterialTheme.typography.labelSmall)
        Choices(listOf(90, 120, 180, 240), cfg.totalMinutes) { onConfig(cfg.copy(totalMinutes = it, freeMinutes = minOf(cfg.freeMinutes, it))) }
        Text("The ceiling, even on your most active day.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Choices(options: List<Int>, value: Int, onPick: (Int) -> Unit) =
    Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val selected = o == value
            Text(
                "$o min",
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else Brand.Moss,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(if (selected) Brand.Moss else Brand.Sage)
                    .clickable { onPick(o) }.padding(vertical = 12.dp),
            )
        }
    }

@Composable
fun PermissionSteps(perms: PermissionActions) {
    val p = perms.permissions
    val unlock = perms.restricted && !p.allGranted
    if (unlock) UnlockCard(perms)
    val n = if (unlock) 1 else 0
    // Overlay first: once it's granted, tend may bring itself back to the front after the second switch.
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
}

/** Step 1 for side-loaded installs: lift Android's "restricted settings" lock before asking for anything. */
@Composable
private fun UnlockCard(perms: PermissionActions) = Column(
    Modifier.fillMaxWidth().surface(radius = 22.dp)
        .border(1.5.dp, Brand.Moss, RoundedCornerShape(22.dp)).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Brand.Sage), contentAlignment = Alignment.Center) {
            Text("1", color = Brand.Moss, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Unlock tend's settings", style = MaterialTheme.typography.titleMedium)
            Text("One time only. Android locks these switches for apps installed outside the Play Store.", style = MaterialTheme.typography.bodyMedium)
        }
    }
    Text("In App info: tap ⋮ in the top right corner → Allow restricted settings → confirm. Then come back.", style = MaterialTheme.typography.bodyLarge)
    PrimaryButton("Open app info", perms.openAppInfo)
    if (!perms.unlockTried) {
        // Some phones only add the menu item after Android has refused a switch once.
        LinkText("No \"Allow restricted settings\" in the menu? Tap here first, then open App info again.", perms.tryUnlock)
    } else {
        Text("Android said no? Good, the menu item should be there now.", style = MaterialTheme.typography.bodyMedium)
    }
    LinkText("Already unlocked? Skip", perms.skipUnlock)
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
        if (!blocked) Text("Switch tend on. We'll bring you right back.", style = MaterialTheme.typography.bodyMedium)
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
 * Asks the launcher to pin the widget. Launchers differ (some ignore the request, some need a
 * second tap), so after the tap we always show the manual way too.
 */
@Composable
fun WidgetButton(avatar: Avatar, onAddWidget: () -> Boolean) {
    var asked by rememberSaveable { mutableStateOf<Boolean?>(null) }
    SecondaryButton("Put ${avatar.displayName} on your home screen", { asked = onAddWidget() })
    asked?.let { accepted ->
        Text(
            (if (accepted) "Confirm in the pop-up. Nothing showed up? " else "Your launcher wants you to add it yourself: ") +
                "long-press an empty spot on your home screen → Widgets → tend.",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = Space.s),
        )
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
                "Without usage access, tend can't see screen time or app opens. Activities, the reading timer and the garden still work.",
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
