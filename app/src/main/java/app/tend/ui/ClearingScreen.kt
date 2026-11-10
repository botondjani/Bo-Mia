package app.tend.ui

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.animateFloat
import androidx.compose.material.icons.filled.Check
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import kotlinx.coroutines.flow.first
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


/**
 * The clearing page: the camp, acorns to spend, what today earned and what to save for. Tap
 * "Place things" (or the scene) to see the spots; tap a spot to buy, swap or sell.
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
    /** Today's minutes: left, available and still to earn, shown in the header as on home; null hides them. */
    minutes: Triple<Int, Int, Int>? = null,
    /** No town set yet: a card asking for one (for the real weather), until set or [onTownLater]. */
    askTown: Boolean = false,
    onTown: (app.tend.data.WeatherService.Place?) -> Unit = {},
    onTownLater: () -> Unit = {},
    /** Presents from friends to open (who sent each); opening them is [onOpenGifts]. */
    gifts: List<String> = emptyList(),
    onOpenGifts: () -> Unit = {},
    /** Opened by tapping the animal on home: greet it right away. */
    greetOnOpen: Boolean = false,
    onGreetedOnOpen: () -> Unit = {},
    /** How far you've come (for the milestones), and the ones already celebrated. */
    totals: app.tend.core.Totals? = null,
    milestonesSeen: Set<app.tend.core.Milestone> = emptySet(),
    onMilestonesSeen: (Set<app.tend.core.Milestone>) -> Unit = {},
) {
    val unlocked = totals?.let { app.tend.core.Milestone.reached(it) }.orEmpty()
    val scroll = rememberScrollState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Column(
    Modifier.fillMaxSize().background(Brand.Sage).verticalScroll(scroll)
        .padding(horizontal = 20.dp).padding(top = Space.xs, bottom = Space.xxl),
    verticalArrangement = Arrangement.spacedBy(Space.m),
) {
    // The shop and friends open from buttons, over the page; the challenges fold into a strip.
    var friendsOpen by rememberSaveable { mutableStateOf(openInvite) }
    androidx.compose.runtime.LaunchedEffect(openInvite) { if (openInvite) friendsOpen = true }
    var shopOpen by rememberSaveable { mutableStateOf(false) }
    var stepsOpen by rememberSaveable { mutableStateOf(false) }
    var collectionAt by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    // The same header as home, settings and all (nothing moves going between them).
    TodayHeader(java.time.LocalDate.now(), minutes, {}) {
        IconButtonSoft(androidx.compose.material.icons.Icons.Filled.Settings, "Settings", onOpenSettings)
    }

    // What friends did: toilet paper unrolls when tapped; a visitor's duck is fished out.
    val paperLeft by androidx.compose.animation.core.animateFloatAsState(if (paper) 1f else 0f, androidx.compose.animation.core.tween(1400), label = "paper")
    val ducksNow by androidx.compose.runtime.rememberUpdatedState(ducks)
    val giftsNow by androidx.compose.runtime.rememberUpdatedState(gifts)
    val paperNow by androidx.compose.runtime.rememberUpdatedState(paper)
    val bridgeNow by androidx.compose.runtime.rememberUpdatedState(bridge)
    var caught by remember { mutableStateOf<String?>(null) }
    // Moving things: hold one to pick it up, drag it anywhere on the meadow, let go to put it down.
    // Shown at once where it was put (the saved layout catches up right after).
    var local by remember { mutableStateOf(layout) }
    androidx.compose.runtime.LaunchedEffect(layout) { local = layout }
    val localNow by androidx.compose.runtime.rememberUpdatedState(local)
    val nightNow by androidx.compose.runtime.rememberUpdatedState(night)
    // Things put somewhere new, for the companion to carry over (spot, from, to), the first one now.
    // Until it's there, a thing shows where it was (saved at once all the same).
    val carries = remember { androidx.compose.runtime.mutableStateListOf<Triple<Int, Offset, Offset>>() }
    val carry = remember { mutableStateOf<Carry?>(null) } // read only where it's drawn
    var carrying by remember { mutableStateOf(false) }
    val pinned = if (carries.isEmpty()) local else local + carries.associate { it.first to it.second }
    val pinnedNow by androidx.compose.runtime.rememberUpdatedState(pinned)
    var drag by remember { mutableStateOf<Pair<Int, Offset>?>(null) } // spot, where (scene fractions)
    var settling by remember { mutableStateOf<Int?>(null) } // just put down, still springing
    val lift = remember { androidx.compose.animation.core.Animatable(0f) }
    var dustAt by remember { mutableStateOf<Int?>(null) }
    val dust = remember { androidx.compose.animation.core.Animatable(1f) }
    // The companion's place before the move, and a clock for walking over to the new one.
    var restFrom by remember { mutableStateOf<Map<Int, Offset>?>(null) }
    val restClock = remember { androidx.compose.animation.core.Animatable(100f) }
    // An animal just greeted: a little card pops up on the camp for a moment.
    var greeted by remember { mutableStateOf<app.tend.core.Visitor?>(null) }
    androidx.compose.runtime.LaunchedEffect(greeted) { if (greeted != null) { kotlinx.coroutines.delay(4000); greeted = null } }
    // The companion using a thing: which spot and thing, and the frame clock that moves it.
    var using by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var walk by remember { mutableStateOf(0f) }
    val restNow by androidx.compose.runtime.rememberUpdatedState(rest)
    var useStart by remember { androidx.compose.runtime.mutableLongStateOf(-1L) }
    var frame by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var strumAt by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    val context = androidx.compose.ui.platform.LocalContext.current
    // Things tapped while busy wait their turn (a few at most), without a fuss: one after another.
    val queue = remember { androidx.compose.runtime.snapshots.SnapshotStateList<Int>() }
    // Woken in the night for something: when it's done, back to bed (the walk there starts again).
    var backToBed by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    // Sent back to bed in the same moment a use ends (so no frame shows them anywhere else in between).
    val sentToBed = remember { booleanArrayOf(false) }
    androidx.compose.runtime.LaunchedEffect(using) {
        val u = using ?: return@LaunchedEffect
        useStart = -1L
        while (true) {
            androidx.compose.runtime.withFrameNanos { frame = it; if (useStart < 0) { useStart = it; strumAt = it } }
            // Done: they stay there.
            if ((frame - useStart) / 1e9f > walk + Uses.seconds(u.second)) {
                using = null; onRest(u.first)
                if (night && queue.isEmpty()) { backToBed++; sentToBed[0] = true }
                break
            }
        }
    }
    val placedNow by androidx.compose.runtime.rememberUpdatedState(placed)
    val nightClock = rememberNightClock(night, backToBed, asleep = rest != null && rest == bedOf(placed))

    /** Over to the thing at [hit] and use it, from where they are now (slower at night; a yawn first out of bed). */
    fun startUse(hit: Int, thing: String) {
        // Switching, or stopped part way: from where they are now. The firewood and the rod: over to them again.
        val here = if (using != null) Whereabouts.drawn else Whereabouts.standing
        var w = if (here == null && restNow == hit && thing !in setOf("woodpile", "fishing_rod")) 0f
            else walkSeconds(here ?: restFraction(restNow, pinnedNow), restFraction(hit, pinnedNow))
        if (nightNow) w *= Uses.NIGHT_SLOW
        // Asleep in the shelter: a yawn first, on the way out.
        if (nightNow && using == null && restNow != null && restNow == bedOf(placedNow) && hit != restNow) w += Uses.YAWN
        walk = w
        using = hit to thing; caught = null; Sounds.forUse(context, thing, (w * 1000).toLong()); onUsed(thing)
    }

    // One done: on to the next waiting, straight from there; nothing waiting at night: back to bed.
    val wasUsing = remember { booleanArrayOf(false) }
    androidx.compose.runtime.LaunchedEffect(using == null) {
        if (using == null && wasUsing[0]) {
            val next = queue.firstOrNull()
            if (next != null) {
                queue.removeAt(0)
                kotlinx.coroutines.delay(250)
                placedNow[next]?.let { startUse(next, it) }
            } else if (night && !sentToBed[0]) backToBed++
        }
        wasUsing[0] = using != null
        sentToBed[0] = false
    }
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
    // After buying something: up to the camp to watch it pop in.
    fun showSpot(@Suppress("UNUSED_PARAMETER") spot: Int) = scope.launch { scroll.animateScrollTo(0) }
    // Once in bed, they stay there: next time it doesn't walk again.
    androidx.compose.runtime.LaunchedEffect(nightClock > Uses.BED_WALK + 2f) {
        val bed = bedOf(placed)
        if (night && nightClock > Uses.BED_WALK + 2f && bed != null && rest != bed) onRest(bed)
    }
    val visitorNow by androidx.compose.runtime.rememberUpdatedState(visitor)
    // Cheered by friends: a card at the top, and the companion beaming (hearts) for a few seconds.
    var cheerCard by remember { mutableStateOf<String?>(null) }
    var cheerT by remember { androidx.compose.runtime.mutableFloatStateOf(-1f) }
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
    // Tapping the companion: a little reaction where they stand (a cheer, hearts, a stretch) and a line.
    val companionAt = remember { floatArrayOf(-1f, -1f) }
    var pokeAct by remember { mutableStateOf<String?>(null) }
    var pokeT by remember { androidx.compose.runtime.mutableFloatStateOf(-1f) }
    fun poke() {
        if (pokeAct != null) return
        val i = kotlin.random.Random.nextInt(4)
        pokeAct = listOf("act_cheer", "act_hearts", "act_stretch", "act_cheer")[i]
        caught = listOf("${avatar.displayName} giggles. That tickles!", "${avatar.displayName} blushes a little.", "${avatar.displayName} stretches and smiles at you.", "${avatar.displayName} does a happy little dance.")[i]
        tick(firm = true)
        scope.launch {
            val start = androidx.compose.runtime.withFrameNanos { it }
            while (true) {
                pokeT = (androidx.compose.runtime.withFrameNanos { it } - start) / 1e9f
                if (pokeT > 2.6f) break
            }
            pokeT = -1f
            pokeAct = null
        }
    }
    // Came from tapping the animal on home: a moment for the page, then the greeting.
    androidx.compose.runtime.LaunchedEffect(greetOnOpen) {
        if (!greetOnOpen) return@LaunchedEffect
        kotlinx.coroutines.delay(650)
        visitor?.let { v -> onGreet()?.let { greeted = it; caught = null; burst(Offset(v.x, v.y - 0.05f)); tick(firm = true); Sounds.chime(context) } }
        onGreetedOnOpen()
    }
    // A milestone just reached: what it adds to the camp, with sparkles where it appears.
    val fresh = unlocked - milestonesSeen
    androidx.compose.runtime.LaunchedEffect(fresh) {
        val m = fresh.firstOrNull() ?: return@LaunchedEffect
        kotlinx.coroutines.delay(900)
        caught = "New in your camp: ${m.adds}! (${m.title})"
        burst(MILESTONE_AT.getValue(m)); tick(firm = true); Sounds.chime(context)
        kotlinx.coroutines.delay(2500)
        onMilestonesSeen(setOf(m))
    }
    // Carrying things over, one by one, once the finger's off and the thing has settled.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            androidx.compose.runtime.snapshotFlow { carries.isNotEmpty() && drag == null && settling == null }.first { ready -> ready }
            carrying = true
            // Busy with something: they put it down where they are (and go from there).
            if (using != null) { using = null; queue.clear(); Sounds.stop() }
            // A couple of frames for where they stand to catch up (an idle use just stopped).
            repeat(2) { androidx.compose.runtime.withFrameNanos { } }
            val job = carries.firstOrNull() ?: run { carrying = false; null } ?: continue
            val (spot, from, to) = job
            val walk = walkSeconds(Whereabouts.standing ?: Whereabouts.drawn ?: restFraction(restNow, pinnedNow), from)
            val move = walkSeconds(from, to) * 1.25f
            val down = walk + Carry.PICK + move + Carry.DOWN
            val start = androidx.compose.runtime.withFrameNanos { it }
            var t = 0f
            var landed = false
            // The last one to move: afterwards a walk round it to see how it looks (more to move: straight on).
            var admire = carries.size == 1
            while (t < Carry.length(walk, move, admire)) {
                t = (androidx.compose.runtime.withFrameNanos { it } - start) / 1e9f
                if (admire && carries.size > 1 && t < down) admire = false
                carry.value = Carry(spot, from, to, t, walk, move, admire)
                if (!landed && t >= down) {
                    // Put down: it's there now (a puff of dust, a soft thud).
                    landed = true
                    carries.remove(job)
                    tick()
                    Sounds.place(context)
                    scope.launch { dustAt = spot; dust.snapTo(0f); dust.animateTo(1f, androidx.compose.animation.core.tween(650)) }
                }
            }
            if (!landed) carries.remove(job)
            onRest(spot)
            carry.value = null
            if (carries.isEmpty()) { carrying = false; if (caught?.endsWith("carry it over.") == true) caught = null }
        }
    }
    // Camp life: clouds and weather move, and the companion gets on with things when left alone.
    // A sheet over the page (the shop, friends): the camp holds still behind it, so the sheet moves smoothly.
    val ambient = rememberAmbientClock(paused = shopOpen || friendsOpen)
    // Until something has been moved: which small thing hints that things can be picked up.
    // Something light and easy to carry, ideally (not the firewood, which belongs by the fire).
    val hintSpot = if (layout.isNotEmpty() || night) null else placed.entries.filter { (spot, id) ->
        spot in SpotPositions && id != "woodpile" &&
            app.tend.core.Things.byId(id)?.let { app.tend.core.Clearing.movable(it) && it.kind == app.tend.core.SpotKind.SMALL } == true
    }.minByOrNull { (_, id) -> listOf("lantern", "wildflowers", "guitar", "telescope").indexOf(id).let { if (it < 0) 9 else it } }?.key
    val idle = rememberIdleLife(placed, rest, enabled = using == null && drag == null && settling == null && !carrying && carries.isEmpty() && !shopOpen && !friendsOpen && greeted == null && pokeAct == null && !night, layout = local) { onRest(it) }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { Sounds.stop() } }

    // The camp, big and edge to edge: wider than the screen, so it pans left and right.
    androidx.compose.foundation.layout.BoxWithConstraints(
        Modifier.fillMaxWidth().layout { m, c ->
            val extra = 20.dp.roundToPx()
            val p = m.measure(c.copy(minWidth = c.maxWidth + 2 * extra, maxWidth = c.maxWidth + 2 * extra))
            layout(c.maxWidth, p.height) { p.place(-extra, 0) }
        },
    ) {
        // The whole camp, framed like on home: nothing cut off at the sides.
        val sceneHeight = maxWidth / CAMP_ASPECT
        Box(Modifier.fillMaxWidth().height(sceneHeight).campEntrance()) {
            Canvas(
                Modifier.fillMaxSize().then(sharedCamp() ?: Modifier).pointerInput(Unit) {
                    // Long presses belong to moving things: a tap only counts when it's quick.
                    detectTapGestures(onLongPress = {}) { tap ->
                        val fx = tap.x / size.width
                        val fy = tap.y / size.height
                        // The companion themselves (standing about, not mid-walk): a reaction.
                        val (cx, cy) = companionAt[0] to companionAt[1]
                        if (cx >= 0f && using == null && idle.value == null && carry.value == null && !night &&
                            kotlin.math.abs(cx - fx) * size.width < size.height * 0.08f && fy in (cy - 0.17f)..(cy + 0.02f)
                        ) { poke(); return@detectTapGestures }
                        // Asleep in the shelter: tapping it lets them sleep (nothing to do there at night).
                        if (nightNow) bedOf(placedNow)?.let { bed ->
                            if (restNow == bed && spotAt(Offset(fx, fy), size.width / size.height.toFloat(), pinnedNow, placedNow) == bed) {
                                caught = "Shh\u2026 ${avatar.displayName} is asleep in the ${Things.byId(placedNow.getValue(bed))?.name?.lowercase() ?: "tent"}."
                                tick(); return@detectTapGestures
                            }
                        }
                        // A present from a friend: opened, its acorns fly out.
                        if (giftsNow.isNotEmpty() && kotlin.math.abs(GiftSpot.x - fx) * size.width < size.height * 0.07f && fy in (GiftSpot.y - 0.1f)..(GiftSpot.y + 0.03f)) {
                            val who = giftsNow.distinct()
                            caught = "${who.joinToString(" and ")} sent you ${if (giftsNow.size == 1) "a present" else "presents"}: ${giftsNow.size * app.tend.data.Friends.GIFT_ACORNS} acorns!"
                            burst(Offset(GiftSpot.x, GiftSpot.y - 0.05f)); tick(firm = true); Sounds.acorns(context)
                            onOpenGifts()
                            return@detectTapGestures
                        }
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
                        val hit = spotAt(Offset(fx, fy), size.width / size.height.toFloat(), pinnedNow, placedNow)
                        val thing = hit?.let { placedNow[it] }
                        if (thing != null && (carrying || carries.isNotEmpty())) { caught = "${avatar.displayName} is busy moving things. One moment!"; return@detectTapGestures }
                        if (thing != null) {
                            tick()
                            when {
                                using?.second == thing && thing == "guitar" -> { strumAt = frame; useStart = frame - ((walk + 1f) * 1e9f).toLong(); Sounds.strum(context) }
                                using?.first == hit -> {}
                                // Busy: it waits its turn, quietly (three at most).
                                using != null -> if (hit !in queue && queue.size < 3) queue += hit
                                // Walk over first, unless already there; the sound starts on arrival.
                                else -> startUse(hit, thing)
                            }
                        } else if (using != null) { using = null; queue.clear(); Sounds.stop() }
                    }
                }.pointerInput(Unit) {
                    // Inside the tap handler, so it sees the finger first: once a thing is picked up, the drag is
                    // its own (the camp doesn't pan, and the tap handler only waits for the finger to lift).
                    var grab = Offset.Zero
                    var kind = app.tend.core.SpotKind.SMALL
                    // What else stands on the meadow (where it's going, if it's being carried): not to be put on.
                    var others = emptyList<Pair<Float, Pair<Float, Float>>>()
                    var half = 0.02f
                    fun drop() {
                        val (spot, at) = drag ?: return
                        drag = null
                        // The companion carries it over (big things dragged). At night things hop there themselves: they're asleep.
                        val carried = !nightNow
                        if (carried) {
                            val from = spotPos(spot, pinnedNow)
                            carries.removeAll { it.first == spot }
                            carries += Triple(spot, from, at)
                        }
                        local = localNow + (spot to at)
                        onPlace(spot, at)
                        settling = spot
                        caught = if (carried) "${avatar.displayName} will carry it over." else null
                        tick(firm = true)
                        if (!carried) Sounds.place(context)
                        scope.launch { lift.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.32f, stiffness = 520f)); if (drag == null) settling = null }
                        if (!carried) scope.launch { dustAt = spot; dust.snapTo(0f); dust.animateTo(1f, androidx.compose.animation.core.tween(650)) }
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
                            val hit = spotAt(f, ratio, pinnedNow, placedNow) ?: return@detectDragGesturesAfterLongPress
                            if (carry.value?.spot == hit) return@detectDragGesturesAfterLongPress // in the companion's hands
                            val thing = placedNow[hit]?.let(app.tend.core.Things::byId) ?: return@detectDragGesturesAfterLongPress
                            if (!app.tend.core.Clearing.movable(thing)) {
                                tick(firm = true)
                                caught = "The ${thing.name.lowercase()} has its own place."
                                return@detectDragGesturesAfterLongPress
                            }
                            if (using != null) { using = null; queue.clear(); Sounds.stop() }
                            kind = thing.kind
                            others = placedNow.keys.filter { it != hit && it in SpotPositions }.mapNotNull { id ->
                                val k = app.tend.core.Clearing.spots.first { it.id == id }.kind
                                if (k == app.tend.core.SpotKind.FIRE || k == app.tend.core.SpotKind.WATER || k == app.tend.core.SpotKind.ABOVE) null
                                else spotPos(id, localNow).let { thingWidth(placedNow.getValue(id)) / 2f to (it.x to it.y) }
                            }
                            half = thingWidth(thing.id) / 2f
                            val from = spotPos(hit, pinnedNow)
                            grab = from - f
                            drag = hit to from
                            settling = null
                            // The companion waits where they are until it's put down.
                            if (restFrom == null) restFrom = pinnedNow
                            scope.launch { restClock.snapTo(0f) }
                            scope.launch { lift.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 380f)) }
                            tick(firm = true)
                            caught = "Drag the ${thing.name.lowercase()} anywhere on the meadow."
                        },
                        onDrag = { change, _ ->
                            val (spot, _) = drag ?: return@detectDragGesturesAfterLongPress
                            change.consume()
                            val f = Offset(change.position.x / size.width, change.position.y / size.height) + grab
                            val (x, y) = app.tend.core.Clearing.keepApart(kind, half, f.x, f.y, others, size.width / size.height.toFloat())
                            drag = spot to Offset(x, y)
                        },
                        onDragEnd = { drop() },
                        onDragCancel = { drop() },
                    )
                },
            ) {
                // Cheered: hearts where they stand (spot -2: no acorns fly, nothing was earned).
                val use = pokeAct?.takeIf { pokeT >= 0f }?.let { InUse(-2, it, pokeT) }
                    ?: cheerT.takeIf { it >= 0f }?.let { InUse(-2, "act_hearts", it) }
                    ?: using?.takeIf { useStart >= 0 }?.let { (spot, thing) -> InUse(spot, thing, (frame - useStart) / 1e9f, walk, Uses.seconds(thing)) } ?: idle.value
                drawClearing(
                    avatar, placed, mood, worn = worn, night = night, paper = paperLeft, ducks = ducks.size, bridge = bridge,
                    use = use, lastStrum = (frame - strumAt) / 1e9f - walk, visitor = visitor, rest = rest, nightClock = nightClock, appear = appear,
                    bursts = bursts.map { it.first to it.second.value },
                    layout = drag?.let { (spot, at) -> pinned + (spot to at) } ?: pinned,
                    carry = carry.value,
                    lifted = (drag?.first ?: settling)?.let { it to lift.value }
                        ?: hintSpot?.takeIf { using == null && carry.value == null && idle.value == null }?.let { spot ->
                            // Not moved anything yet: now and then a small thing hops, as if to say "pick me up".
                            val k = ambient.value % 4f
                            if (k < 0.6f) spot to kotlin.math.sin(k / 0.6f * Math.PI.toFloat()) * 0.45f else null
                        },
                    dust = dustAt?.let { it to dust.value },
                    restFrom = restFrom, restClock = restClock.value,
                    ambient = ambient.value, guests = guests,
                    onCompanion = { companionAt[0] = it.x; companionAt[1] = it.y },
                    gifts = gifts.size,
                    unlocked = unlocked,
                )
            }
        }
        // On the camp: the way back home (top left) and the acorns (top right).
        Box(
            Modifier.align(Alignment.TopStart).padding(start = 28.dp, top = 12.dp).size(36.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xE6FFFFFF)).tappable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, "Home", tint = Brand.Moss, modifier = Modifier.size(18.dp)) }
        AcornBadge(acorns, Modifier.align(Alignment.TopEnd).padding(12.dp))
        // Until something has been moved: how, right on the camp.
        if (hintSpot != null && drag == null && carries.isEmpty()) Text(
            "Tip: hold a thing to move it", style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = Brand.Moss,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp)
                .clip(RoundedCornerShape(50)).background(Color(0xE6FFFFFF)).padding(horizontal = 12.dp, vertical = 5.dp),
        )
        val shown = remember { mutableStateOf<app.tend.core.Visitor?>(null) }
        if (greeted != null) shown.value = greeted
        // Greeted: the animal, big, in a card in the middle of the camp ("You found a rare fox!"); a tap closes it.
        androidx.compose.animation.AnimatedVisibility(
            greeted != null, Modifier.align(Alignment.Center),
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180)) +
                androidx.compose.animation.scaleIn(androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 420f), initialScale = 0.6f),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)) + androidx.compose.animation.scaleOut(targetScale = 0.9f),
        ) { shown.value?.let { GreetedCard(it, guestbook, Modifier.tappable { greeted = null }) } }
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
    // What the companion is up to, for a familiar camp (the same for the hour). Worked out every time,
    // not only when shown: a remember inside a changing branch can trip up what follows it.
    val campLine = remember(java.time.LocalTime.now().hour) {
        java.time.LocalDateTime.now().let { app.tend.core.Voice.camp(avatar.displayName, it, app.tend.core.LiveWeather.fresh() ?: app.tend.core.Weather.of(it.toLocalDate(), it.hour)) }
    }
    Text(
        when {
            drag != null && caught != null -> caught!!
            using != null -> Uses.line(using!!.second, avatar.displayName)
            gifts.isNotEmpty() -> "A present from ${gifts.distinct().joinToString(" and ")}! Tap it to open."
            paper -> "Someone toilet-papered your oak! Tap it to unroll."
            visitor != null -> "${visitor.aName.replaceFirstChar { it.uppercase() }} came to the camp. Tap it to say hello."
            ducks.isNotEmpty() -> "A rubber duck: someone visited. Tap it to fish it out."
            caught != null -> caught!!
            hintSpot != null -> "Hold the ${app.tend.core.Things.byId(placed.getValue(hintSpot))?.name?.lowercase()} to pick it up, then drag it anywhere. ${avatar.displayName} carries it over."
            bridge -> "Cross the bridge to visit your friends' camps."
            // Once the camp is familiar, what the companion is up to; before that, how it works.
            placed.isNotEmpty() -> if (placed.size >= 3) campLine else "Tap something: ${avatar.displayName} will use it. Hold it to move it."
            else -> "Earn acorns by living; spend them in the shop below."
        },
        style = MaterialTheme.typography.bodyMedium,
    )

    // An event on (a few days a year): a word about it, and its thing in the shop.
    app.tend.core.CampEvent.on(java.time.LocalDate.now())?.let { event ->
        val left = event.daysLeft(java.time.LocalDate.now())
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brand.Coral.copy(alpha = 0.12f)).tappable { shopOpen = true }.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.size(40.dp)) { drawThingIcon(event.thing) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${event.title.uppercase()} · ${if (left == 1) "LAST DAY" else "$left DAYS LEFT"}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Brand.Coral)
                Text(event.line, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), maxLines = 2)
            }
        }
    }

    // The shop, friends, and sharing the camp: a button each.
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        PillButton("Shop", androidx.compose.material.icons.Icons.Filled.ShoppingCart, Modifier.weight(1f)) { shopOpen = true }
        PillButton("Friends", androidx.compose.material.icons.Icons.Filled.Person, Modifier.weight(1f)) { friendsOpen = true }
        PillButton("Share", androidx.compose.material.icons.Icons.Filled.Share, Modifier.weight(1f), onShare)
    }

    // Set up before the town was asked: once, the real weather offered.
    if (askTown) Card {
        Text("REAL WEATHER IN YOUR CAMP", style = MaterialTheme.typography.labelSmall)
        Text("Set your town and the camp gets its rain, snow and sunshine.", style = MaterialTheme.typography.bodyMedium)
        TownPicker(null, onTown)
        Text("Not now", style = MaterialTheme.typography.labelLarge, color = Brand.Muted, modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable(onClick = onTownLater).padding(vertical = 6.dp))
    }

    // The challenges: a strip with the round's progress; tapped, the whole list unfolds (and folds back).
    androidx.compose.animation.AnimatedContent(
        stepsOpen, label = "steps",
        transitionSpec = {
            (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(260, delayMillis = 80)) togetherWith
                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(140)))
                .using(androidx.compose.animation.SizeTransform(clip = false) { _, _ -> androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = 380f) })
        },
    ) { open ->
    // Folded: what's done waits on top, to claim right away; then the strip with what's next.
    if (!open) Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        ClaimableSteps(steps) { onClaimStep(it); Sounds.acorns(context) }
        FirstStepsStrip(steps, onOpen = { stepsOpen = true }, onGo = { stepsOpen = true }, showWaiting = false)
    }
    else Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
    FirstStepsCard(steps, onClaim = { onClaimStep(it); Sounds.acorns(context) }, onFold = { stepsOpen = false }, onGo = { step ->
        when (step) {
            app.tend.core.Step.PLACE, app.tend.core.Step.TREES, app.tend.core.Step.BETTER_FIRE, app.tend.core.Step.SHELTER, app.tend.core.Step.ON_THE_RIVER -> shopOpen = true
            app.tend.core.Step.USE, app.tend.core.Step.MOVE, app.tend.core.Step.GREET, app.tend.core.Step.THREE_THINGS -> scope.launch { scroll.animateScrollTo(0) }
            app.tend.core.Step.FRIEND, app.tend.core.Step.TOGETHER -> friendsOpen = true
            app.tend.core.Step.FIVE_ANIMALS, app.tend.core.Step.RARE -> scope.launch { scroll.animateScrollTo(collectionAt) }
            else -> onStep(step)
        }
    })
    Text(
        "Show less", style = MaterialTheme.typography.labelLarge, color = Brand.Muted,
        modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(10.dp)).tappable { stepsOpen = false }.padding(horizontal = 12.dp, vertical = 6.dp),
    )
    }
    }

    Column(Modifier.onGloballyPositioned { collectionAt = it.positionInParent().y.toInt() }, verticalArrangement = Arrangement.spacedBy(Space.m)) {
        Guestbook(guestbook)
        totals?.let { MilestonesCard(it) }
        Things(avatar, worn, onShelf)
    }

    // The shop: over the page, all the way up. Bought: it closes, and up to the camp to watch it pop in.
    if (shopOpen) {
        val shopSheet = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        fun closeShopThen(then: () -> Unit) = scope.launch { shopSheet.hide(); shopOpen = false; then() }
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { shopOpen = false }, sheetState = shopSheet, containerColor = Brand.Sage) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = Space.xl)) {
            Shop(acorns, placed, earnedToday.values.sum() + lastNight, avatar.displayName, plus = plus, onPlus = onPlus, outfits = outfits, outfitWorn = outfitWorn,
                onBuyOutfit = { a -> onBuyOutfit(a).also { if (it) { tick(firm = true); Sounds.chime(context); closeShopThen { scope.launch { scroll.animateScrollTo(0) } } } } },
                onWearOutfit = { tick(); onWearOutfit(it) }, onBuy = { id ->
                // Bought: a firm tick, a chime, and up to the camp to watch it pop in.
                val spot = Things.byId(id)?.let { Clearing.spotFor(placed, it) }
                onBuy(id).also { ok -> if (ok) { tick(firm = true); Sounds.chime(context); closeShopThen { spot?.let { showSpot(it) } } } }
            }, onSell = onSell)
            }
        }
    }

    // Friends: over the page, from the bottom.
    if (friendsOpen) androidx.compose.material3.ModalBottomSheet(onDismissRequest = { friendsOpen = false }, containerColor = Brand.Sage) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = Space.xl)) {
            FriendsCard(onOpenSettings, onOpenRiver, onFriendsChanged, openInvite)
        }
    }
}
}

/** Where each milestone's addition is in the camp (scene fractions), for its sparkles. */
private val MILESTONE_AT = mapOf(
    app.tend.core.Milestone.FLAG to Offset(0.37f, 0.6f),
    app.tend.core.Milestone.PATH_LIGHTS to Offset(0.32f, 0.76f),
    app.tend.core.Milestone.GARDEN to Offset(0.63f, 0.58f),
    app.tend.core.Milestone.HUT to Offset(0.735f, 0.3f),
    app.tend.core.Milestone.SHOOTING_STARS to Offset(0.55f, 0.12f),
)

/** The milestones: each with how far along it is, and what it adds to the camp; the reached ones ticked. */
@Composable
private fun MilestonesCard(t: app.tend.core.Totals) = Card {
    val reached = app.tend.core.Milestone.reached(t)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("MILESTONES", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text("${reached.size} / ${app.tend.core.Milestone.entries.size}", style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
    }
    app.tend.core.Milestone.entries.forEach { m ->
        val done = m in reached
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Box(Modifier.size(28.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (done) Brand.Moss else Brand.Sage), contentAlignment = Alignment.Center) {
                if (done) androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.title, style = MaterialTheme.typography.bodyLarge, color = if (done) Brand.Ink else Brand.Ink)
                Text(if (done) "Added ${m.adds}" else "${m.label(t)} · adds ${m.adds}", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
                if (!done) Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(Brand.Sage)) {
                    Box(Modifier.fillMaxWidth(m.progress(t) / m.need.toFloat()).height(4.dp).clip(RoundedCornerShape(50)).background(Brand.Mint))
                }
            }
        }
    }
}

/** A round-ended button with an icon, for the row under the camp. */
@Composable
private fun PillButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) = Row(
    modifier.clip(RoundedCornerShape(50)).background(Brand.Card).tappable(onClick = onClick).padding(vertical = 11.dp),
    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
) {
    androidx.compose.material3.Icon(icon, null, tint = Brand.Moss, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(label, style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
}

/** The camp's shape on its own page: a little taller than on home, so it's bigger. */
private const val CAMP_ASPECT = 1.4f

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
        // An event's own, first, while it's on (a few days): then the season's.
        app.tend.core.CampEvent.on(java.time.LocalDate.now())?.let { event ->
            val left = event.daysLeft(java.time.LocalDate.now())
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) {
                Text("${event.title.uppercase()} · ${if (left == 1) "LAST DAY" else "$left DAYS LEFT"}", style = MaterialTheme.typography.labelSmall, color = Brand.Coral, modifier = Modifier.weight(1f))
                Text("Yours to keep ", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = Brand.Muted)
                PlusMark()
            }
            Text(event.line, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                Things.forEvent(event).forEach { thing ->
                    val owned = thing.id in placed.values
                    ShopTile(thing, owned = owned, affordable = plus && acorns >= thing.price, Modifier.weight(1f), background = Brand.Coral.copy(alpha = 0.12f)) { if (plus || owned) open = thing else onPlus() }
                }
                repeat(2) { Spacer(Modifier.weight(1f)) }
            }
        }
        // The season's own: sold only while it lasts.
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
        val nowEvent = app.tend.core.CampEvent.on(java.time.LocalDate.now())
        val earlier = (app.tend.core.Season.entries.filter { it != season }.flatMap { Things.forSeason(it) } +
            app.tend.core.CampEvent.entries.filter { it != nowEvent }.flatMap { Things.forEvent(it) }).filter { it.id in placed.values }
        if (earlier.isNotEmpty()) {
            Text("FROM OTHER SEASONS AND NIGHTS", style = MaterialTheme.typography.labelSmall, color = Brand.Muted, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
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
private fun GreetedCard(v: app.tend.core.Visitor, book: Map<app.tend.core.Visitor, Int>, modifier: Modifier = Modifier) = Column(
    modifier.width(250.dp).clip(RoundedCornerShape(26.dp)).background(Brand.Card).padding(horizontal = 18.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    // The animal, big, with a little sway and sparkles around it.
    val sway = androidx.compose.animation.core.rememberInfiniteTransition(label = "sway")
    val k by sway.animateFloat(0f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1400), androidx.compose.animation.core.RepeatMode.Reverse), label = "k")
    Box(Modifier.size(116.dp).clip(CircleShape).background(Brand.Sage), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(96.dp).graphicsLayer { rotationZ = (k - 0.5f) * 8f; scaleX = 1f + k * 0.04f; scaleY = 1f + k * 0.04f }) { drawVisitorIcon(v) }
    }
    val first = (book[v] ?: 0) <= 1
    Text(
        when (v.rarity) {
            app.tend.core.Rarity.COMMON -> "You found ${v.aName}!"
            else -> "You found ${if (v.rarity == app.tend.core.Rarity.RARE) "a rare" else "an uncommon"} ${v.title.lowercase()}!"
        },
        style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp),
    )
    Text(
        (if (first) "New in your guestbook! " else "") + "${book.size} of ${app.tend.core.Visitor.entries.size} met",
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = Brand.Muted, textAlign = TextAlign.Center,
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(50)).background(Brand.Moss).padding(horizontal = 12.dp, vertical = 5.dp)) {
        Text("+${v.rarity.acorns}", style = MaterialTheme.typography.labelLarge, color = Color.White)
        Spacer(Modifier.width(4.dp))
        AcornIcon(14.dp)
    }
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
