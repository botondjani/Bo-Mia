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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.horizontalScroll
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
import kotlinx.coroutines.launch

/** The camp's shape, the same on the home page and here, so moving between them is seamless. */
const val CAMP_RATIO = 1.8f

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
    onBuy: (thing: String) -> Boolean,
    onSell: (thing: String) -> Unit,
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
    /** Where the companion stays (the spot of the last thing used), and to save a new one. */
    rest: Int? = null,
    onRest: (Int?) -> Unit = {},
    mood: app.tend.core.Mood = app.tend.core.Mood.HAPPY,
    openInvite: Boolean = false,
    onStep: (app.tend.core.Step) -> Unit = {},
    visitor: app.tend.core.Visitor? = null,
    guestbook: Map<app.tend.core.Visitor, Int> = emptyMap(),
    onGreet: () -> app.tend.core.Visitor? = { null },
    ducks: List<app.tend.data.Friends.Note> = emptyList(),
    onUnroll: () -> Unit = {},
    onCatchDuck: (app.tend.data.Friends.Note) -> Int = { 0 },
) {
    val scroll = rememberScrollState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Column(
    Modifier.fillMaxSize().background(Brand.Sage).verticalScroll(scroll)
        .padding(horizontal = 20.dp).padding(top = Space.xs, bottom = Space.xxl),
    verticalArrangement = Arrangement.spacedBy(Space.m),
) {
    var tab by rememberSaveable { mutableIntStateOf(if (openInvite) 1 else 0) }
    androidx.compose.runtime.LaunchedEffect(openInvite) { if (openInvite) tab = 1 }

    // The same height as the home page's header.
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButtonSoft(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
        Spacer(Modifier.width(Space.m))
        Column(Modifier.weight(1f)) {
            Text("Tap anything to use it", style = MaterialTheme.typography.bodyMedium)
            Text("${avatar.displayName}'s camp", style = MaterialTheme.typography.headlineMedium, maxLines = 1)
        }
        IconButtonSoft(androidx.compose.material.icons.Icons.Filled.Share, "Share", onShare)
    }

    // What friends did: toilet paper unrolls when tapped; a visitor's duck is fished out.
    val paperLeft by androidx.compose.animation.core.animateFloatAsState(if (paper) 1f else 0f, androidx.compose.animation.core.tween(1400), label = "paper")
    val ducksNow by androidx.compose.runtime.rememberUpdatedState(ducks)
    val paperNow by androidx.compose.runtime.rememberUpdatedState(paper)
    val bridgeNow by androidx.compose.runtime.rememberUpdatedState(bridge)
    var caught by remember { mutableStateOf<String?>(null) }
    // An animal just greeted: a little card pops up on the camp for a moment.
    var greeted by remember { mutableStateOf<app.tend.core.Visitor?>(null) }
    androidx.compose.runtime.LaunchedEffect(greeted) { if (greeted != null) { kotlinx.coroutines.delay(2800); greeted = null } }
    // The companion using a thing: which spot and thing, and the frame clock that moves it.
    var using by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var walk by remember { mutableStateOf(0f) }
    val restNow by androidx.compose.runtime.rememberUpdatedState(rest)
    var useStart by remember { androidx.compose.runtime.mutableLongStateOf(-1L) }
    var frame by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var strumAt by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(using) {
        val u = using ?: return@LaunchedEffect
        useStart = -1L
        while (true) {
            androidx.compose.runtime.withFrameNanos { frame = it; if (useStart < 0) { useStart = it; strumAt = it } }
            // Done: they stay there.
            if ((frame - useStart) / 1e9f > walk + Uses.seconds(u.second)) { using = null; onRest(u.first); break }
        }
    }
    val placedNow by androidx.compose.runtime.rememberUpdatedState(placed)
    val nightClock = rememberNightClock(night)
    val appear = rememberAppear(placed)
    // Once in bed, they stay there: next time it doesn't walk again.
    androidx.compose.runtime.LaunchedEffect(nightClock > Uses.WALK + 2f) {
        val bed = bedOf(placed)
        if (night && nightClock > Uses.WALK + 2f && bed != null && rest != bed) onRest(bed)
    }
    val visitorNow by androidx.compose.runtime.rememberUpdatedState(visitor)
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { Sounds.stop() } }

    // The camp, big and edge to edge: wider than the screen, so it pans left and right.
    androidx.compose.foundation.layout.BoxWithConstraints(
        Modifier.fillMaxWidth().layout { m, c ->
            val extra = 20.dp.roundToPx()
            val p = m.measure(c.copy(minWidth = c.maxWidth + 2 * extra, maxWidth = c.maxWidth + 2 * extra))
            layout(c.maxWidth, p.height) { p.place(-extra, 0) }
        },
    ) {
        val sceneHeight = maxWidth * 1.0f
        val pan = rememberScrollState()
        // Starts in the middle of the camp.
        var centered by rememberSaveable { mutableStateOf(false) }
        androidx.compose.runtime.LaunchedEffect(pan.maxValue) { if (!centered && pan.maxValue > 0) { pan.scrollTo(pan.maxValue / 2); centered = true } }
        Box(Modifier.fillMaxWidth().height(sceneHeight).campEntrance().horizontalScroll(pan)) {
            Canvas(
                Modifier.width(sceneHeight * CAMP_RATIO).height(sceneHeight).pointerInput(Unit) {
                    detectTapGestures { tap ->
                        val fx = tap.x / size.width
                        val fy = tap.y / size.height
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
                                onGreet()?.let { greeted = it; caught = null; Sounds.chime(context) }
                                return@detectTapGestures
                            }
                        }
                        if (paperNow && fx > PAPER_LEFT && fy < PAPER_BOTTOM) { onUnroll(); caught = "All clean again."; return@detectTapGestures }
                        // The bridge leads to friends' camps along the river.
                        if (bridgeNow && fx in 0.06f..0.37f && fy in 0.48f..0.62f) { onOpenRiver(null); return@detectTapGestures } // the bridge or its signpost
                        // A thing in the camp: the companion uses it (the guitar: another strum).
                        val hit = spotAt(Offset(fx, fy), size.width / size.height.toFloat())
                        val thing = hit?.let { placedNow[it] }
                        if (thing != null) {
                            if (using?.second == thing && thing == "guitar") { strumAt = frame; useStart = frame - ((walk + 1f) * 1e9f).toLong(); Sounds.strum(context) }
                            else {
                                // Walk over first, unless already there; the sound starts on arrival.
                                using?.let { onRest(it.first) } // switching mid-use: from where they are now
                                walk = if ((using?.first ?: restNow) == hit) 0f else Uses.WALK
                                using = hit to thing; caught = null; Sounds.forUse(context, thing, (walk * 1000).toLong()); onUsed()
                            }
                        } else if (using != null) { using = null; Sounds.stop() }
                    }
                },
            ) {
                val use = using?.takeIf { useStart >= 0 }?.let { (spot, thing) -> InUse(spot, thing, (frame - useStart) / 1e9f, walk) }
                drawClearing(
                    avatar, placed, mood, worn = worn, night = night, paper = paperLeft, ducks = ducks.size, bridge = bridge,
                    use = use, lastStrum = (frame - strumAt) / 1e9f - walk, visitor = visitor, rest = rest, nightClock = nightClock, appear = appear,
                )
            }
        }
        AcornBadge(acorns, Modifier.align(Alignment.TopEnd).padding(12.dp))
        val shown = remember { mutableStateOf<app.tend.core.Visitor?>(null) }
        if (greeted != null) shown.value = greeted
        androidx.compose.animation.AnimatedVisibility(
            greeted != null, Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.7f),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { -it },
        ) { shown.value?.let { GreetedCard(it, guestbook.size) } }
    }
    Text(
        when {
            using != null -> Uses.line(using!!.second, avatar.displayName)
            paper -> "Someone toilet-papered your oak! Tap it to unroll."
            visitor != null -> "A ${visitor.title.lowercase()} came to the camp. Tap it to say hello."
            ducks.isNotEmpty() -> "A rubber duck: someone visited. Tap it to fish it out."
            caught != null -> caught!!
            bridge -> "Cross the bridge to visit your friends' camps."
            placed.isNotEmpty() -> "Tap something in the camp: ${avatar.displayName} will use it. Swipe to look around."
            else -> "Earn acorns by living; spend them in the shop below."
        },
        style = MaterialTheme.typography.bodyMedium,
    )

    FirstStepsCard(steps, onClaim = { onClaimStep(it); Sounds.chime(context) }, onGo = { step ->
        when (step) {
            app.tend.core.Step.PLACE -> tab = 0
            app.tend.core.Step.USE -> scope.launch { scroll.animateScrollTo(0) }
            app.tend.core.Step.FRIEND -> tab = 1
            else -> onStep(step)
        }
    })

    // One thing at a time below the camp: the shop, friends, or what's been collected.
    Tabs(listOf("Shop", "Friends", "Collection"), tab) { tab = it }
    when (tab) {
        0 -> Shop(acorns, placed, earnedToday.values.sum() + lastNight, avatar.displayName, onBuy, onSell)
        1 -> FriendsCard(onOpenSettings, onOpenRiver, onFriendsChanged, openInvite)
        else -> {
            Guestbook(guestbook)
            Things(avatar, worn, onShelf)
        }
    }
}
}

/** A row of tabs as one rounded control; the picked one is filled. */
@Composable
private fun Tabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) = Row(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.Card).padding(4.dp),
) {
    labels.forEachIndexed { i, label ->
        Text(
            label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,
            color = if (i == selected) Color.White else Brand.Moss,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (i == selected) Brand.Moss else Color.Transparent)
                .tappable { onSelect(i) }.padding(vertical = 10.dp),
        )
    }
}

/**
 * The shop: every thing, three to a row, grouped by where it goes, each bought once. Tapping one
 * shows what it's for and its button: get it (it goes to its place by itself, swapping out the
 * cheaper thing there for half back), or sell it.
 */
@Composable
private fun Shop(acorns: Int, placed: Map<Int, String>, earnedToday: Int, name: String, onBuy: (String) -> Boolean, onSell: (String) -> Unit) {
    var open by remember { mutableStateOf<app.tend.core.Thing?>(null) }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("SHOP", style = MaterialTheme.typography.labelSmall)
                Text(
                    if (earnedToday > 0) "+$earnedToday acorns today" else "Activities earn 10, a \"Not now\" 2.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted,
                )
            }
            AcornBadge(acorns)
        }
        listOf(SpotKind.SMALL, SpotKind.FIRE, SpotKind.SEAT, SpotKind.SHELTER, SpotKind.TREES, SpotKind.WATER, SpotKind.ABOVE).forEach { kind ->
            Text(kindTitle(kind).uppercase(), style = MaterialTheme.typography.labelSmall, color = Brand.Muted, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
            Things.forKind(kind).chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    row.forEach { thing -> ShopTile(thing, owned = thing.id in placed.values, affordable = acorns >= thing.price, Modifier.weight(1f)) { open = thing } }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
    open?.let { t ->
        val owned = t.id in placed.values
        val swapped = if (owned) null else Clearing.spotFor(placed, t)?.let { placed[it] }?.let(Things::byId)
        val cost = t.price - (swapped?.let(Clearing::sellValue) ?: 0)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { open = null },
            icon = { Canvas(Modifier.size(84.dp)) { drawThingIcon(t.id) } },
            title = { Text(t.name) },
            text = {
                Text(
                    Uses.line(t.id, name).substringBefore(". ").removeSuffix(".") + "." +
                        (swapped?.let { " It takes the ${it.name.lowercase()}'s place; that one goes back for ${Clearing.sellValue(it)} acorns." } ?: "") +
                        if (owned) " Selling gives ${Clearing.sellValue(t)} acorns back." else "",
                )
            },
            confirmButton = {
                when {
                    owned -> androidx.compose.material3.TextButton(onClick = { onSell(t.id); open = null }) { Text("Sell for ${Clearing.sellValue(t)}", color = Brand.Coral) }
                    acorns >= cost -> androidx.compose.material3.TextButton(onClick = { if (onBuy(t.id)) open = null }) { Text("Get it for ${t.price}", color = Brand.Moss) }
                    else -> androidx.compose.material3.TextButton(onClick = { open = null }) { Text("${cost - acorns} more acorns to go", color = Brand.Muted) }
                }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { open = null }) { Text("Close") } },
            containerColor = Brand.Card,
        )
    }
}

/** One thing in the shop: its picture, name and price (a tick when it's in the camp). */
@Composable
internal fun ShopTile(thing: app.tend.core.Thing, owned: Boolean, affordable: Boolean, modifier: Modifier, background: Color = Brand.Sage, onTap: () -> Unit) = Column(
    modifier.clip(RoundedCornerShape(16.dp)).background(if (owned) Brand.Picked else background).tappable(onClick = onTap).padding(vertical = 8.dp, horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(48.dp)) { drawThingIcon(thing.id) }
    }
    Text(thing.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        if (owned) Text("In camp", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = Brand.Moss)
        else {
            Text("${thing.price}", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = if (affordable) Brand.Moss else Brand.Muted)
            Spacer(Modifier.width(3.dp))
            AcornIcon(12.dp)
        }
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

private fun kindTitle(kind: SpotKind) = when (kind) {
    SpotKind.FIRE -> "The fire pit"
    SpotKind.SEAT -> "By the fire"
    SpotKind.SHELTER -> "A place to sleep"
    SpotKind.TREES -> "In the trees"
    SpotKind.WATER -> "On the river"
    SpotKind.ABOVE -> "Between the trees"
    SpotKind.SMALL -> "Around the camp"
}

/** "A deer!": the animal, how rare it is, its acorns, and how full the guestbook is now. */
@Composable
private fun GreetedCard(v: app.tend.core.Visitor, inBook: Int) = Row(
    Modifier.clip(RoundedCornerShape(20.dp)).background(Brand.Card).padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Brand.Sage)) { Canvas(Modifier.fillMaxSize().padding(4.dp)) { drawVisitorIcon(v) } }
    Spacer(Modifier.width(10.dp))
    Column {
        Text("A ${v.title.lowercase()}!", style = MaterialTheme.typography.titleSmall)
        Text("${v.rarity.label} · guestbook $inBook/${app.tend.core.Visitor.entries.size}", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
    }
    Spacer(Modifier.width(10.dp))
    Text("+${v.rarity.acorns}", style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
    Spacer(Modifier.width(3.dp))
    AcornIcon(16.dp)
}

/** The animals that came to the camp: seen ones in colour with how often, the rest as shadows, four to a row. */
@Composable
private fun Guestbook(book: Map<app.tend.core.Visitor, Int>) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("GUESTBOOK", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text("${book.size} / ${app.tend.core.Visitor.entries.size}", style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
        }
        app.tend.core.Visitor.entries.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                row.forEach { v ->
                    val n = book[v] ?: 0
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp)).background(Brand.Sage), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.fillMaxSize().padding(6.dp)) { drawVisitorIcon(v, seen = n > 0) }
                        }
                        Text(if (n > 0) v.title + if (n > 1) " ×$n" else "" else "?", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        Text(v.rarity.label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp), color = if (v.rare) Color(0xFFB9783F) else Brand.Muted)
                    }
                }
            }
        }
        Text("Someone comes most days; tap them to say hello. Rare ones only after a good day.", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
    }
}
