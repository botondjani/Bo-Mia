package app.tend.ui

import app.tend.core.tr

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.togetherWith
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import app.tend.core.slot
import app.tend.core.Avatar
import app.tend.core.Clearing
import app.tend.core.SpotKind
import app.tend.core.Things
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.CornerRadius


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
    /** The chest: what's in it, putting a thing away, and taking one out. */
    chest: List<String> = emptyList(),
    onPutAway: (thing: String) -> Unit = {},
    onTakeOut: (thing: String) -> Boolean = { false },
    onBack: () -> Unit,
    onShare: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenRiver: (friendId: String?) -> Unit = {},
    paper: Boolean = false,
    bridge: Boolean = false,
    /** A first friend made: the signpost by the path. */
    signpost: Boolean = false,
    onFriendsChanged: () -> Unit = {},
    steps: List<app.tend.core.StepState> = emptyList(),
    onClaimStep: (app.tend.core.Step) -> Unit = {},
    onUsed: (thing: String) -> Unit = {},
    /** Where the companion stays (the spot of the last thing used), and to save a new one. */
    rest: Int? = null,
    onRest: (Int?) -> Unit = {},
    mood: app.tend.core.Mood = app.tend.core.Mood.HAPPY,
    openInvite: Boolean = false,
    /** Open with the friends list up (once: [onFriendsShown] then). */
    openFriends: Boolean = false,
    onFriendsShown: () -> Unit = {},
    /**
     * Open with a sheet up, once ([onSheetShown] then): "shop", "shop:" plus a spot kind for the
     * shop on that filter (e.g. "shop:TREES"), or "things" for the chest.
     */
    openSheet: String? = null,
    onSheetShown: () -> Unit = {},
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
    /** The level (as seen): what the shop has open, and the extra small spot. */
    level: Int = 1,
    onPlus: () -> Unit = {},
    /** The outfits bought, and buying one (it goes on). */
    outfits: Set<app.tend.core.Accessory> = emptySet(),
    onBuyOutfit: (app.tend.core.Accessory) -> Boolean = { false },
    /** Bo's wardrobe: everything owned, what's chosen per place, emptying a place, and dressing by itself. */
    owned: Set<Accessory> = worn,
    chosen: Map<app.tend.core.Slot, Accessory> = emptyMap(),
    onTakeOff: (app.tend.core.Slot) -> Unit = {},
    autoDress: Boolean = false,
    onAutoDress: (Boolean) -> Unit = {},
    /** Opens the challenges (they live on home). */
    onOpenChallenges: () -> Unit = {},
    /** The habits held right now (their things shine), and putting a thing on or off. */
    earned: Set<Accessory> = worn,
    onToggleAccessory: ((Accessory) -> Unit)? = null,
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
    /** Presents from friends to open (who sent each, oldest first); [onOpenGift] opens the oldest. */
    gifts: List<String> = emptyList(),
    onOpenGift: () -> app.tend.data.TendStore.Opened? = { null },
    /** Takes a thing out of the chest into the camp; false when there's no spot for it. */
    onTakeOutThing: (String) -> Boolean = { false },
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
    // The page's height and where the tiles start: they take the rest.
    var pageH by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var hubTop by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var pageTop by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    Column(
    Modifier.fillMaxSize().onSizeChanged { pageH = it.height }.onGloballyPositioned { pageTop = it.positionInRoot().y }.background(Brand.Sage).verticalScroll(scroll)
        .padding(horizontal = 20.dp).padding(top = Space.xs, bottom = Space.xxl),
    verticalArrangement = Arrangement.spacedBy(Space.m),
) {
    // The shop and friends open from buttons, over the page; the challenges fold into a strip.
    var friendsOpen by rememberSaveable { mutableStateOf(openInvite || openFriends) }
    androidx.compose.runtime.LaunchedEffect(openInvite, openFriends) {
        if (openInvite || openFriends) friendsOpen = true
        if (openFriends) onFriendsShown()
    }
    var shopOpen by rememberSaveable { mutableStateOf(false) }
    // A thing from the shop, shown in the camp before buying it.
    var preview by remember { mutableStateOf<app.tend.core.Thing?>(null) }
    var bookOpen by rememberSaveable { mutableStateOf(false) }
    var milestonesOpen by rememberSaveable { mutableStateOf(false) }
    var chestOpen by rememberSaveable { mutableStateOf(false) }
    var shopStart by remember { mutableStateOf<SpotKind?>(null) }
    var shopTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(openSheet) {
        val sheet = openSheet ?: return@LaunchedEffect
        when {
            sheet == "things" -> chestOpen = true
            sheet.startsWith("shop") -> { shopTab = 0; shopStart = sheet.substringAfter(':', "").takeIf { it.isNotEmpty() }?.let { k -> runCatching { SpotKind.valueOf(k) }.getOrNull() }; shopOpen = true }
        }
        onSheetShown()
    }

    // The same header as home, settings and all (nothing moves going between them).
    TodayHeader(java.time.LocalDate.now(), minutes, {}) {
        // Sharing the camp: a small icon by the settings.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButtonSoft(TendIcons.vector("share"), tr("Share", "Megosztás"), onShare)
            IconButtonSoft(TendIcons.vector("gear"), tr("Settings", "Beállítások"), onOpenSettings)
        }
    }

    // What friends did: toilet paper unrolls when tapped; a visitor's duck is fished out.
    val paperLeft by androidx.compose.animation.core.animateFloatAsState(if (paper) 1f else 0f, androidx.compose.animation.core.tween(1400), label = "paper")
    val ducksNow by androidx.compose.runtime.rememberUpdatedState(ducks)
    val fishedOut = remember { mutableSetOf<Long>() }
    val giftsNow by androidx.compose.runtime.rememberUpdatedState(gifts)
    val paperNow by androidx.compose.runtime.rememberUpdatedState(paper)
    val bridgeNow by androidx.compose.runtime.rememberUpdatedState(bridge)
    var caught by remember { mutableStateOf<String?>(null) }
    var giftPop by remember { mutableIntStateOf(0) }
    var opened by remember { mutableStateOf<app.tend.data.TendStore.Opened?>(null) }
    var giftKey by remember { mutableIntStateOf(0) }
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
        // The clock starts on the next frame: not a frame drawn on the last use's clock (they'd show up there, arrived).
        useStart = -1L
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
    val fireBuild = rememberFireBuild(placed)
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
        cheerCard = if (cheers.size == 1) tr("${cheers[0]} cheered you on", "${cheers[0]} megtapsolt") else tr("${cheers.dropLast(1).joinToString(", ")} and ${cheers.last()} cheered you on", "${cheers.dropLast(1).joinToString(", ")} és ${cheers.last()} megtapsolt")
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
    // Poked again and again: they notice (and say so).
    val pokes = remember { mutableListOf<Long>() }
    fun poke() {
        if (pokeAct != null) return
        val n = avatar.displayName
        val now = System.currentTimeMillis()
        pokes.removeAll { now - it > 12_000 }
        pokes += now
        val hour = java.time.LocalTime.now().hour
        // What fits right now first (poked a lot, late, an outfit on), otherwise one of the everyday ones.
        val (act, line) = when {
            pokes.size >= 4 -> "act_stretch" to tr("$n laughs: \"Okay, okay, I'm awake!\"", "$n nevet: „Jó, jó, ébren vagyok!”")
            pokes.size == 3 -> "act_hearts" to tr("$n pokes you back. Fair's fair.", "$n visszabök. Így igazságos.")
            (hour >= 22 || hour < 6) && kotlin.random.Random.nextBoolean() -> "act_stretch" to tr("$n yawns a big yawn. Bedtime soon?", "$n nagyot ásít. Lassan alvás?")
            worn.any { it.isOutfit } && kotlin.random.Random.nextInt(3) == 0 -> worn.first { it.isOutfit }.let { o -> "act_cheer" to tr("$n straightens the ${o.title.lowercase()} and strikes a pose.", "$n megigazítja: ${o.title.lowercase()}, és pózol egyet.") }
            else -> listOf(
                "act_cheer" to tr("$n giggles. That tickles!", "$n kuncog. Ez csikiz!"),
                "act_hearts" to tr("$n blushes a little.", "$n kicsit elpirul."),
                "act_stretch" to tr("$n stretches and smiles at you.", "$n nyújtózik egyet, és rád mosolyog."),
                "act_cheer" to tr("$n does a happy little dance.", "$n örömében táncra perdül."),
                "act_hearts" to tr("$n gives you a hug. A small one, but a real one.", "$n megölel. Kicsit, de igazán."),
                "act_cheer" to tr("$n waves, as if you'd been away for ages.", "$n integet, mintha évek óta nem látott volna."),
                "act_stretch" to tr("$n points at the sky: a bird went by.", "$n az égre mutat: elrepült egy madár."),
            ).random()
        }
        pokeAct = act
        caught = line
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
        caught = tr("New in your camp: ${m.adds}! (${m.title})", "Új a táborodban: ${m.adds}! (${m.title})")
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
            if (carries.isEmpty()) { carrying = false; if (caught?.endsWith(tr("carry it over.", "átviszi.")) == true) caught = null }
        }
    }
    // Camp life: clouds and weather move, and the companion gets on with things when left alone.
    // A sheet over the page (the shop, friends): the camp holds still behind it, so the sheet moves smoothly.
    // The camp keeps living behind the sheets (stopping it looked like a freeze).
    val ambient = rememberAmbientClock()
    // Until something has been moved: which small thing hints that things can be picked up.
    // Something light and easy to carry, ideally (not the firewood, which belongs by the fire).
    val hintSpot = if (layout.isNotEmpty() || night) null else placed.entries.filter { (spot, id) ->
        spot in SpotPositions && id != "woodpile" &&
            app.tend.core.Things.byId(id)?.let { app.tend.core.Clearing.movable(it) && it.kind == app.tend.core.SpotKind.SMALL } == true
    }.minByOrNull { (_, id) -> listOf("lantern", "wildflowers", "guitar", "telescope").indexOf(id).let { if (it < 0) 9 else it } }?.key
    val idle = rememberIdleLife(placed, rest, enabled = using == null && drag == null && settling == null && !carrying && carries.isEmpty() && greeted == null && pokeAct == null && !night, layout = local) { onRest(it) }
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
                                caught = tr("Shh\u2026 ${avatar.displayName} is asleep in the ${Things.byId(placedNow.getValue(bed))?.name?.lowercase() ?: "tent"}.", "Pszt… ${avatar.displayName} alszik, itt: ${Things.byId(placedNow.getValue(bed))?.name?.lowercase() ?: "sátor"}.")
                                tick(); return@detectTapGestures
                            }
                        }
                        // A present from a friend: opened, one at a time (acorns fly out; a thing is shown).
                        if (giftsNow.isNotEmpty() && kotlin.math.abs(GiftSpot.x - fx) * size.width < size.height * 0.07f && fy in (GiftSpot.y - 0.1f)..(GiftSpot.y + 0.03f)) {
                            when (val o = onOpenGift()) {
                                is app.tend.data.TendStore.Opened.Acorns -> {
                                    caught = tr("${o.from} sent you ${o.acorns} acorns!", "${o.from} ${o.acorns} makkot küldött neked!")
                                    burst(Offset(GiftSpot.x, GiftSpot.y - 0.05f)); tick(firm = true); Sounds.acorns(context)
                                    giftPop = o.acorns; giftKey++
                                }
                                null -> {}
                                else -> { burst(Offset(GiftSpot.x, GiftSpot.y - 0.05f)); tick(firm = true); Sounds.acorns(context); opened = o }
                            }
                            return@detectTapGestures
                        }
                        // Only a duck that's really there (one fished out a moment ago is gone at once, before the
                        // camp redraws), so a tap on the empty water is just water.
                        val swimming = ducksNow.filter { it.id !in fishedOut }
                        val duck = DuckSpots.take(swimming.size).indexOfFirst { kotlin.math.abs(it.x - fx) * size.width < size.height * 0.07f && kotlin.math.abs(it.y - fy) < 0.07f }
                        if (duck >= 0) {
                            val n = swimming[duck]
                            fishedOut += n.id
                            val total = onCatchDuck(n)
                            caught = tr("${n.name}'s duck from their visit. You have $total.", "${n.name} kacsája a látogatásából. Már $total van.")
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
                        if (paperNow && fx > PAPER_LEFT && fy < PAPER_BOTTOM) { onUnroll(); tick(); caught = tr("All clean again.", "Megint minden tiszta."); return@detectTapGestures }
                        // The bridge leads to friends' camps along the river.
                        if (bridgeNow && fx in 0.06f..0.37f && fy in 0.48f..0.62f) { onOpenRiver(null); return@detectTapGestures } // the bridge or its signpost
                        // A thing in the camp: the companion uses it (the guitar: another strum).
                        val hit = spotAt(Offset(fx, fy), size.width / size.height.toFloat(), pinnedNow, placedNow)
                        val thing = hit?.let { placedNow[it] }
                        if (thing != null && (carrying || carries.isNotEmpty())) { caught = tr("${avatar.displayName} is busy moving things. One moment!", "${avatar.displayName} épp pakol. Egy pillanat!"); return@detectTapGestures }
                        if (thing != null) {
                            tick()
                            when {
                                using?.second == thing && thing == "guitar" -> { strumAt = frame; useStart = frame - ((walk + 1f) * 1e9f).toLong(); Sounds.strum(context) }
                                using?.first == hit -> {}
                                // Busy with something else: that's left off, and over to this one from where they are.
                                using != null -> { queue.clear(); Sounds.stop(); startUse(hit, thing) }
                                // Walk over first, unless already there; the sound starts on arrival.
                                else -> startUse(hit, thing)
                            }
                        // A tap on the grass stops what they're doing; not at night: woken up for something,
                        // they finish it and walk back to bed (stopped, they'd be back in the tent in a blink).
                        } else if (using != null && !nightNow) { using = null; queue.clear(); Sounds.stop() }
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
                        caught = if (carried) tr("${avatar.displayName} will carry it over.", "${avatar.displayName} átviszi.") else null
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
                                caught = tr("The ${thing.name.lowercase()} has its own place.", "Ennek megvan a maga helye: ${thing.name.lowercase()}.")
                                return@detectDragGesturesAfterLongPress
                            }
                            if (using != null) { using = null; queue.clear(); Sounds.stop() }
                            kind = thing.kind
                            others = placedNow.keys.filter { it != hit && it in SpotPositions }.mapNotNull { id ->
                                val k = app.tend.core.Clearing.spots.first { it.id == id }.kind
                                if (k == app.tend.core.SpotKind.FIRE || k == app.tend.core.SpotKind.WATER || k == app.tend.core.SpotKind.ABOVE || k == app.tend.core.SpotKind.AROUND) null
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
                            caught = tr("Drag the ${thing.name.lowercase()} anywhere on the meadow.", "Húzd bárhová a réten: ${thing.name.lowercase()}.")
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
                    avatar, preview?.let { t -> Clearing.spotFor(placed, t, level)?.let { placed + (it to t.id) } } ?: placed, mood, worn = worn, night = night, signpost = signpost, paper = paperLeft, ducks = ducks.count { it.id !in fishedOut }, bridge = bridge,
                    use = use, fireBuild = fireBuild, lastStrum = (frame - strumAt) / 1e9f - walk, visitor = visitor, rest = rest, nightClock = nightClock, appear = appear,
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
            // A present opened: what was in it, popping up over the camp.
            AcornsClaimed(giftPop, giftKey, Modifier.align(Alignment.Center))
            // Trying a thing in the camp: how it would look, and buying it right here.
            preview?.let { t ->
                Row(
                    Modifier.align(Alignment.BottomCenter).padding(12.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xF2FFFFFF)).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("How it would look", "Így nézne ki"), style = MaterialTheme.typography.labelSmall, color = Brand.MossOnPill)
                        Text(t.name, style = MaterialTheme.typography.titleSmall, color = Brand.InkOnPill)
                    }
                    Text(tr("Back", "Vissza"), style = MaterialTheme.typography.labelLarge, color = Brand.MossOnPill,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).tappable { preview = null; shopOpen = true }.padding(horizontal = 10.dp, vertical = 8.dp))
                    if (acorns >= t.price) Text(tr("Get it · ${t.price}", "Megveszem · ${t.price}"), style = MaterialTheme.typography.labelLarge, color = Color.White,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Brand.MossOnPill).tappable {
                            if (onBuy(t.id)) { tick(firm = true); Sounds.chime(context) }
                            preview = null
                        }.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
            opened?.let { o ->
                OpenedGift(o, gifts.size, onTakeOutThing, onNext = {
                    // The next one, right here: a thing in the same window, acorns popping over the camp.
                    when (val n = onOpenGift()) {
                        is app.tend.data.TendStore.Opened.Acorns -> { opened = null; giftPop = n.acorns; giftKey++; Sounds.acorns(context); caught = tr("${n.from} sent you ${n.acorns} acorns!", "${n.from} ${n.acorns} makkot küldött neked!") }
                        null -> opened = null
                        else -> { opened = n; Sounds.acorns(context) }
                    }
                }) { opened = null }
            }
        }
        // On the camp: the way back home (top left) and the acorns (top right).
        Box(
            Modifier.align(Alignment.TopStart).padding(start = 28.dp, top = 12.dp).size(36.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xE6FFFFFF)).tappable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { androidx.compose.material3.Icon(TendIcons.vector("back"), tr("Home", "Főoldal"), tint = Brand.MossOnPill, modifier = Modifier.size(18.dp)) }
        AcornBadge(acorns, Modifier.align(Alignment.TopEnd).padding(12.dp))
        // Until something has been moved: how, right on the camp.
        if (hintSpot != null && drag == null && carries.isEmpty()) Text(
            tr("Tip: hold a thing to move it", "Tipp: tartsd nyomva, és húzd"), style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = Brand.MossOnPill,
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
    // Everything under the camp rises in as the page opens (the camp itself glides over from home).
    Column(Modifier.pageBody(), verticalArrangement = Arrangement.spacedBy(Space.m)) {
    // What the companion is up to, for a familiar camp (the same for the hour). Worked out every time,
    // not only when shown: a remember inside a changing branch can trip up what follows it.
    val campLine = remember(java.time.LocalTime.now().hour) {
        java.time.LocalDateTime.now().let { app.tend.core.Voice.camp(avatar.displayName, it, app.tend.core.LiveWeather.fresh() ?: app.tend.core.Weather.of(it.toLocalDate(), it.hour)) }
    }
    Text(
        when {
            drag != null && caught != null -> caught!!
            using != null -> Uses.line(using!!.second, avatar.displayName)
            gifts.isNotEmpty() -> if (gifts.size == 1) tr("A present from ${gifts[0]} by the fire! Tap it to open.", "Ajándék vár a tűznél, ${gifts[0]} küldte! Koppints rá, és kibontod.")
                else tr("${gifts.size} presents by the fire (from ${gifts.distinct().joinToString(" and ")})! Tap to open them one by one.", "${gifts.size} ajándék vár a tűznél (${gifts.distinct().joinToString(" és ")} küldte)! Koppints, és egyenként kibontod.")
            paper -> tr("Someone toilet-papered your oak! Tap it to unroll.", "Valaki vécépapírral dobálta meg a tölgyfádat! Koppints rá, és letekered.")
            visitor != null -> tr("${visitor.aName.replaceFirstChar { it.uppercase() }} came to the camp. Tap it to say hello.", "${visitor.aName.replaceFirstChar { it.uppercase() }} jött a táborba. Koppints rá, és köszönj neki.")
            ducks.isNotEmpty() -> tr("A rubber duck: someone visited. Tap it to fish it out.", "Egy gumikacsa: valaki itt járt. Koppints rá, és kihalászod.")
            caught != null -> caught!!
            hintSpot != null -> tr("Hold the ${app.tend.core.Things.byId(placed.getValue(hintSpot))?.name?.lowercase()} to pick it up, then drag it anywhere. ${avatar.displayName} carries it over.", "Tartsd nyomva, és húzd bárhová: ${app.tend.core.Things.byId(placed.getValue(hintSpot))?.name?.lowercase()}. ${avatar.displayName} majd átviszi.")
            bridge -> tr("Cross the bridge to visit your friends' camps.", "Menj át a hídon, és látogasd meg a barátaid táborát.")
            // Once the camp is familiar, what the companion is up to; before that, how it works.
            placed.isNotEmpty() -> if (placed.size >= 3) campLine else tr("Tap something: ${avatar.displayName} will use it. Hold it to move it.", "Koppints valamire: ${avatar.displayName} használni fogja. Tartsd nyomva, ha arrébb tennéd.")
            else -> tr("Earn acorns by living; spend them in the shop below.", "A makkot élettel keresed; lent a boltban költheted el.")
        },
        style = MaterialTheme.typography.bodyMedium,
    )

    // An event on (a few days a year): a word about it, and its thing in the shop.
    app.tend.core.CampEvent.on(java.time.LocalDate.now())?.let { event ->
        val left = event.daysLeft(java.time.LocalDate.now())
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brand.Plus.copy(alpha = 0.14f)).tappable { shopTab = 2; shopOpen = true }.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.size(40.dp)) { drawThingIcon(event.thing) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("${event.title.uppercase()} · ${if (left == 1) "LAST DAY" else "$left DAYS LEFT"}", "${event.title.uppercase()} · ${if (left == 1) "UTOLSÓ NAP" else "MÉG $left NAP"}"), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Brand.Coral)
                Text(event.line, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), maxLines = 2)
            }
        }
    }


    // Set up before the town was asked: once, the real weather offered.
    if (askTown) Card {
        Text(tr("REAL WEATHER IN YOUR CAMP", "VALÓDI IDŐJÁRÁS A TÁBORBAN"), style = MaterialTheme.typography.labelSmall)
        Text(tr("Set your town and the camp gets its rain, snow and sunshine.", "Add meg a városod, és a táborban is ott esik, havazik vagy süt a nap."), style = MaterialTheme.typography.bodyMedium)
        TownPicker(null, onTown)
        Text(tr("Not now", "Most nem"), style = MaterialTheme.typography.labelLarge, color = Brand.Muted, modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable(onClick = onTownLater).padding(vertical = 6.dp))
    }

    // Six tiles, each with its own little animation: up top, big, the shop and your things; then friends
    // and challenges; then the collection (guestbook, achievements). Under them, the river.
    run {
        val waiting = app.tend.core.FirstSteps.current(steps).filter { it.claimable }.sumOf { it.step.acorns }
        var claimed by remember { mutableIntStateOf(0) }
        var claimKey by remember { mutableIntStateOf(0) }
        val density = androidx.compose.ui.platform.LocalDensity.current
        val free = with(density) { (pageH - hubTop - Space.xxl.toPx()).toDp() }
        val tileH = 68.dp
        val bigH = 80.dp
        val smallH = 56.dp
        // What's left under the tiles: the river, so the page ends calmly and still doesn't scroll.
        val rest = (free - bigH - tileH - smallH - 20.dp - Space.m * 2 - 6.dp).coerceAtLeast(0.dp)
        // Where they start on the page (not on screen: scrolling doesn't change it).
        Column(Modifier.fillMaxWidth().onGloballyPositioned { hubTop = (it.positionInRoot().y - pageTop).toInt() + scroll.value }, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HubTile("shop", tr("Shop", "Bolt"), tr("$acorns acorns", "$acorns makk"), false, Modifier.weight(1f).height(bigH)) { shopTab = 0; shopOpen = true }
                HubTile("things", tr("Things", "Holmik"), tr("${placed.size + chest.size} things", "${placed.size + chest.size} tárgy"), false, Modifier.weight(1f).height(bigH)) { chestOpen = true }
            }
            Box { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HubTile("friends", tr("Friends", "Barátok"), if (guests.isNotEmpty()) tr("${guests.size} at the fire", "${guests.size} a tűznél") else tr("The fire, together", "Közös tűz"), false, Modifier.weight(1f).height(tileH)) { friendsOpen = true }
                HubTile("challenges", tr("Challenges", "Kihívások"), if (waiting > 0) tr("$waiting acorns waiting", "$waiting makk vár") else tr("Quests and goals", "Küldetések, célok"), waiting > 0, Modifier.weight(1f).height(tileH)) {
                    // Acorns waiting: tapped, they're taken at once (the list opens when there's nothing to take).
                    val ready = app.tend.core.FirstSteps.current(steps).filter { it.claimable }
                    if (ready.isEmpty()) onOpenChallenges() else { ready.forEach { onClaimStep(it.step) }; Sounds.acorns(context); claimed = waiting; claimKey++ }
                }
            }
                AcornsClaimed(claimed, claimKey, Modifier.align(Alignment.CenterEnd).fillMaxWidth(0.5f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HubTile("guestbook", tr("Guestbook", "Vendégkönyv"), "${guestbook.size} / ${app.tend.core.Visitor.entries.size}", false, Modifier.weight(1f).height(smallH), visitor = guestbook.keys.firstOrNull()) { bookOpen = true }
                HubTile("achievements", tr("Achievements", "Eredmények"), totals?.let { t -> "${app.tend.core.Milestone.reached(t).size} / ${app.tend.core.Milestone.entries.size}" } ?: "", false, Modifier.weight(1f).height(smallH)) { milestonesOpen = true }
            }
        }
        if (rest > 24.dp) {
            // A fine line with a leaf in the middle: the tiles above, the quiet drawing below.
            Canvas(Modifier.fillMaxWidth().height(14.dp)) {
                val y = size.height / 2
                val c = Offset(size.width / 2, y)
                val line = Brand.Moss.copy(alpha = 0.28f)
                drawLine(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.Transparent, line), endX = c.x - 14.dp.toPx()), Offset(size.width * 0.08f, y), Offset(c.x - 14.dp.toPx(), y), strokeWidth = 1.dp.toPx())
                drawLine(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(line, Color.Transparent), startX = c.x + 14.dp.toPx(), endX = size.width * 0.92f), Offset(c.x + 14.dp.toPx(), y), Offset(size.width * 0.92f, y), strokeWidth = 1.dp.toPx())
                val r = 6.dp.toPx()
                drawPath(Path().apply { moveTo(c.x - r, y); quadraticTo(c.x, y - r * 0.9f, c.x + r, y); quadraticTo(c.x, y + r * 0.9f, c.x - r, y); close() }, Brand.Moss.copy(alpha = 0.45f))
                drawLine(Brand.Sage, Offset(c.x - r * 0.7f, y), Offset(c.x + r * 0.7f, y), strokeWidth = 0.8.dp.toPx())
            }
            RiverPreview(Modifier.fillMaxWidth().height(rest).clip(RoundedCornerShape(20.dp)).tappable { onOpenRiver(null) })
        }
    }

    }

    // The shop: over the page, all the way up. Bought: it closes, and up to the camp to watch it pop in.
    if (shopOpen) {
        val shopSheet = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        fun closeShopThen(then: () -> Unit) = scope.launch { shopSheet.hide(); shopOpen = false; then() }
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { shopOpen = false }, sheetState = shopSheet, containerColor = Brand.Sage) {
            // Smaller than the other sheets; its tiles come a frame after the sheet, so it slides up smoothly.
            Column(Modifier.sheetBody(0.62f)) {
            var ready by remember { mutableStateOf(false) }
            androidx.compose.runtime.LaunchedEffect(Unit) { androidx.compose.runtime.withFrameNanos { }; ready = true }
            if (ready) Shop(acorns, placed, earnedToday.values.sum() + lastNight, avatar.displayName, plus = plus, onPlus = onPlus, level = level, startFilter = shopStart, startTab = shopTab, outfits = outfits, worn = worn, avatar = avatar, onPreview = { preview = it; shopOpen = false },
                onBuyOutfit = { a -> onBuyOutfit(a).also { if (it) { tick(firm = true); Sounds.chime(context) } } },
                onWearOutfit = { tick(); onToggleAccessory?.invoke(it) }, onBuy = { id ->
                // Bought: a firm tick, a chime, and up to the camp to watch it pop in.
                val spot = Things.byId(id)?.let { Clearing.spotFor(placed, it, level) }
                // Bought: a firm tick and a chime; the shop stays open (it pops into the camp behind it).
                onBuy(id).also { ok -> if (ok) { tick(firm = true); Sounds.chime(context) } }
            }, onSell = onSell, chest = chest, onPutAway = { tick(); onPutAway(it) }, onTakeOut = { id ->
                // Out of the chest: up to the camp to watch it pop in, like a new one.
                onTakeOut(id).also { ok -> if (ok) tick(firm = true) }
            })
            }
        }
    }

    // Friends: over the page, from the bottom.
    if (chestOpen) androidx.compose.material3.ModalBottomSheet(onDismissRequest = { chestOpen = false }, containerColor = Brand.Sage,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.sheetBody(0.62f)) {
            Inventory(avatar, placed, chest, onPutAway = { tick(); onPutAway(it) }, onSell = onSell, onTakeOut = { id ->
                onTakeOut(id).also { ok -> if (ok) { tick(firm = true); chestOpen = false; scope.launch { scroll.animateScrollTo(0) } } }
            }, onTraded = onFriendsChanged,
                wardrobe = {
                    Wardrobe(avatar, worn, chosen, owned, earned, plus, acorns, autoDress, { tick(); onAutoDress(it) },
                        onToggle = { tick(); onToggleAccessory?.invoke(it) }, onTakeOff = { tick(); onTakeOff(it) },
                        onBuy = { a -> onBuyOutfit(a).also { if (it) { tick(firm = true); Sounds.chime(context) } } }, onPlus = onPlus)
                })
        }
    }
    // Achievements: the milestones, and what the companion earns by habits.
    if (milestonesOpen) androidx.compose.material3.ModalBottomSheet(onDismissRequest = { milestonesOpen = false }, containerColor = Brand.Sage, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.sheetBody(0.62f), verticalArrangement = Arrangement.spacedBy(Space.m)) {
            Text(tr("Achievements", "Eredmények"), style = MaterialTheme.typography.headlineSmall)
            totals?.let { MilestonesCard(it) }
            Things(avatar, worn, emptySet(), owned, onToggleAccessory)
        }
    }
    if (bookOpen) androidx.compose.material3.ModalBottomSheet(onDismissRequest = { bookOpen = false }, containerColor = Brand.Sage, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.sheetBody(0.62f)) { Guestbook(guestbook) }
    }

    if (friendsOpen) androidx.compose.material3.ModalBottomSheet(onDismissRequest = { friendsOpen = false }, containerColor = Brand.Sage, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.sheetBody()) {
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
        Text(tr("MILESTONES", "MÉRFÖLDKÖVEK"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text("${reached.size} / ${app.tend.core.Milestone.entries.size}", style = MaterialTheme.typography.titleMedium, color = Brand.Moss)
    }
    app.tend.core.Milestone.entries.forEach { m ->
        val done = m in reached
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Box(Modifier.size(28.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (done) Brand.Moss else Brand.Sage), contentAlignment = Alignment.Center) {
                if (done) androidx.compose.material3.Icon(TendIcons.vector("check"), null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.title, style = MaterialTheme.typography.bodyLarge, color = if (done) Brand.Ink else Brand.Ink)
                Text(if (done) tr("Added ${m.adds}", "Megvan: ${m.adds}") else tr("${m.label(t)} · adds ${m.adds}", "${m.label(t)} · ezt hozza: ${m.adds}"), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
                if (!done) Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(Brand.Sage)) {
                    Box(Modifier.fillMaxWidth(m.progress(t) / m.need.toFloat()).height(4.dp).clip(RoundedCornerShape(50)).background(Brand.Mint))
                }
            }
        }
    }
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
    outfits: Set<app.tend.core.Accessory>, worn: Set<Accessory>, avatar: Avatar, onBuyOutfit: (app.tend.core.Accessory) -> Boolean, onWearOutfit: (app.tend.core.Accessory) -> Unit,
    onBuy: (String) -> Boolean, onSell: (String) -> Unit,
    chest: List<String> = emptyList(), onPutAway: (String) -> Unit = {}, onTakeOut: (String) -> Boolean = { false },
    level: Int = 1,
    /** The filter the shop opens on (from a challenge like "something for the trees"). */
    startFilter: SpotKind? = null,
    /** The tab it opens on: 0 things, 1 outfits, 2 seasonal. */
    startTab: Int = 0,
    /** Shows a thing in the camp before buying it. */
    onPreview: (app.tend.core.Thing) -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val inCamp = placed.values.toSet()
    fun owns(id: String) = id in inCamp || id in chest
    var outfitOpen by remember { mutableStateOf<app.tend.core.Accessory?>(null) }
    var open by remember { mutableStateOf<app.tend.core.Thing?>(null) }
    // "New" on what the shop hasn't shown before (nothing the very first time); seen once the shop closes.
    val store = remember { app.tend.data.TendStore(context) }
    val seenBefore = remember { store.shopSeen ?: Things.all.map { it.id }.toSet().also { store.shopSeen = it } }
    val shownNow = remember { mutableSetOf<String>() }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { store.shopSeen = (store.shopSeen.orEmpty() + shownNow) } }
    var savingFor by remember { mutableStateOf(store.savingFor) }
    // Three tabs: things for the camp, things for the companion to wear, and the season's own.
    var tab by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(startTab) }
    val season = app.tend.core.Season.of(java.time.LocalDate.now())
    var filter by remember { mutableStateOf(startFilter) }
    @Composable
    fun tiles(things: List<app.tend.core.Thing>, background: Color = Brand.Sage, paid: Boolean = false) = things.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
            row.forEach { thing ->
                val owned = owns(thing.id)
                val at = app.tend.core.Perks.unlockLevel(thing)
                ShopTile(thing, owned = owned, affordable = (!paid || plus) && acorns >= thing.price, modifier = Modifier.weight(1f), background = background, inChest = thing.id in chest,
                    lockedUntil = at.takeIf { it > level && !owned }, line = Uses.short(thing.id, name), isNew = !owned && thing.id !in seenBefore, saving = thing.id == savingFor) {
                    if (!paid || plus || owned) open = thing else onPlus()
                }
                shownNow += thing.id
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
    @Composable
    fun heading(text: String, coral: Boolean = false, plusMark: Boolean = false) = Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = if (coral) Brand.Coral else Brand.Muted, modifier = Modifier.weight(1f))
        if (plusMark) { Text(tr("Yours to keep ", "Örökre a tiéd "), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp), color = Brand.Muted); PlusMark() }
    }
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("SHOP", "BOLT"), style = MaterialTheme.typography.labelSmall)
                Text(
                    if (earnedToday > 0) tr("+$earnedToday acorns today", "Ma +$earnedToday makk") else tr("An activity earns ${app.tend.core.Acorns.PER_ACTIVITY}, a \"Not now\" ${app.tend.core.Acorns.PER_NOT_NOW}.", "Egy tevékenység ${app.tend.core.Acorns.PER_ACTIVITY}-at ér, egy „Most nem” ${app.tend.core.Acorns.PER_NOT_NOW}-et."),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted,
                )
            }
            AcornBadge(acorns)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
            listOf(tr("Things", "Tárgyak"), tr("Outfits", "Ruhák"), tr("Seasonal", "Szezonális")).forEachIndexed { i, label ->
                Text(
                    label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), textAlign = TextAlign.Center, maxLines = 1,
                    color = if (tab == i) Color.White else Brand.Moss,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (tab == i) Brand.Moss else Brand.Sage).tappable { tab = i }.padding(vertical = 8.dp),
                )
            }
        }
        when (tab) {
            // Things for the camp, by where they go. The home isn't sold: it comes with levels.
            0 -> {
                // One list, cheapest first; a row of filters instead of headings.
                val kinds = listOf(SpotKind.SMALL, SpotKind.FIRE, SpotKind.SEAT, SpotKind.TREES, SpotKind.WATER, SpotKind.ABOVE, SpotKind.AROUND)
                Row(Modifier.padding(top = 8.dp).then(Modifier.horizontalScroll(rememberScrollState())), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (listOf<SpotKind?>(null) + kinds).forEach { k ->
                        val on = filter == k
                        Text(
                            k?.let(::kindChip) ?: tr("All", "Mind"), style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), maxLines = 1,
                            color = if (on) Color.White else Brand.Moss,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) Brand.Moss else Brand.Sage).tappable { filter = k }.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                tiles(kinds.filter { filter == null || it == filter }.flatMap { Things.forKind(it) }.sortedBy { it.price })
            }
            // To wear: this season's outfit and any bought before (tap to put on or take off).
            1 -> {
                val today = java.time.LocalDate.now()
                val wardrobe = (outfits + app.tend.core.Accessory.entries.filter { it.isOutfit && it.forSale(today) }).sortedBy { it.ordinal }
                heading(tr("OUTFITS", "RUHÁK"), plusMark = true)
                wardrobe.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                        row.forEach { a ->
                            val owned = a in outfits
                            OutfitTile(a, owned, worn = a in worn, affordable = plus && acorns >= app.tend.core.Accessory.OUTFIT_PRICE, Modifier.weight(1f)) {
                                // Owned: on or off. Not yet: tried on first.
                                if (owned) onWearOutfit(a) else outfitOpen = a
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Text(tr("The rest $name earns by doing: see Things in the camp.", "A többit $name tettekkel szerzi: lásd a Holmik a táborban."), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted, modifier = Modifier.padding(top = 8.dp))
            }
            // The season's and an event's own, sold only while they last; and those bought before.
            else -> {
                app.tend.core.CampEvent.on(java.time.LocalDate.now())?.let { event ->
                    val left = event.daysLeft(java.time.LocalDate.now())
                    heading(tr("${event.title.uppercase()} · ${if (left == 1) "LAST DAY" else "$left DAYS LEFT"}", "${event.title.uppercase()} · ${if (left == 1) "UTOLSÓ NAP" else "MÉG $left NAP"}"), coral = true, plusMark = true)
                    Text(event.line, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
                    tiles(Things.forEvent(event), Brand.Plus.copy(alpha = 0.14f), paid = true)
                }
                heading(tr("THIS ${season.title.uppercase()} ONLY", "CSAK EBBEN AZ ÉVSZAKBAN: ${season.title.uppercase()}"), coral = true, plusMark = true)
                tiles(Things.forSeason(season), Brand.Plus.copy(alpha = 0.14f), paid = true)
                val nowEvent = app.tend.core.CampEvent.on(java.time.LocalDate.now())
                val earlier = (app.tend.core.Season.entries.filter { it != season }.flatMap { Things.forSeason(it) } +
                    app.tend.core.CampEvent.entries.filter { it != nowEvent }.flatMap { Things.forEvent(it) }).filter { owns(it.id) }
                if (earlier.isNotEmpty()) { heading(tr("FROM OTHER SEASONS AND NIGHTS", "MÁS ÉVSZAKOKBÓL ÉS ESTÉKRŐL")); tiles(earlier) }
            }
        }
    }
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.LaunchedEffect(outfitOpen) {
        if (outfitOpen != null) { view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK); Sounds.tryOn(context) }
    }
    outfitOpen?.let { a ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { outfitOpen = null },
            // Tried on: Bo, big, wearing it (in the place of what's there now).
            icon = { TryOn(avatar, worn, a) },
            title = { Text(a.title) },
            text = { Text(tr("For ${name} to wear, now and every season after. You can take it off any time.", "${name} viselheti, most és minden későbbi évszakban. Bármikor leveheted róla.")) },
            confirmButton = {
                val price = app.tend.core.Accessory.OUTFIT_PRICE
                when {
                    !plus -> androidx.compose.material3.TextButton(onClick = { outfitOpen = null; onPlus() }) { Text(tr("With Plus", "Plusszal"), color = Brand.Moss) }
                    acorns >= price -> androidx.compose.material3.TextButton(onClick = { if (onBuyOutfit(a)) outfitOpen = null }) { Text(tr("Get it for $price", "Megveszem $price makkért"), color = Brand.Moss) }
                    else -> androidx.compose.material3.TextButton(onClick = { outfitOpen = null }) { Text(tr("${price - acorns} more acorns to go", "Még ${price - acorns} makk kell hozzá"), color = Brand.Muted) }
                }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { outfitOpen = null }) { Text(tr("Close", "Bezárás")) } },
            containerColor = Brand.Card,
        )
    }
    open?.let { t ->
        val stored = t.id in chest
        val owned = owns(t.id)
        val swapped = if (owned && !stored) null else Clearing.spotFor(placed, t, level)?.let { placed[it] }?.let(Things::byId)
        val lockedAt = app.tend.core.Perks.unlockLevel(t).takeIf { it > level && !owned }
        val cost = t.price
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { open = null },
            icon = { Canvas(Modifier.size(84.dp)) { drawThingIcon(t.id) } },
            title = { Text(t.name) },
            text = {
                Text(
                    Uses.line(t.id, name).substringBefore(". ").removeSuffix(".") + "." +
                        (swapped?.let { tr(" It takes the ${it.name.lowercase()}'s place; that one goes into your chest.", " Ennek a helyére kerül: ${it.name.lowercase()}; az a ládádba kerül.") } ?: "") +
                        when {
                            stored -> tr(" It's in your chest.", " A ládádban van.")
                            owned -> tr(" Put it away to make room; it waits in your chest.", " Ha helyet csinálnál, tedd el: a ládádban vár.")
                            else -> ""
                        },
                )
            },
            confirmButton = {
                when {
                    stored -> androidx.compose.material3.TextButton(onClick = { if (onTakeOut(t.id)) open = null }) { Text(tr("Put it out", "Kirakom"), color = Brand.Moss) }
                    owned -> androidx.compose.material3.TextButton(onClick = { onPutAway(t.id); open = null }) { Text(tr("Put away", "Elteszem"), color = Brand.Moss) }
                    lockedAt != null -> androidx.compose.material3.TextButton(onClick = { open = null }) { Text(tr("Opens at level $lockedAt", "A(z) $lockedAt. szinten nyílik"), color = Brand.Muted) }
                    acorns >= cost -> androidx.compose.material3.TextButton(onClick = { if (onBuy(t.id)) open = null }) { Text(tr("Get it for ${t.price}", "Megveszem ${t.price} makkért"), color = Brand.Moss) }
                    // Not enough yet: make it the goal (home shows how far), or stop saving for it.
                    savingFor == t.id -> androidx.compose.material3.TextButton(onClick = { store.savingFor = null; savingFor = null }) { Text(tr("Not my goal", "Mégsem ez a célom"), color = Brand.Muted) }
                    else -> androidx.compose.material3.TextButton(onClick = { store.savingFor = t.id; savingFor = t.id; open = null }) { Text(tr("Save up for it (${cost - acorns} to go)", "Erre gyűjtök (még ${cost - acorns})"), color = Brand.Moss) }
                }
            },
            dismissButton = {
                Row {
                    if (owned) androidx.compose.material3.TextButton(onClick = { onSell(t.id); open = null }) { Text(tr("Sell ${Clearing.sellValue(t)}", "Eladom ${Clearing.sellValue(t)}"), color = Brand.Coral) }
                    else {
                        // See it in the camp first, where it would stand.
                        if (lockedAt == null) androidx.compose.material3.TextButton(onClick = { open = null; onPreview(t) }) { Text(tr("See it in camp", "Így nézne ki"), color = Brand.Moss) }
                        androidx.compose.material3.TextButton(onClick = { open = null }) { Text(tr("Close", "Bezárás")) }
                    }
                }
            },
            containerColor = Brand.Card,
        )
    }
}

/**
 * Everything that's yours, in one place: what's out in the camp, what's in the chest, and what the
 * companion has to wear. A thing: put away or out, sell, or offer a friend; a thing to wear: on or off.
 */
@Composable
private fun Inventory(
    avatar: Avatar, placed: Map<Int, String>, chest: List<String>, onPutAway: (String) -> Unit, onSell: (String) -> Unit, onTakeOut: (String) -> Boolean, onTraded: () -> Unit,
    /** The companion's tab: the wardrobe. */
    wardrobe: @Composable () -> Unit = {},
) = Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
    var tab by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    var open by remember { mutableStateOf<app.tend.core.Thing?>(null) }
    var trading by remember { mutableStateOf<app.tend.core.Thing?>(null) }
    val signedIn = app.tend.data.Account.user(androidx.compose.ui.platform.LocalContext.current) != null
    Text(tr("Things", "Holmik"), style = MaterialTheme.typography.headlineSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(tr("In camp", "Táborban"), tr("Chest (${chest.size})", "Láda (${chest.size})"), avatar.displayName).forEachIndexed { i, label ->
            Text(
                label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), textAlign = TextAlign.Center, maxLines = 1,
                color = if (tab == i) Color.White else Brand.Moss,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (tab == i) Brand.Moss else Brand.Card).tappable { tab = i }.padding(vertical = 8.dp),
            )
        }
    }
    @Composable
    fun grid(ids: List<String>, inChest: Boolean) = ids.mapNotNull(Things::byId).chunked(4).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { t -> ShopTile(t, owned = true, affordable = true, Modifier.weight(1f), background = Brand.Card, inChest = inChest) { open = t } }
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
    when (tab) {
        0 -> grid(placed.values.filterNot(app.tend.core.Home::isHome), false)
        1 -> if (chest.isEmpty()) Text(tr("Empty. Put things away here to make room in the camp; presents from friends come here once you open them by the fire.", "Üres. Ide teheted el, amit most nem szeretnél kint látni; a barátaid ajándékai is ide kerülnek, ha kibontod őket a tűz mellett."), style = MaterialTheme.typography.bodyMedium)
            else grid(chest, true)
        else -> wardrobe()
    }
    open?.let { t ->
        val stored = t.id in chest
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { open = null },
            icon = { Canvas(Modifier.size(72.dp)) { drawThingIcon(t.id) } },
            title = { Text(t.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (stored) tr("In your chest.", "A ládádban van.") else tr("Out in the camp.", "Kint van a táborban."), style = MaterialTheme.typography.bodyMedium)
                    if (signedIn) Text(
                        tr("Give it to a friend", "Ajándékba adom egy barátnak"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable { trading = t; open = null }.padding(vertical = 6.dp),
                    )
                }
            },
            confirmButton = {
                if (stored) androidx.compose.material3.TextButton(onClick = { if (onTakeOut(t.id)) open = null }) { Text(tr("Put it out", "Kirakom"), color = Brand.Moss) }
                else androidx.compose.material3.TextButton(onClick = { onPutAway(t.id); open = null }) { Text(tr("Put away", "Elteszem"), color = Brand.Moss) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { onSell(t.id); open = null }) { Text(tr("Sell ${Clearing.sellValue(t)}", "Eladom ${Clearing.sellValue(t)}"), color = Brand.Coral) } },
            containerColor = Brand.Card,
        )
    }
    trading?.let { t -> GiftDialog(null, thing = t, onGave = onTraded) { trading = null } }
}

/** One thing in the shop: its picture, name and price (a tick when it's in the camp). */
@Composable
internal fun ShopTile(
    thing: app.tend.core.Thing, owned: Boolean, affordable: Boolean, modifier: Modifier, background: Color = Brand.Sage, inChest: Boolean = false, lockedUntil: Int? = null,
    /** In the shop: what it's for, in a line, and "New" until the shop has been seen with it. */
    line: String? = null, isNew: Boolean = false, saving: Boolean = false,
    onTap: () -> Unit,
) = Box(modifier) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (owned) Brand.Picked else background).tappable(onClick = onTap).padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { ShopTileBody(thing, owned, affordable, inChest, lockedUntil, line) }
    if (isNew || saving) Text(
        if (saving) tr("Goal", "Cél") else tr("New", "Új"), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.White,
        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).clip(RoundedCornerShape(50)).background(if (saving) Brand.Moss else Brand.Plus).padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

@Composable
private fun ShopTileBody(thing: app.tend.core.Thing, owned: Boolean, affordable: Boolean, inChest: Boolean, lockedUntil: Int?, line: String?) {
    Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Image(thingIconBitmap(thing.id), null, Modifier.size(34.dp).graphicsLayer { alpha = if (lockedUntil != null) 0.4f else 1f })
    }
    // The whole name, on two lines if it needs them.
    Text(thing.name, style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, lineHeight = 13.sp), maxLines = 2, minLines = if (line != null) 2 else 1, textAlign = TextAlign.Center)
    line?.let { Text(it, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp, lineHeight = 12.sp), color = Brand.Muted, maxLines = 2, minLines = 2, textAlign = TextAlign.Center, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
    Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (lockedUntil != null) Text(tr("Level $lockedUntil", "$lockedUntil. szint"), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = Brand.Muted)
        else if (owned) Text(if (inChest) tr("In chest", "A ládában") else tr("In camp", "A táborban"), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = Brand.Moss)
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

/** A short name for the shop's filter. */
private fun kindChip(kind: SpotKind) = when (kind) {
    SpotKind.SMALL -> tr("Little things", "Apróságok")
    SpotKind.FIRE -> tr("Fire", "Tűz")
    SpotKind.SEAT -> tr("Seats", "Ülőhely")
    SpotKind.TREES -> tr("Trees", "Fákra")
    SpotKind.WATER -> tr("River", "Vízre")
    SpotKind.ABOVE -> tr("Overhead", "Fölé")
    SpotKind.SHELTER -> tr("Homes", "Otthon")
    SpotKind.AROUND -> tr("Around the camp", "Tábor körül")
}

private fun kindTitle(kind: SpotKind) = when (kind) {
    SpotKind.FIRE -> tr("The fire pit", "A tűzrakóhely")
    SpotKind.SEAT -> tr("By the fire", "A tűz mellé")
    SpotKind.SHELTER -> tr("A place to sleep", "Egy hely az alváshoz")
    SpotKind.TREES -> tr("In the trees", "A fák közé")
    SpotKind.WATER -> tr("On the river", "A folyóra")
    SpotKind.ABOVE -> tr("Between the trees", "A fák közé kifeszítve")
    SpotKind.AROUND -> tr("Around the camp", "A tábor köré")
    SpotKind.SMALL -> tr("Around the camp", "A tábor köré")
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
            app.tend.core.Rarity.COMMON -> tr("You found ${v.aName}!", "Megtaláltad: ${v.aName}!")
            else -> tr("You found ${if (v.rarity == app.tend.core.Rarity.RARE) "a rare" else "an uncommon"} ${v.title.lowercase()}!", "Találtál egy ${if (v.rarity == app.tend.core.Rarity.RARE) "ritka" else "ritkább"} állatot: ${v.title.lowercase()}!")
        },
        style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp),
    )
    Text(
        (if (first) tr("New in your guestbook! ", "Új a vendégkönyvedben! ") else "") + tr("${book.size} of ${app.tend.core.Visitor.entries.size} met", "${book.size} / ${app.tend.core.Visitor.entries.size} állattal találkoztál"),
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = Brand.Muted, textAlign = TextAlign.Center,
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(50)).background(Brand.Moss).padding(horizontal = 12.dp, vertical = 5.dp)) {
        Text("+${v.rarity.acorns}", style = MaterialTheme.typography.labelLarge, color = Color.White)
        Spacer(Modifier.width(4.dp))
        AcornIcon(14.dp)
    }
}

/** One of the camp's four tiles: a little animated picture, a name and what's there; filled when something's waiting. */
@Composable
private fun HubTile(icon: String, title: String, value: String, highlight: Boolean, modifier: Modifier, visitor: app.tend.core.Visitor? = null, onOpen: () -> Unit) = Row(
    modifier.clip(RoundedCornerShape(20.dp)).background(if (highlight) Brand.Moss else Brand.Card).tappable(onClick = onOpen).padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    HubIcon(icon, visitor, Modifier.size(40.dp))
    Spacer(Modifier.width(10.dp))
    Column(Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), color = if (highlight) Color.White else Brand.Ink, maxLines = 1)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = if (highlight) Color.White.copy(alpha = 0.85f) else Brand.Moss, maxLines = 1)
    }
}

/** The tiles' pictures, each gently alive: a star turning and twinkling, an animal bobbing, a box whose lid hops, a flag waving. */
@Composable
private fun HubIcon(kind: String, visitor: app.tend.core.Visitor?, modifier: Modifier) {
    val loop = androidx.compose.animation.core.rememberInfiniteTransition(label = "hub")
    val t by loop.animateFloat(0f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(2400, easing = androidx.compose.animation.core.LinearEasing)), label = "t")
    val a = t * 2f * Math.PI.toFloat()
    Canvas(modifier) {
        val w = size.width
        val c = Offset(w / 2, w / 2)
        when (kind) {
            "challenges" -> {
                rotate(kotlin.math.sin(a) * 10f, c) {
                    val r = w * (0.36f + 0.03f * kotlin.math.sin(a * 2))
                    drawPath(Path().apply {
                        for (i in 0 until 10) {
                            val ang = -Math.PI.toFloat() / 2 + i * Math.PI.toFloat() / 5
                            val rr = if (i % 2 == 0) r else r * 0.45f
                            val p = c + Offset(kotlin.math.cos(ang) * rr, kotlin.math.sin(ang) * rr)
                            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                        }
                        close()
                    }, Color(0xFFF2C94C))
                }
                // Two sparkles blinking in turn.
                for ((k, o) in listOf(0f to Offset(w * 0.86f, w * 0.18f), 0.5f to Offset(w * 0.14f, w * 0.8f))) {
                    val v = kotlin.math.sin(((t + k) % 1f) * Math.PI.toFloat())
                    drawCircle(Color.White.copy(alpha = v), w * 0.05f * v, o)
                    drawCircle(Color(0xFFF2C94C).copy(alpha = v), w * 0.03f * v, o)
                }
            }
            "guestbook" -> translate(0f, -kotlin.math.abs(kotlin.math.sin(a)) * w * 0.08f) {
                drawVisitorIcon(visitor ?: app.tend.core.Visitor.RABBIT, seen = visitor != null)
            }
            "shop" -> {
                // A little stall: a striped awning that sways, an acorn bobbing on the counter.
                val sway = kotlin.math.sin(a) * w * 0.02f
                drawRect(Color(0xFFB98A5E), Offset(w * 0.2f, w * 0.5f), Size(w * 0.6f, w * 0.38f))
                drawRect(Color(0xFF8E6440), Offset(w * 0.16f, w * 0.48f), Size(w * 0.68f, w * 0.07f))
                for (i in 0 until 4) drawRect(if (i % 2 == 0) Color(0xFFE07A5F) else Color.White, Offset(w * (0.14f + 0.18f * i) + sway, w * 0.22f), Size(w * 0.18f, w * 0.2f))
                drawCircle(Color(0xFF9C6B3F), w * 0.07f, Offset(w * 0.5f, w * 0.4f - kotlin.math.abs(kotlin.math.sin(a * 2)) * w * 0.06f))
            }
            "friends" -> {
                // A small fire, flickering.
                drawLine(Color(0xFF6B4A33), Offset(w * 0.24f, w * 0.86f), Offset(w * 0.76f, w * 0.74f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                drawLine(Color(0xFF8E6440), Offset(w * 0.24f, w * 0.74f), Offset(w * 0.76f, w * 0.86f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
                val f = 1f + 0.08f * kotlin.math.sin(a * 3)
                drawPath(Path().apply { moveTo(w * 0.5f, w * (0.78f - 0.6f * f)); cubicTo(w * 0.78f, w * 0.42f, w * 0.7f, w * 0.78f, w * 0.5f, w * 0.78f); cubicTo(w * 0.3f, w * 0.78f, w * 0.22f, w * 0.42f, w * 0.5f, w * (0.78f - 0.6f * f)); close() }, Color(0xFFF2994A))
                drawPath(Path().apply { moveTo(w * 0.5f, w * (0.78f - 0.34f * f)); cubicTo(w * 0.64f, w * 0.56f, w * 0.6f, w * 0.78f, w * 0.5f, w * 0.78f); cubicTo(w * 0.4f, w * 0.78f, w * 0.36f, w * 0.56f, w * 0.5f, w * (0.78f - 0.34f * f)); close() }, Color(0xFFF2C94C))
            }
            "things" -> {
                // A present: the lid hops now and then, a sparkle escapes.
                val hop = kotlin.math.max(0f, kotlin.math.sin(a * 2)) * w * 0.1f
                drawRoundRect(Color(0xFFE07A5F), Offset(w * 0.18f, w * 0.42f), Size(w * 0.64f, w * 0.46f), CornerRadius(w * 0.05f))
                drawRect(Color(0xFFF2C94C), Offset(w * 0.45f, w * 0.42f), Size(w * 0.1f, w * 0.46f))
                drawRoundRect(Color(0xFFC9634A), Offset(w * 0.12f, w * 0.3f - hop), Size(w * 0.76f, w * 0.14f), CornerRadius(w * 0.04f))
                drawRect(Color(0xFFF2C94C), Offset(w * 0.45f, w * 0.3f - hop), Size(w * 0.1f, w * 0.14f))
                if (hop > w * 0.04f) drawCircle(Color(0xFFF2C94C), w * 0.04f, Offset(w * 0.5f, w * 0.22f - hop))
            }
            else -> {
                // A flag waving on its pole.
                drawLine(Color(0xFF6B4A33), Offset(w * 0.25f, w * 0.9f), Offset(w * 0.25f, w * 0.1f), strokeWidth = w * 0.06f, cap = StrokeCap.Round)
                drawPath(Path().apply {
                    moveTo(w * 0.27f, w * 0.14f)
                    for (i in 1..10) { val x = w * (0.27f + 0.06f * i); lineTo(x, w * 0.14f + kotlin.math.sin(a + i * 0.6f) * w * 0.04f) }
                    for (i in 10 downTo 0) { val x = w * (0.27f + 0.06f * i); lineTo(x, w * 0.5f + kotlin.math.sin(a + i * 0.6f) * w * 0.04f) }
                    close()
                }, Color(0xFF316849))
                drawCircle(Color(0xFFF2C94C), w * 0.06f, Offset(w * 0.25f, w * 0.08f))
            }
        }
    }
}

/** The animals that came to the camp: seen ones in colour, the rest as shadows, four to a row. */
@Composable
private fun Guestbook(book: Map<app.tend.core.Visitor, Int>) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("GUESTBOOK", "VENDÉGKÖNYV"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
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
                        Text(if (n > 0) v.title else "?", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        Text(v.rarity.label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp), color = if (v.rare) Color(0xFFB9783F) else Brand.Muted)
                    }
                }
            }
        }
        Text(tr("Someone comes most days; tap them to say hello. Rare ones only after a good day.", "A legtöbb nap jön valaki; koppints rá, és köszönj neki. A ritkák csak egy jó nap után jönnek."), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
    }
}

/** One of the seasons' outfits in the shop: its price, or (bought) whether it's on. */
@Composable
private fun OutfitTile(a: app.tend.core.Accessory, owned: Boolean, worn: Boolean, affordable: Boolean, modifier: Modifier, onTap: () -> Unit) = Column(
    modifier.clip(RoundedCornerShape(14.dp)).background(if (worn) Brand.Picked else Brand.Plus.copy(alpha = 0.14f)).tappable(onClick = onTap).padding(vertical = 6.dp, horizontal = 2.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    val icon = remember(a) { CharacterPainter.accessoryIcon(a, 144).asImageBitmap() }
    Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) { androidx.compose.foundation.Image(icon, null, Modifier.size(34.dp)) }
    Text(a.title, style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        when {
            worn -> Text(tr("Wearing", "Rajta van"), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = Brand.Moss)
            owned -> Text(tr("Put on", "Felveszi"), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = Brand.Muted)
            else -> {
                Text("${app.tend.core.Accessory.OUTFIT_PRICE}", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = if (affordable) Brand.Moss else Brand.Muted)
                Spacer(Modifier.width(3.dp))
                AcornIcon(12.dp)
            }
        }
    }
}

/** Each thing's shop picture, drawn once and kept: scrolling and opening the shop only show them. */
private val thingIcons = HashMap<String, androidx.compose.ui.graphics.ImageBitmap>()

private fun thingIconBitmap(id: String): androidx.compose.ui.graphics.ImageBitmap = thingIcons.getOrPut(id) {
    val px = 120
    androidx.compose.ui.graphics.ImageBitmap(px, px).also { img ->
        androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
            androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(img), Size(px.toFloat(), px.toFloat()),
        ) { drawThingIcon(id) }
    }
}

/** The page's end: the river winding past little camps downstream, and "Along the river". Tapped: the river. */
@Composable
private fun RiverPreview(modifier: Modifier) = Box(modifier) {
    Canvas(Modifier.matchParentSize()) {
        val w = size.width
        val h = size.height
        drawRect(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Brand.Mint.copy(alpha = 0.18f), Brand.Mint.copy(alpha = 0.32f))))
        // The water: wide in front, narrowing off to the right.
        drawPath(Path().apply {
            moveTo(0f, h * 0.72f)
            cubicTo(w * 0.3f, h * 0.58f, w * 0.55f, h * 0.78f, w, h * 0.5f)
            lineTo(w, h * 0.6f)
            cubicTo(w * 0.6f, h * 0.92f, w * 0.3f, h * 0.76f, 0f, h)
            close()
        }, Color(0xFF8EC4E8).copy(alpha = 0.7f))
        for ((x, y) in listOf(0.12f to 0.82f, 0.4f to 0.74f, 0.7f to 0.66f)) drawLine(Color.White.copy(alpha = 0.7f), Offset(w * x, h * y), Offset(w * (x + 0.06f), h * (y - 0.01f)), strokeWidth = h * 0.012f, cap = StrokeCap.Round)
        // Little camps along the bank, smaller the farther they are.
        for ((x, k) in listOf(0.22f to 1f, 0.52f to 0.8f, 0.8f to 0.6f)) {
            val base = Offset(w * x, h * (0.56f - 0.12f * (1f - k)))
            val t = h * 0.16f * k
            drawPath(Path().apply { moveTo(base.x - t, base.y); lineTo(base.x, base.y - t * 1.1f); lineTo(base.x + t, base.y); close() }, Brand.Moss.copy(alpha = 0.55f))
            drawCircle(Color(0xFFF2994A).copy(alpha = 0.85f), t * 0.18f, base + Offset(t * 1.5f, -t * 0.12f))
        }
    }
    Text(
        tr("Along the river →", "A folyó mentén →"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss,
        modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
    )
}

/** What was in a present: a thing (into the camp or left in the chest), acorns for one you had, or one to update for. */
@Composable
private fun OpenedGift(o: app.tend.data.TendStore.Opened, left: Int, onTakeOut: (String) -> Boolean, onNext: () -> Unit, onClose: () -> Unit) {
    var note by remember(o) { mutableStateOf<String?>(null) }
    // One that can't be opened yet stays by the fire: not counted as another.
    val others = if (o is app.tend.data.TendStore.Opened.Unknown) left - 1 else left
    val more = if (others > 0) tr(" There's another one by the fire.", " A tűznél még vár egy.") else ""
    val thing = when (o) { is app.tend.data.TendStore.Opened.Thing -> o.thing; is app.tend.data.TendStore.Opened.Had -> o.thing; else -> null }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        icon = { if (thing != null) Canvas(Modifier.size(72.dp)) { drawThingIcon(thing.id) } else TendIcons.Glyph("gift", 40.dp) },
        title = { Text(tr("A present from ${o.from}", "Ajándék tőle: ${o.from}")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    when (o) {
                        is app.tend.data.TendStore.Opened.Thing -> tr("${o.thing.name}! It's in your chest now: put it out, or keep it for later.", "${o.thing.name}! Most a ládádban van: kirakhatod, vagy elteheted későbbre.")
                        is app.tend.data.TendStore.Opened.Had -> tr("${o.thing.name}, but you already have one, so it's ${o.acorns} acorns instead.", "${o.thing.name}, de ilyened már van, ezért helyette ${o.acorns} makkot kaptál.")
                        is app.tend.data.TendStore.Opened.Unknown -> tr("Something new that only the latest tend knows. Update the app, then open it.", "Valami új, amit csak a tend legújabb verziója ismer. Frissítsd az appot, és utána kibonthatod.")
                        is app.tend.data.TendStore.Opened.Acorns -> tr("${o.acorns} acorns.", "${o.acorns} makk.")
                    } + more,
                    style = MaterialTheme.typography.bodyMedium,
                )
                note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Brand.Coral) }
            }
        },
        confirmButton = {
            Row {
                // A thing: out into the camp, or left in the chest; then (or otherwise) the next one, if there is one.
                if (o is app.tend.data.TendStore.Opened.Thing && note == null) {
                    androidx.compose.material3.TextButton(onClick = { if (onTakeOut(o.thing.id)) { if (others > 0) onNext() else onClose() } else note = tr("No room for it in the camp right now: it stays in the chest.", "Most nincs neki hely a táborban: a ládában marad.") }) {
                        Text(tr("Put it out", "Kirakom"), color = Brand.Moss)
                    }
                } else if (others > 0) androidx.compose.material3.TextButton(onClick = onNext) { Text(tr("Open the next", "Kibontom a következőt"), color = Brand.Moss) }
                else androidx.compose.material3.TextButton(onClick = onClose) { Text(tr("OK", "Rendben"), color = Brand.Moss) }
            }
        },
        dismissButton = {
            if (o is app.tend.data.TendStore.Opened.Thing && note == null) androidx.compose.material3.TextButton(onClick = { if (others > 0) onNext() else onClose() }) {
                Text(tr("Keep it in the chest", "Ládában hagyom"), color = Brand.Muted)
            } else if (others > 0) androidx.compose.material3.TextButton(onClick = onClose) { Text(tr("Later", "Később"), color = Brand.Muted) }
        },
        containerColor = Brand.Card,
    )
}

/** Bo wearing [a] in its place, instead of what's there: for trying things on. */
@Composable
private fun TryOn(avatar: Avatar, worn: Set<Accessory>, a: Accessory?, size: androidx.compose.ui.unit.Dp = 120.dp) {
    val shown = if (a == null) worn else worn.filter { it.slot != a.slot }.toSet() + a
    androidx.compose.runtime.CompositionLocalProvider(LocalAccessories provides shown) {
        CharacterView(avatar, app.tend.core.Mood.HAPPY, size)
    }
}

/**
 * Bo's wardrobe: Bo, big, and the four places (head, eyes, neck, extra), each with what's on it or
 * nothing. A place opens to everything for it: what's yours goes on with a tap, what isn't yet is
 * tried on and says how to get it. Things whose habit holds right now shine. And: dressing by itself.
 */
@Composable
internal fun Wardrobe(
    avatar: Avatar, worn: Set<Accessory>, chosen: Map<app.tend.core.Slot, Accessory>, owned: Set<Accessory>, active: Set<Accessory>,
    plus: Boolean, acorns: Int, autoDress: Boolean, onAutoDress: (Boolean) -> Unit,
    onToggle: (Accessory) -> Unit, onTakeOff: (app.tend.core.Slot) -> Unit, onBuy: (Accessory) -> Boolean, onPlus: () -> Unit,
) = Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    var place by remember { mutableStateOf<app.tend.core.Slot?>(null) }
    var trying by remember { mutableStateOf<Accessory?>(null) }
    // Trying something on has its own feel: a soft click and two notes up.
    val view = androidx.compose.ui.platform.LocalView.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(trying) {
        if (trying != null) { view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK); Sounds.tryOn(ctx) }
    }
    val season = app.tend.core.Season.of(java.time.LocalDate.now())
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TryOn(avatar, worn, trying, 132.dp) }
    trying?.let { a ->
        // Tried on: how to get it, and the button when it's for sale now.
        val forSale = a.isOutfit && a.forSale(java.time.LocalDate.now())
        val price = Accessory.OUTFIT_PRICE
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brand.Card).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(a.title, style = MaterialTheme.typography.labelLarge)
                Text(if (forSale) tr("In the shop, $price acorns", "A boltban, $price makk") else a.how, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp))
            }
            if (forSale) Text(
                when { !plus -> tr("With Plus", "Plusszal"); acorns >= price -> tr("Get it", "Megveszem"); else -> tr("${price - acorns} to go", "még ${price - acorns}") },
                style = MaterialTheme.typography.labelLarge, color = if (!plus || acorns >= price) Brand.Moss else Brand.Muted,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable {
                    when { !plus -> onPlus(); acorns >= price -> if (onBuy(a)) trying = null }
                }.padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
    app.tend.core.Slot.entries.forEach { slot ->
        val on = chosen[slot]
        val open = place == slot
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (open) Brand.Picked else Brand.Card)
                .tappable { place = if (open) null else slot; trying = null }.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(slot.title.uppercase(), style = MaterialTheme.typography.labelSmall, color = Brand.Muted, modifier = Modifier.width(64.dp))
            if (on != null) Box(Modifier.size(30.dp).clip(CircleShape).background(IconChip), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Image(remember(on) { CharacterPainter.accessoryIcon(on, 96).asImageBitmap() }, null, Modifier.size(26.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(on?.title ?: tr("Nothing", "Semmi"), style = MaterialTheme.typography.bodyLarge, color = if (on != null) Brand.Ink else Brand.Muted, modifier = Modifier.weight(1f))
            Text(if (open) tr("Close", "Bezár") else tr("Change", "Csere"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
        }
        if (open) {
            // Yours first, then the rest (faded, with how to get them).
            val all = Accessory.entries.filter { it.slot == slot }.sortedBy { if (it in owned) 0 else 1 }
            androidx.compose.foundation.layout.FlowRow(maxItemsInEachRow = 4, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WardrobeTile(null, picked = on == null, mine = true, shines = false, Modifier.weight(1f)) { onTakeOff(slot); trying = null }
                all.forEach { a ->
                    val mine = a in owned
                    WardrobeTile(a, picked = a == on, mine = mine, shines = mine && a in active, Modifier.weight(1f)) {
                        if (mine) { trying = null; if (a != on) onToggle(a) } else trying = a
                    }
                }
                repeat((4 - (all.size + 1) % 4) % 4) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brand.Card).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(tr("${avatar.displayName} dresses itself", "${avatar.displayName} öltözzön magától"), style = MaterialTheme.typography.bodyLarge)
            Text(tr("From its own things, by the weather and the season. At night: the nightcap.", "A saját holmijából, időjárás és évszak szerint. Éjjel hálósapkában."), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp))
        }
        androidx.compose.material3.Switch(autoDress, onAutoDress, colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = Brand.Moss))
    }
}

/** One thing in a wardrobe place (null: nothing): its picture and name; faded when not yours yet, a gold ring when its habit holds. */
@Composable
private fun WardrobeTile(a: Accessory?, picked: Boolean, mine: Boolean, shines: Boolean, modifier: Modifier, onTap: () -> Unit) = Column(
    modifier.clip(RoundedCornerShape(14.dp)).background(if (picked) Brand.Picked else Brand.Card)
        .then(if (shines) Modifier.border(2.dp, Brand.Sunny, RoundedCornerShape(14.dp)) else Modifier)
        .tappable(onClick = onTap).padding(vertical = 6.dp, horizontal = 2.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Box(Modifier.size(38.dp).clip(CircleShape).background(if (a != null) IconChip else Color.Transparent), contentAlignment = Alignment.Center) {
        if (a != null) androidx.compose.foundation.Image(remember(a) { CharacterPainter.accessoryIcon(a, 120).asImageBitmap() }, null, Modifier.size(32.dp).graphicsLayer { alpha = if (mine) 1f else 0.35f })
        else Text("–", style = MaterialTheme.typography.titleLarge, color = Brand.Muted)
    }
    Text(a?.title ?: tr("Nothing", "Semmi"), style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp), maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, color = if (mine) Brand.Ink else Brand.Muted)
    Text(
        when { picked -> tr("Wearing", "Rajta van"); a == null -> tr("Take off", "Leveszi"); mine -> tr("Put on", "Felveszi"); a.isOutfit -> tr("Shop", "Bolt"); else -> a.short },
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        color = if (picked) Brand.Moss else Brand.Muted,
    )
}

/** Behind a thing's picture: light in both themes, so the ink-drawn glasses stay visible. */
private val IconChip: Color @Composable get() = if (Brand.dark) Color(0xFFDCE2DD) else Color.Transparent
