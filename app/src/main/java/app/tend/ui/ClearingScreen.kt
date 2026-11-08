package app.tend.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.compose.ui.graphics.asImageBitmap
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
    onUsed: (thing: String) -> Unit = {},
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
    /** Where the user put things (spot to scene fraction). */
    layout: Map<Int, Offset> = emptyMap(),
    /** Puts the thing at a spot somewhere else on the meadow. */
    onPlace: (spot: Int, at: Offset) -> Unit = { _, _ -> },
    /** Plus is on: the season's own things can be bought; otherwise a tap on one shows Plus. */
    plus: Boolean = true,
    onPlus: () -> Unit = {},
    /** The seasons' outfits bought, the one worn, and buying or putting one on (null: off). */
    outfits: Set<app.tend.core.Accessory> = emptySet(),
    outfitWorn: app.tend.core.Accessory? = null,
    onBuyOutfit: (app.tend.core.Accessory) -> Boolean = { false },
    onWearOutfit: (app.tend.core.Accessory?) -> Unit = {},
    /** Friends phone-free right now, at the fire (companion, name). */
    guests: List<Pair<Avatar, String>> = emptyList(),
    /** Friends who cheered you since the camp last showed it; shown once, then [onCheersShown]. */
    cheers: List<String> = emptyList(),
    onCheersShown: () -> Unit = {},
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
            Text("Tap to use, hold to move", style = MaterialTheme.typography.bodyMedium)
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
    // Moving things: hold one to pick it up, drag it anywhere on the meadow, let go to put it down.
    // Shown at once where it was put (the saved layout catches up right after).
    var local by remember { mutableStateOf(layout) }
    androidx.compose.runtime.LaunchedEffect(layout) { local = layout }
    val localNow by androidx.compose.runtime.rememberUpdatedState(local)
    var drag by remember { mutableStateOf<Pair<Int, Offset>?>(null) } // spot, where (scene fractions)
    var settling by remember { mutableStateOf<Int?>(null) } // just put down, still springing
    val lift = remember { androidx.compose.animation.core.Animatable(0f) }
    var dustAt by remember { mutableStateOf<Int?>(null) }
    val dust = remember { androidx.compose.animation.core.Animatable(1f) }
    // The companion's place before the move, and a clock for walking over to the new one.
    var restFrom by remember { mutableStateOf<Map<Int, Offset>?>(null) }
    val restClock = remember { androidx.compose.animation.core.Animatable(100f) }
    var fingerInView by remember { mutableStateOf(-1f) } // px from the left of what's on screen, while dragging
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
    // A light tick under the finger when a tap does something; a firmer one for a purchase.
    val view = androidx.compose.ui.platform.LocalView.current
    fun tick(firm: Boolean = false) = view.performHapticFeedback(
        if (firm && android.os.Build.VERSION.SDK_INT >= 30) android.view.HapticFeedbackConstants.CONFIRM else android.view.HapticFeedbackConstants.KEYBOARD_TAP,
    )
    // Sparkles where an animal was greeted or a duck caught, as they vanish.
    val bursts = remember { androidx.compose.runtime.mutableStateListOf<Pair<Offset, androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D>>>() }
    fun burst(at: Offset) {
        val a = androidx.compose.animation.core.Animatable(0f)
        bursts += at to a
        scope.launch { a.animateTo(1f, androidx.compose.animation.core.tween(800)); bursts.removeAll { it.second === a } }
    }
    // The camp pans left and right; after buying something it slides to show it popping up.
    val pan = rememberScrollState()
    var sceneWidthPx by remember { mutableIntStateOf(0) }
    fun showSpot(spot: Int) = scope.launch {
        scroll.animateScrollTo(0)
        val x = (if (spot in SpotPositions) spotPos(spot, localNow).x else 0.5f) * sceneWidthPx
        val viewport = sceneWidthPx - pan.maxValue
        pan.animateScrollTo((x - viewport / 2f).toInt().coerceIn(0, pan.maxValue))
    }
    // Once in bed, they stay there: next time it doesn't walk again.
    androidx.compose.runtime.LaunchedEffect(nightClock > Uses.WALK + 2f) {
        val bed = bedOf(placed)
        if (night && nightClock > Uses.WALK + 2f && bed != null && rest != bed) onRest(bed)
    }
    val visitorNow by androidx.compose.runtime.rememberUpdatedState(visitor)
    // Cheered by friends: a card at the top, and the companion beaming (hearts) for a few seconds.
    var cheerCard by remember { mutableStateOf<String?>(null) }
    var cheerT by remember { mutableStateOf(-1f) }
    androidx.compose.runtime.LaunchedEffect(cheers) {
        if (cheers.isEmpty()) return@LaunchedEffect
        kotlinx.coroutines.delay(600)
        cheerCard = if (cheers.size == 1) "${cheers[0]} cheered you on" else "${cheers.dropLast(1).joinToString(", ")} and ${cheers.last()} cheered you on"
        tick(firm = true); Sounds.chime(context)
        onCheersShown()
        // Hearts from the companion while the card shows; the card itself stays a few seconds.
        val hearts = launch {
            val start = androidx.compose.runtime.withFrameNanos { it }
            while (true) cheerT = (androidx.compose.runtime.withFrameNanos { it } - start) / 1e9f
        }
        kotlinx.coroutines.delay(4_000)
        hearts.cancel()
        cheerT = -1f
        cheerCard = null
    }
    // Camp life: clouds and weather move, and the companion gets on with things when left alone.
    val ambient = rememberAmbientClock()
    val idle = rememberIdleLife(placed, rest, enabled = using == null && drag == null && settling == null && greeted == null && !night) { onRest(it) }
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
        // Starts in the middle of the camp.
        var centered by rememberSaveable { mutableStateOf(false) }
        androidx.compose.runtime.LaunchedEffect(pan.maxValue) { if (!centered && pan.maxValue > 0) { pan.scrollTo(pan.maxValue / 2); centered = true } }
        Box(Modifier.fillMaxWidth().height(sceneHeight).campEntrance().horizontalScroll(pan)) {
            Canvas(
                Modifier.width(sceneHeight * CAMP_RATIO).height(sceneHeight).onSizeChanged { sceneWidthPx = it.width }.pointerInput(Unit) {
                    // Long presses belong to moving things: a tap only counts when it's quick.
                    detectTapGestures(onLongPress = {}) { tap ->
                        val fx = tap.x / size.width
                        val fy = tap.y / size.height
                        val duck = DuckSpots.take(ducksNow.size).indexOfFirst { kotlin.math.abs(it.x - fx) * size.width < size.height * 0.08f && kotlin.math.abs(it.y - fy) < 0.09f }
                        if (duck >= 0) {
                            val n = ducksNow[duck]
                            val total = onCatchDuck(n)
                            caught = "${n.name}'s duck from their visit. You have $total."
                            burst(DuckSpots[duck]); tick()
                            Sounds.chime(context)
                            return@detectTapGestures
                        }
                        visitorNow?.let { v ->
                            if (kotlin.math.abs(v.x - fx) * size.width < size.height * 0.08f && fy in (v.y - 0.16f)..(v.y + 0.03f)) {
                                onGreet()?.let { greeted = it; caught = null; burst(Offset(v.x, v.y - 0.05f)); tick(firm = true); Sounds.chime(context) }
                                return@detectTapGestures
                            }
                        }
                        if (paperNow && fx > PAPER_LEFT && fy < PAPER_BOTTOM) { onUnroll(); tick(); caught = "All clean again."; return@detectTapGestures }
                        // The bridge leads to friends' camps along the river.
                        if (bridgeNow && fx in 0.06f..0.37f && fy in 0.48f..0.62f) { onOpenRiver(null); return@detectTapGestures } // the bridge or its signpost
                        // A thing in the camp: the companion uses it (the guitar: another strum).
                        val hit = spotAt(Offset(fx, fy), size.width / size.height.toFloat(), localNow, placedNow)
                        val thing = hit?.let { placedNow[it] }
                        if (thing != null) {
                            tick()
                            if (using?.second == thing && thing == "guitar") { strumAt = frame; useStart = frame - ((walk + 1f) * 1e9f).toLong(); Sounds.strum(context) }
                            else {
                                // Walk over first, unless already there; the sound starts on arrival.
                                using?.let { onRest(it.first) } // switching mid-use: from where they are now
                                walk = if ((using?.first ?: restNow) == hit) 0f else Uses.WALK
                                using = hit to thing; caught = null; Sounds.forUse(context, thing, (walk * 1000).toLong()); onUsed(thing)
                            }
                        } else if (using != null) { using = null; Sounds.stop() }
                    }
                }.pointerInput(Unit) {
                    // Inside the tap handler, so it sees the finger first: once a thing is picked up, the drag is
                    // its own (the camp doesn't pan, and the tap handler only waits for the finger to lift).
                    var grab = Offset.Zero
                    var kind = app.tend.core.SpotKind.SMALL
                    fun drop() {
                        val (spot, at) = drag ?: return
                        drag = null
                        fingerInView = -1f
                        local = localNow + (spot to at)
                        onPlace(spot, at)
                        settling = spot
                        caught = null
                        tick(firm = true)
                        Sounds.place(context)
                        scope.launch { lift.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.32f, stiffness = 520f)); if (drag == null) settling = null }
                        scope.launch { dustAt = spot; dust.snapTo(0f); dust.animateTo(1f, androidx.compose.animation.core.tween(650)) }
                        // If the companion's place moved with it, they walk over.
                        scope.launch {
                            restClock.animateTo(Uses.WALK + 0.05f, androidx.compose.animation.core.tween(((Uses.WALK + 0.05f) * 1000).toInt(), easing = androidx.compose.animation.core.LinearEasing))
                            restFrom = null
                        }
                    }
                    detectDragGesturesAfterLongPress(
                        onDragStart = { at ->
                            val ratio = size.width / size.height.toFloat()
                            val f = Offset(at.x / size.width, at.y / size.height)
                            val hit = spotAt(f, ratio, localNow, placedNow) ?: return@detectDragGesturesAfterLongPress
                            val thing = placedNow[hit]?.let(app.tend.core.Things::byId) ?: return@detectDragGesturesAfterLongPress
                            if (!app.tend.core.Clearing.movable(thing)) {
                                tick(firm = true)
                                caught = "The ${thing.name.lowercase()} has its own place."
                                return@detectDragGesturesAfterLongPress
                            }
                            if (using != null) { using?.let { onRest(it.first) }; using = null; Sounds.stop() }
                            kind = thing.kind
                            val from = spotPos(hit, localNow)
                            grab = from - f
                            drag = hit to from
                            settling = null
                            fingerInView = at.x - pan.value
                            // The companion waits where they are until it's put down.
                            if (restFrom == null) restFrom = localNow
                            scope.launch { restClock.snapTo(0f) }
                            scope.launch { lift.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 380f)) }
                            tick(firm = true)
                            caught = "Drag the ${thing.name.lowercase()} anywhere on the meadow."
                        },
                        onDrag = { change, _ ->
                            val (spot, _) = drag ?: return@detectDragGesturesAfterLongPress
                            change.consume()
                            fingerInView = change.position.x - pan.value
                            val f = Offset(change.position.x / size.width, change.position.y / size.height) + grab
                            val (x, y) = app.tend.core.Clearing.keepOnMeadow(kind, f.x, f.y, size.width / size.height.toFloat())
                            drag = spot to Offset(x, y)
                        },
                        onDragEnd = { drop() },
                        onDragCancel = { drop() },
                    )
                },
            ) {
                // Cheered: hearts where they stand (spot -2: no acorns fly, nothing was earned).
                val use = cheerT.takeIf { it >= 0f }?.let { InUse(-2, "act_hearts", it) }
                    ?: using?.takeIf { useStart >= 0 }?.let { (spot, thing) -> InUse(spot, thing, (frame - useStart) / 1e9f, walk) } ?: idle
                drawClearing(
                    avatar, placed, mood, worn = worn, night = night, paper = paperLeft, ducks = ducks.size, bridge = bridge,
                    use = use, lastStrum = (frame - strumAt) / 1e9f - walk, visitor = visitor, rest = rest, nightClock = nightClock, appear = appear,
                    bursts = bursts.map { it.first to it.second.value },
                    layout = drag?.let { (spot, at) -> local + (spot to at) } ?: local,
                    lifted = (drag?.first ?: settling)?.let { it to lift.value },
                    dust = dustAt?.let { it to dust.value },
                    restFrom = restFrom, restClock = restClock.value,
                    ambient = ambient, guests = guests,
                )
            }
            // Near the screen's edge while dragging: the camp slides along, and the thing with it.
            val edge = with(androidx.compose.ui.platform.LocalDensity.current) { 56.dp.toPx() }
            // Only while the finger is near an edge, so holding still costs nothing.
            val nearEdge by remember { androidx.compose.runtime.derivedStateOf {
                val view = sceneWidthPx - pan.maxValue
                drag != null && fingerInView >= 0f && (fingerInView < edge || fingerInView > view - edge)
            } }
            androidx.compose.runtime.LaunchedEffect(nearEdge) {
                while (nearEdge) {
                    androidx.compose.runtime.withFrameNanos { }
                    val view = sceneWidthPx - pan.maxValue
                    val x = fingerInView
                    val v = if (x < edge) -(edge - x) / edge * 14f else (x - (view - edge)) / edge * 14f
                    val moved = pan.scrollBy(v)
                    val d = drag ?: break
                    if (moved != 0f && sceneWidthPx > 0) {
                        val f = d.second + Offset(moved / sceneWidthPx, 0f)
                        val kindNow = placedNow[d.first]?.let(app.tend.core.Things::byId)?.kind ?: app.tend.core.SpotKind.SMALL
                        val (nx, ny) = app.tend.core.Clearing.keepOnMeadow(kindNow, f.x, f.y, CAMP_RATIO)
                        drag = d.first to Offset(nx, ny)
                    } else if (moved == 0f) break // at the end of the camp
                }
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
        val cheerShown = remember { mutableStateOf("") }
        cheerCard?.let { cheerShown.value = it }
        androidx.compose.animation.AnimatedVisibility(
            cheerCard != null, Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.7f),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { -it },
        ) {
            Text(
                "\uD83D\uDC4F  ${cheerShown.value}", style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Brand.Card).padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
    Text(
        when {
            drag != null && caught != null -> caught!!
            using != null -> Uses.line(using!!.second, avatar.displayName)
            paper -> "Someone toilet-papered your oak! Tap it to unroll."
            visitor != null -> "A ${visitor.title.lowercase()} came to the camp. Tap it to say hello."
            ducks.isNotEmpty() -> "A rubber duck: someone visited. Tap it to fish it out."
            caught != null -> caught!!
            bridge -> "Cross the bridge to visit your friends' camps."
            // Once the camp is familiar, what the companion is up to; before that, how it works.
            placed.isNotEmpty() -> if (placed.size >= 3) remember(java.time.LocalTime.now().hour) {
                java.time.LocalDateTime.now().let { app.tend.core.Voice.camp(avatar.displayName, it, app.tend.core.Weather.of(it.toLocalDate(), it.hour)) }
            } else "Tap something: ${avatar.displayName} will use it. Hold it to move it."
            else -> "Earn acorns by living; spend them in the shop below."
        },
        style = MaterialTheme.typography.bodyMedium,
    )

    FirstStepsCard(steps, onClaim = { onClaimStep(it); Sounds.chime(context) }, onGo = { step ->
        when (step) {
            app.tend.core.Step.PLACE, app.tend.core.Step.TREES, app.tend.core.Step.BETTER_FIRE, app.tend.core.Step.SHELTER, app.tend.core.Step.ON_THE_RIVER -> tab = 0
            app.tend.core.Step.USE, app.tend.core.Step.GREET, app.tend.core.Step.THREE_THINGS -> scope.launch { scroll.animateScrollTo(0) }
            app.tend.core.Step.FRIEND, app.tend.core.Step.TOGETHER -> tab = 1
            app.tend.core.Step.FIVE_ANIMALS, app.tend.core.Step.RARE -> tab = 2
            else -> onStep(step)
        }
    })

    // One thing at a time below the camp: the shop, friends, or what's been collected.
    Tabs(listOf("Shop", "Friends", "Collection"), tab) { tab = it }
    when (tab) {
        0 -> Shop(acorns, placed, earnedToday.values.sum() + lastNight, avatar.displayName, plus = plus, onPlus = onPlus, outfits = outfits, outfitWorn = outfitWorn,
            onBuyOutfit = { a -> onBuyOutfit(a).also { if (it) { tick(firm = true); Sounds.chime(context); scope.launch { scroll.animateScrollTo(0) } } } },
            onWearOutfit = { tick(); onWearOutfit(it) }, onBuy = { id ->
            // Bought: a firm tick, a chime, and up to the camp to watch it pop in.
            val spot = Things.byId(id)?.let { Clearing.spotFor(placed, it) }
            onBuy(id).also { ok -> if (ok) { tick(firm = true); Sounds.chime(context); spot?.let { showSpot(it) } } }
        }, onSell = onSell)
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
private fun Shop(
    acorns: Int, placed: Map<Int, String>, earnedToday: Int, name: String, plus: Boolean, onPlus: () -> Unit,
    outfits: Set<app.tend.core.Accessory>, outfitWorn: app.tend.core.Accessory?, onBuyOutfit: (app.tend.core.Accessory) -> Boolean, onWearOutfit: (app.tend.core.Accessory?) -> Unit,
    onBuy: (String) -> Boolean, onSell: (String) -> Unit,
) {
    var outfitOpen by remember { mutableStateOf<app.tend.core.Accessory?>(null) }
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
        // The season's own, first: sold only while it lasts.
        val season = app.tend.core.Season.of(java.time.LocalDate.now())
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) {
            Text("THIS ${season.title.uppercase()} ONLY", style = MaterialTheme.typography.labelSmall, color = Brand.Coral, modifier = Modifier.weight(1f))
            Text("Yours to keep ", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = Brand.Muted)
            PlusMark()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            Things.forSeason(season).forEach { thing ->
                val owned = thing.id in placed.values
                ShopTile(thing, owned = owned, affordable = plus && acorns >= thing.price, Modifier.weight(1f), background = Brand.Coral.copy(alpha = 0.12f)) { if (plus || owned) open = thing else onPlus() }
            }
        }
        // Something to wear: this season's outfit, and any bought before (tap to put on or take off).
        val wardrobe = (outfits + app.tend.core.Accessory.outfitFor(season)).sortedBy { it.ordinal }
        Text("FOR ${name.uppercase()} TO WEAR", style = MaterialTheme.typography.labelSmall, color = Brand.Muted, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
        wardrobe.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                row.forEach { a ->
                    val owned = a in outfits
                    OutfitTile(a, owned, worn = a == outfitWorn, affordable = plus && acorns >= app.tend.core.Accessory.OUTFIT_PRICE, Modifier.weight(1f)) {
                        when {
                            owned -> onWearOutfit(if (a == outfitWorn) null else a)
                            plus -> outfitOpen = a
                            else -> onPlus()
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        // Bought in other seasons: still here, to sell if wanted.
        val earlier = app.tend.core.Season.entries.filter { it != season }.flatMap { Things.forSeason(it) }.filter { it.id in placed.values }
        if (earlier.isNotEmpty()) {
            Text("FROM OTHER SEASONS", style = MaterialTheme.typography.labelSmall, color = Brand.Muted, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
            earlier.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    row.forEach { thing -> ShopTile(thing, owned = true, affordable = true, Modifier.weight(1f)) { open = thing } }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
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
    outfitOpen?.let { a ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { outfitOpen = null },
            icon = { androidx.compose.foundation.Image(remember(a) { CharacterPainter.accessoryIcon(a, 240).asImageBitmap() }, null, Modifier.size(84.dp)) },
            title = { Text(a.title) },
            text = { Text("For ${name} to wear, now and every season after. You can take it off any time.") },
            confirmButton = {
                val price = app.tend.core.Accessory.OUTFIT_PRICE
                if (acorns >= price) androidx.compose.material3.TextButton(onClick = { if (onBuyOutfit(a)) outfitOpen = null }) { Text("Get it for $price", color = Brand.Moss) }
                else androidx.compose.material3.TextButton(onClick = { outfitOpen = null }) { Text("${price - acorns} more acorns to go", color = Brand.Muted) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { outfitOpen = null }) { Text("Close") } },
            containerColor = Brand.Card,
        )
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

/** The thing nearest to a tap (in scene fractions), if it's close enough: its spot. */
private fun spotAt(tap: Offset, ratio: Float, layout: Map<Int, Offset>, placed: Map<Int, String>): Int? = placed.keys
    .filter { it in SpotPositions }
    .map { id ->
        val p = spotPos(id, layout)
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
    modifier.clip(RoundedCornerShape(50)).background(Color(0xE6FFFFFF)).then(acornsExplained()).padding(horizontal = 10.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    AcornIcon(18.dp)
    Spacer(Modifier.width(5.dp))
    Text("$acorns", style = MaterialTheme.typography.titleMedium, color = Color(0xFF3D2A1C))
}

/** Tapping an acorn count explains acorns. */
@Composable
private fun acornsExplained(): Modifier {
    var open by remember { mutableStateOf(false) }
    if (open) AcornsDialog { open = false }
    return Modifier.tappable { open = true }
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

/** One of the seasons' outfits in the shop: its price, or (bought) whether it's on. */
@Composable
private fun OutfitTile(a: app.tend.core.Accessory, owned: Boolean, worn: Boolean, affordable: Boolean, modifier: Modifier, onTap: () -> Unit) = Column(
    modifier.clip(RoundedCornerShape(16.dp)).background(if (worn) Brand.Picked else Brand.Coral.copy(alpha = 0.12f)).tappable(onClick = onTap).padding(vertical = 8.dp, horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    val icon = remember(a) { CharacterPainter.accessoryIcon(a, 144).asImageBitmap() }
    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) { androidx.compose.foundation.Image(icon, null, Modifier.size(44.dp)) }
    Text(a.title, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        when {
            worn -> Text("Wearing", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = Brand.Moss)
            owned -> Text("Put on", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = Brand.Muted)
            else -> {
                Text("${app.tend.core.Accessory.OUTFIT_PRICE}", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = if (affordable) Brand.Moss else Brand.Muted)
                Spacer(Modifier.width(3.dp))
                AcornIcon(12.dp)
            }
        }
    }
}
