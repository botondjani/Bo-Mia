package app.tend.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tend.core.Accessory
import app.tend.core.Avatar
import app.tend.core.Clearing
import app.tend.core.SpotKind
import app.tend.core.Things

/** The scene's shape on screen: wide, like a window onto the camp. */
private const val SCENE_RATIO = 1.6f

/**
 * The clearing page: the camp, acorns to spend, what today earned and what to save for. Tap
 * "Place things" (or the scene) to see the spots; tap a spot to buy, swap or sell.
 */
@Composable
fun ClearingScreen(
    avatar: Avatar,
    placed: Map<Int, String>,
    acorns: Int,
    earnedToday: Map<String, Int>,
    lastNight: Int,
    worn: Set<Accessory>,
    onShelf: Set<Accessory>,
    night: Boolean,
    onBuy: (spot: Int, thing: String) -> Boolean,
    onSell: (spot: Int) -> Unit,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenRiver: (friendId: String?) -> Unit = {},
    paper: Boolean = false,
    bridge: Boolean = false,
    onFriendsChanged: () -> Unit = {},
    steps: List<app.tend.core.StepState> = emptyList(),
    onClaimStep: (app.tend.core.Step) -> Unit = {},
    onUsed: () -> Unit = {},
    visitor: app.tend.core.Visitor? = null,
    guestbook: Map<app.tend.core.Visitor, Int> = emptyMap(),
    onGreet: () -> app.tend.core.Visitor? = { null },
    ducks: List<app.tend.data.Friends.Note> = emptyList(),
    onUnroll: () -> Unit = {},
    onCatchDuck: (app.tend.data.Friends.Note) -> Int = { 0 },
    week: @Composable ColumnScope.() -> Unit,
) = Column(
    Modifier.fillMaxSize().background(Brand.Sage).verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp).padding(top = Space.s, bottom = Space.xxl),
    verticalArrangement = Arrangement.spacedBy(14.dp),
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButtonSoft(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
        Spacer(Modifier.width(Space.m))
        Text("${avatar.displayName}'s clearing", style = MaterialTheme.typography.headlineMedium, maxLines = 1, modifier = Modifier.weight(1f))
        IconButtonSoft(androidx.compose.material.icons.Icons.Filled.Share, "Share", onShare)
    }

    // What friends did: toilet paper unrolls when tapped; a visitor's duck is fished out.
    val paperLeft by androidx.compose.animation.core.animateFloatAsState(if (paper) 1f else 0f, androidx.compose.animation.core.tween(1400), label = "paper")
    val ducksNow by androidx.compose.runtime.rememberUpdatedState(ducks)
    val paperNow by androidx.compose.runtime.rememberUpdatedState(paper)
    val bridgeNow by androidx.compose.runtime.rememberUpdatedState(bridge)
    var caught by remember { mutableStateOf<String?>(null) }
    // The companion using a thing: which spot and thing, and the frame clock that moves it.
    var using by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var useStart by remember { androidx.compose.runtime.mutableLongStateOf(-1L) }
    var frame by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var strumAt by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(using) {
        val u = using ?: return@LaunchedEffect
        useStart = -1L
        while (true) {
            androidx.compose.runtime.withFrameNanos { frame = it; if (useStart < 0) { useStart = it; strumAt = it } }
            if ((frame - useStart) / 1e9f > Uses.seconds(u.second)) { using = null; break }
        }
    }
    val placedNow by androidx.compose.runtime.rememberUpdatedState(placed)
    val visitorNow by androidx.compose.runtime.rememberUpdatedState(visitor)
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { Sounds.stop() } }

    // The camp. Tapping it shows the spots; tapping a spot opens the shop for it.
    Box(Modifier.fillMaxWidth().aspectRatio(SCENE_RATIO).clip(RoundedCornerShape(26.dp))) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(editing) {
                detectTapGestures { tap ->
                    val fx = tap.x / size.width
                    val fy = tap.y / size.height
                    if (!editing) {
                        val duck = DuckSpots.take(ducksNow.size).indexOfFirst { kotlin.math.abs(it.x - fx) * size.width < size.height * 0.08f && kotlin.math.abs(it.y - fy) < 0.09f }
                        if (duck >= 0) {
                            val n = ducksNow[duck]
                            val total = onCatchDuck(n)
                            caught = "${n.name}'s duck from their visit. You have $total."
                            Sounds.chime(context)
                            return@detectTapGestures
                        }
                        visitorNow?.let { v ->
                            if (kotlin.math.abs(v.x - fx) * size.width < size.height * 0.08f && fy in (v.y - 0.16f)..(v.y + 0.03f)) {
                                onGreet()?.let { caught = "A ${it.title.lowercase()} came by! It's in your guestbook. +${app.tend.core.Visitors.ACORNS} acorns."; Sounds.chime(context) }
                                return@detectTapGestures
                            }
                        }
                        if (paperNow && fx > PAPER_LEFT && fy < PAPER_BOTTOM) { onUnroll(); caught = "All clean again."; return@detectTapGestures }
                        // The bridge leads to friends' camps along the river.
                        if (bridgeNow && fx in 0.1f..0.37f && fy in 0.5f..0.64f) { onOpenRiver(null); return@detectTapGestures }
                        // A thing in the camp: the companion uses it (the guitar: another strum).
                        val hit = spotAt(Offset(fx, fy), size.width / size.height.toFloat())
                        val thing = hit?.let { placedNow[it] }
                        if (thing != null) {
                            if (using?.second == thing && thing == "guitar") { strumAt = frame; useStart = frame - 1_000_000_000L; Sounds.strum(context) }
                            else { using = hit to thing; caught = null; Sounds.forUse(context, thing); onUsed() }
                            return@detectTapGestures
                        }
                        if (using != null) { using = null; Sounds.stop(); return@detectTapGestures }
                    }
                    val spot = spotAt(Offset(tap.x / size.width, tap.y / size.height), size.width / size.height.toFloat())
                    if (!editing) editing = true
                    selected = spot
                }
            },
        ) {
            val use = using?.takeIf { !editing && useStart >= 0 }?.let { (spot, thing) -> InUse(spot, thing, (frame - useStart) / 1e9f) }
            drawClearing(
                avatar, placed, worn = worn, night = night, showSpots = editing, selected = selected, paper = paperLeft, ducks = ducks.size, bridge = bridge,
                use = use, lastStrum = (frame - strumAt) / 1e9f, visitor = visitor,
            )
        }
        AcornBadge(acorns, Modifier.align(Alignment.TopEnd).padding(10.dp))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            when {
                editing -> "Tap a spot to put something there."
                using != null -> Uses.line(using!!.second, avatar.displayName)
                paper -> "Someone toilet-papered your oak! Tap it to unroll."
                visitor != null -> "A ${visitor.title.lowercase()} came to the camp. Tap it to say hello."
                ducks.isNotEmpty() -> "A rubber duck: someone visited. Tap it to fish it out."
                caught != null -> caught!!
                bridge -> "Cross the bridge to visit your friends' camps."
                placed.isNotEmpty() -> "Tap something in the camp: ${avatar.displayName} will use it."
                else -> "Earn acorns by living; spend them here."
            },
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
        )
        ChipButton(if (editing) "Done" else "Place things", { editing = !editing; selected = null; using = null; Sounds.stop() }, filled = !editing)
    }

    FirstStepsCard(steps) { onClaimStep(it); Sounds.chime(context) }
    Guestbook(guestbook)
    NextThing(acorns, placed)
    EarnedToday(earnedToday, lastNight)
    FriendsCard(onOpenSettings, onOpenRiver, onFriendsChanged)
    Things(avatar, worn, onShelf)
    week()

    selected?.let { id ->
        SpotSheet(id, placed[id], acorns, onDismiss = { selected = null }, onBuy = { thing -> if (onBuy(id, thing)) selected = null }, onSell = { onSell(id); selected = null })
    }
}

/** The spot nearest to a tap (in scene fractions), if it's close enough. */
private fun spotAt(tap: Offset, ratio: Float): Int? = SpotPositions.entries
    .map { (id, p) ->
        // Things stand on their spot, so a tap a little above it counts too.
        val dx = (tap.x - p.x) * ratio
        val dy = (tap.y - (p.y - 0.04f))
        id to kotlin.math.sqrt(dx * dx + dy * dy)
    }
    .minByOrNull { it.second }
    ?.takeIf { it.second < 0.12f }?.first

/** Acorns, as a small pill. */
@Composable
fun AcornBadge(acorns: Int, modifier: Modifier = Modifier) = Row(
    modifier.clip(RoundedCornerShape(50)).background(Color(0xE6FFFFFF)).padding(horizontal = 10.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    AcornIcon(18.dp)
    Spacer(Modifier.width(5.dp))
    Text("$acorns", style = MaterialTheme.typography.titleMedium, color = Color(0xFF3D2A1C))
}

/** An acorn: a warm brown nut under a darker cap with a stalk. */
@Composable
fun AcornIcon(size: Dp) = Canvas(Modifier.size(size)) {
    val s = this.size.minDimension
    drawOval(Color(0xFFB9783F), Offset(s * 0.2f, s * 0.32f), Size(s * 0.6f, s * 0.62f))
    drawOval(Color(0x33FFFFFF), Offset(s * 0.3f, s * 0.45f), Size(s * 0.14f, s * 0.26f))
    drawArc(Color(0xFF6B4A2B), 180f, 180f, true, Offset(s * 0.14f, s * 0.2f), Size(s * 0.72f, s * 0.34f))
    drawLine(Color(0xFF6B4A2B), Offset(s * 0.5f, s * 0.22f), Offset(s * 0.58f, s * 0.06f), strokeWidth = s * 0.09f)
}

/** What to save for: the cheapest thing not yet in the clearing that's out of reach, and how far. */
@Composable
private fun NextThing(acorns: Int, placed: Map<Int, String>) {
    val owned = placed.values.toSet()
    val next = Things.all.filter { it.id !in owned && it.price > acorns }.minByOrNull { it.price } ?: return
    val canToday = Things.all.filter { it.id !in owned && it.price <= acorns }.maxByOrNull { it.price }
    Row(Modifier.fillMaxWidth().surface(radius = 22.dp).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Brand.Sage), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(48.dp)) { drawThingIcon(next.id) }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("NEXT UP", style = MaterialTheme.typography.labelSmall)
            Text(next.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${next.price - acorns} more acorns" + (canToday?.let { " · today you could get the ${it.name.lowercase()}" } ?: ""),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted,
            )
        }
    }
}

/** Where today's acorns came from. */
@Composable
private fun EarnedToday(earned: Map<String, Int>, lastNight: Int) {
    val parts = listOfNotNull(
        earned["activities"]?.takeIf { it > 0 }?.let { "Activities" to it },
        earned["not_now"]?.takeIf { it > 0 }?.let { "\"Not now\"" to it },
        earned["challenge"]?.takeIf { it > 0 }?.let { "Weekly challenge" to it },
        earned["level"]?.takeIf { it > 0 }?.let { "Level up" to it },
        earned["together"]?.takeIf { it > 0 }?.let { "Together with friends" to it },
        earned["steps"]?.takeIf { it > 0 }?.let { "First steps" to it },
        earned["visitor"]?.takeIf { it > 0 }?.let { "A visitor" to it },
        lastNight.takeIf { it > 0 }?.let { "Last night" to it },
    )
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ACORNS TODAY", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text("+${parts.sumOf { it.second }}", style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
        }
        if (parts.isEmpty()) Text(
            "An activity is 10, a \"Not now\" is 2. Staying within your minutes and below your old normal adds more at midnight.",
            style = MaterialTheme.typography.bodyMedium,
        )
        parts.forEach { (label, n) ->
            Row(Modifier.padding(top = 4.dp)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("+$n", style = MaterialTheme.typography.bodyMedium, color = Brand.Moss)
            }
        }
    }
}

private fun kindTitle(kind: SpotKind) = when (kind) {
    SpotKind.FIRE -> "The fire pit"
    SpotKind.SEAT -> "By the fire"
    SpotKind.SHELTER -> "A place to sleep"
    SpotKind.TREES -> "In the trees"
    SpotKind.WATER -> "On the river"
    SpotKind.ABOVE -> "Between the trees"
    SpotKind.SMALL -> "Around the camp"
}

/** The shop for one spot: what's there now (sell it), and what could go there. */
@Composable
private fun SpotSheet(spotId: Int, current: String?, acorns: Int, onDismiss: () -> Unit, onBuy: (String) -> Unit, onSell: () -> Unit) =
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) { SpotSheetCard(spotId, current, acorns, onDismiss, onBuy, onSell) }

@Composable
internal fun SpotSheetCard(spotId: Int, current: String?, acorns: Int, onDismiss: () -> Unit, onBuy: (String) -> Unit, onSell: () -> Unit) {
    val spot = Clearing.spot(spotId) ?: return
    val refund = current?.let(Things::byId)?.let(Clearing::sellValue) ?: 0
    run {
        Column(
            Modifier.fillMaxWidth().surface(radius = 28.dp).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(kindTitle(spot.kind), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                AcornBadge(acorns)
            }
            Things.forKind(spot.kind).forEach { thing ->
                val here = thing.id == current
                val cost = thing.price - refund
                val affordable = acorns >= cost
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(if (here) Brand.Picked else Brand.Sage)
                        .border(if (here) 2.dp else 0.dp, if (here) Brand.Moss else Color.Transparent, RoundedCornerShape(18.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x66FFFFFF)), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(46.dp)) { drawThingIcon(thing.id) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(thing.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AcornIcon(13.dp)
                            Spacer(Modifier.width(3.dp))
                            Text("${thing.price}", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
                        }
                    }
                    when {
                        here -> Text("Sell · +$refund", style = MaterialTheme.typography.labelLarge, color = Brand.Moss, textAlign = TextAlign.Center,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White).tappable(onClick = onSell).padding(horizontal = 12.dp, vertical = 7.dp))
                        affordable -> Text("Get", style = MaterialTheme.typography.labelLarge, color = Color.White,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(Brand.Moss).tappable { onBuy(thing.id) }.padding(horizontal = 16.dp, vertical = 7.dp))
                        else -> Text("${cost - acorns} more", style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = Brand.Muted)
                    }
                }
            }
            if (current != null) Text("Swapping sells what's there for half its price.", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
            SecondaryButton("Close", onDismiss)
        }
    }
}

/** The animals that came to the camp: seen ones in colour with how often, the rest as shadows. */
@Composable
private fun Guestbook(book: Map<app.tend.core.Visitor, Int>) {
    if (book.isEmpty()) return
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("GUESTBOOK", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text("${book.size} / ${app.tend.core.Visitor.entries.size}", style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            app.tend.core.Visitor.entries.forEach { v ->
                val n = book[v] ?: 0
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Brand.Sage), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize().padding(4.dp)) { drawVisitorIcon(v, seen = n > 0) }
                    }
                    Text(if (n > 0) "${v.title}${if (n > 1) " ×$n" else ""}" else if (v.rare) "Rare" else "?", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = Brand.Muted, maxLines = 1)
                }
            }
        }
        Text("Someone new comes most days. After a good day, a rare one might.", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
    }
}
