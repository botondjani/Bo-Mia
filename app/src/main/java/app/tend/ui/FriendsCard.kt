package app.tend.ui

import app.tend.core.tr

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tend.core.Avatar
import app.tend.data.Account
import app.tend.data.Friends
import kotlinx.coroutines.launch

/**
 * Friends on the clearing page: who they are (companion, name, level), inviting someone (link,
 * QR code, 6-letter code), typing a friend's code, and accepting an invite that was opened.
 * Without an account it says what an account gives and links to Settings.
 */
@Composable
fun FriendsCard(onOpenSettings: () -> Unit, onOpenRiver: (friendId: String?) -> Unit = {}, onChanged: () -> Unit = {}, openInvite: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val signedIn = Account.user(context) != null
    var friends by remember { mutableStateOf<List<Friends.Friend>?>(null) }
    var offline by remember { mutableStateOf(false) }
    var inviting by rememberSaveable { mutableStateOf(openInvite && signedIn) }
    LaunchedEffect(openInvite) { if (openInvite && signedIn) inviting = true }
    var typing by rememberSaveable { mutableStateOf(false) }
    // A present: from the grid (pick who), or from a friend's row (for them).
    var giving by rememberSaveable { mutableStateOf(false) }
    var givingTo by remember { mutableStateOf<Friends.Friend?>(null) }
    var asking by remember { mutableStateOf<Pair<String, Friends.Friend>?>(null) }
    var removing by remember { mutableStateOf<Friends.Friend?>(null) }
    var blocking by remember { mutableStateOf<Friends.Friend?>(null) }
    var buddyAbout by remember { mutableStateOf<Friends.Friend?>(null) }
    var note by rememberSaveable { mutableStateOf<String?>(null) }
    // Cheered today (the server allows one a day per friend): kept here so the button says so at once.
    var cheered by remember { mutableStateOf(setOf<String>()) }
    // Called to the fire in the last hour.
    var called by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(Unit) {
        Friends.sent(context)?.let { s ->
            called = s.used.filterValues { Friends.Kind.FIRE in it }.keys
            cheered = s.used.filterValues { Friends.Kind.CHEER in it }.keys
        }
    }

    var claimed by remember { mutableStateOf(false) }
    suspend fun reload() {
        val list = Friends.list(context)
        offline = list == null
        if (list != null) friends = list
        // A new friendship: said here too, with its acorns.
        Friends.takeNewFriends(context)?.let { note = it; onChanged() }
        // The together goal reached: acorns (and the bridge, the first time).
        val me = Friends.me(context)
        if (list != null && Friends.claimWeek(context, Friends.week(me, list), me.week)) { claimed = true; onChanged() }
    }
    /** Looks a code up and asks before adding; says so when it's unknown. */
    suspend fun lookUp(code: String) {
        val owner = Friends.owner(context, code)
        if (owner != null) asking = code to owner else note = tr("That code isn't anyone's (or it's yours). Check it and try again.", "Ez a kód senkié (vagy a tiéd). Nézd meg, és próbáld újra.")
    }

    // What's new since you last looked (then it's seen): a few lines, not a history.
    val notes = remember { Friends.unseenNotes(context).take(4) }
    LaunchedEffect(signedIn) {
        if (!signedIn) return@LaunchedEffect
        Friends.markSeen(context)
        reload()
        Friends.pendingCode(context)?.let { code -> Friends.setPendingCode(context, null); lookUp(code) }
    }

    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("FRIENDS", "BARÁTOK"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            friends?.takeIf { it.isNotEmpty() }?.let { Text("${it.size}", style = MaterialTheme.typography.labelLarge, color = Brand.Moss) }
        }
        if (!signedIn) {
            Text(
                if (Friends.pendingCode(context) != null) tr("Someone invited you. Log in to accept, and to see each other's camps.", "Valaki meghívott. Lépj be, hogy elfogadhasd, és lássátok egymás táborát.")
                else tr("Add friends to see each other's camps along the river. It needs an account (Settings → Account).", "Vegyél fel barátokat, és lássátok egymás táborát a folyó mentén. Fiók kell hozzá (Beállítások → Fiók)."),
                style = MaterialTheme.typography.bodyMedium,
            )
            ChipButton(tr("Log in", "Belépés"), onOpenSettings)
        } else {
            val list = friends.orEmpty()
            notes.forEach { n -> NoteRow(n) }
            // Asked to be someone's buddy: yes or no, right here.
            list.filter { it.buddyIn == "asked" }.forEach { f ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brand.Sage).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        tr("${f.name} asks you to be their buddy: you'll hear from tend if they turn Strict off or go over their minutes 3 times in a week. Nothing else, and either of you can stop it any time.",
                            "${f.name} a pajtásának kér: szólunk neked, ha kikapcsolja a Szigorút, vagy egy héten háromszor túllépi a keretét. Mást nem, és bármelyikőtök bármikor kiléphet."),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChipButton(tr("Yes", "Vállalom"), { scope.launch { if (Friends.buddyAnswer(context, f.id, true)) { note = tr("You're ${f.name}'s buddy now.", "Mostantól ${f.name} pajtása vagy."); reload() } } })
                        ChipButton(tr("No", "Most nem"), { scope.launch { if (Friends.buddyAnswer(context, f.id, false)) reload() } }, filled = false)
                    }
                }
            }
            if (list.isEmpty()) Text(
                if (offline) tr("Can't reach the server right now.", "Most nem érjük el a szervert.") else tr("No friends yet. Invite someone: when they accept, you'll see each other's camps.", "Még nincs barátod. Hívj meg valakit: ha elfogadja, látjátok egymás táborát."),
                style = MaterialTheme.typography.bodyMedium,
            )
            // Many friends: the ones who did something most recently first, five at a time.
            var showAll by rememberSaveable { mutableStateOf(false) }
            val sorted = list.sortedByDescending { it.lastDoneAt ?: 0L }
            val shown = if (showAll || sorted.size <= FRIENDS_SHOWN) sorted else sorted.take(FRIENDS_SHOWN)
            shown.forEach { f ->
                FriendRow(
                    f, cheered = f.id in cheered, onVisit = { onOpenRiver(f.id) },
                    onGift = { givingTo = f; giving = true },
                    called = f.id in called,
                    onCall = {
                        called = called + f.id
                        scope.launch {
                            if (Friends.send(context, f.id, Friends.Kind.FIRE)) { note = tr("${f.name} hears the call. Start something phone-free and meet at the fire.", "${f.name} hallja a hívást. Kezdj bele valami telefonmentesbe, és találkoztok a tűznél."); app.tend.data.Stats.log(context, "fire_call") }
                            else { called = called - f.id; note = tr("Couldn't call ${f.name} right now (once an hour to each friend).", "Most nem sikerült hívni: ${f.name} (egy barátot óránként egyszer lehet).") }
                        }
                    },
                    onBuddy = { buddyAbout = f },
                    onBlock = { blocking = f },
                    onCheer = {
                        cheered = cheered + f.id
                        scope.launch { if (!Friends.send(context, f.id, Friends.Kind.CHEER)) note = tr("Couldn't send the cheer. Try again in a moment.", "Nem sikerült elküldeni a tapsot. Próbáld újra egy pillanat múlva.") else app.tend.data.Stats.log(context, "cheer") }
                    },
                ) { removing = f }
            }
            if (sorted.size > FRIENDS_SHOWN) Text(
                if (showAll) tr("Show fewer", "Kevesebb") else tr("All ${sorted.size} friends", "Mind a ${sorted.size} barát"),
                style = MaterialTheme.typography.labelLarge, color = Brand.Moss, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).tappable { showAll = !showAll }.padding(vertical = 8.dp),
            )
            if (list.isNotEmpty()) TogetherWeek(Friends.week(Friends.me(context), list), app.tend.data.TendStore(context).togetherDone.size, claimed)
            // What to do, as an even grid: the river and presents once there are friends, then inviting.
            val actions = buildList {
                if (list.isNotEmpty()) add(Triple("wave", tr("Along the river", "A folyó mentén")) { onOpenRiver(null) })
                if (list.isNotEmpty()) add(Triple("gift", tr("Send a present", "Ajándék küldése")) { givingTo = null; giving = true })
                add(Triple("friends", tr("Invite a friend", "Meghívás")) { inviting = true })
                add(Triple("pen", tr("Enter a code", "Kód megadása")) { typing = true })
            }
            Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (icon, label, go) ->
                            // No friends yet: inviting is the one to do, filled.
                            val main = list.isEmpty() && icon == "friends"
                            Row(
                                Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(14.dp)).background(if (main) Brand.Moss else Brand.Sage)
                                    .tappable(onClick = go).padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TendIcons.Glyph(icon, 18.dp, tint = if (main) androidx.compose.ui.graphics.Color.White else Brand.Moss)
                                Spacer(Modifier.width(8.dp))
                                Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = if (main) androidx.compose.ui.graphics.Color.White else Brand.Moss, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Brand.Coral) }
    }

    if (inviting) InviteDialog { inviting = false }
    if (giving) GiftDialog(friends.orEmpty(), to = givingTo, onGave = onChanged) { giving = false }
    if (typing) EnterCodeDialog(onDismiss = { typing = false }) { code -> typing = false; note = null; scope.launch { lookUp(code) } }
    asking?.let { (code, owner) ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { asking = null },
            icon = { Face(owner.companion, 72) },
            title = { Text(tr("Add ${owner.name} as a friend?", "Felveszed barátnak: ${owner.name}?")) },
            text = { Text(tr("You'll see each other's camps, levels, weekly goals and what you did lately (like \"went for a walk\"). Nothing else: not your apps, minutes or health data.", "Látjátok majd egymás táborát, szintjét, heti céljait, és hogy mit csináltatok mostanában (például „sétált egyet”). Mást nem: sem az appjaidat, sem a perceidet, sem az egészségadataidat.")) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    asking = null
                    scope.launch { if (Friends.accept(context, code)) { app.tend.data.TendStore(context).invited = true; reload() } else note = tr("Couldn't add them. Try again in a moment.", "Nem sikerült felvenni. Próbáld újra egy pillanat múlva.") }
                }) { Text(tr("Add", "Hozzáadás"), color = Brand.Moss) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { asking = null }) { Text(tr("Not now", "Most nem")) } },
            containerColor = Brand.Card,
        )
    }
    blocking?.let { f ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { blocking = null },
            title = { Text(tr("Block ${f.name}?", "Letiltod: ${f.name}?")) },
            text = { Text(tr("You stop being friends, and neither of you can add the other again. They won't be told.", "Nem lesztek többé barátok, és egyikőtök sem veheti fel újra a másikat. Ő nem kap róla értesítést.")) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { blocking = null; scope.launch { Friends.block(context, f.id); reload() } }) { Text(tr("Block", "Tiltás"), color = Brand.Coral) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { blocking = null }) { Text(tr("Keep", "Mégsem")) } },
            containerColor = Brand.Card,
        )
    }
    // A buddy: asking, taking it back, or ending it.
    buddyAbout?.let { f ->
        val mine = friends.orEmpty().count { it.buddyOut != null }
        val max = Friends.buddiesMax(context)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { buddyAbout = null },
            icon = { Face(f.companion, 56) },
            title = { Text(if (f.buddyOut == null && f.buddyIn == null) tr("Ask ${f.name} to be your buddy?", "Pajtásnak kéred: ${f.name}?") else tr("${f.name} and you", "${f.name} és te")) },
            text = {
                Text(
                    when {
                        f.buddyOut == "yes" -> tr("${f.name} is your buddy: they hear when you turn Strict off or go over your minutes 3 times in a week.", "${f.name} a pajtásod: szólunk neki, ha kikapcsolod a Szigorút, vagy egy héten háromszor túllépted a keretet.")
                        f.buddyOut == "asked" -> tr("Asked. Waiting for ${f.name} to say yes.", "Elküldve. Várjuk, hogy ${f.name} igent mondjon.")
                        f.buddyIn == "yes" -> tr("You're ${f.name}'s buddy.", "Te vagy ${f.name} pajtása.")
                        mine >= max -> if (max == 1) tr("You have a buddy already. With Plus you can have three.", "Már van pajtásod. Plusszal három is lehet.") else tr("You have three buddies already.", "Már három pajtásod van.")
                        else -> tr("If you agree, ${f.name} hears from tend when you turn Strict off or go over your minutes 3 times in a week. Nothing else: not your apps or minutes. Either of you can stop it any time.", "Ha elfogadja, ${f.name} értesítést kap, ha kikapcsolod a Szigorút, vagy egy héten háromszor túllépted a keretet. Mást nem: sem az appjaidat, sem a perceidet. Bármelyikőtök bármikor kiléphet.")
                    },
                )
            },
            confirmButton = {
                val pact = f.buddyOut != null || f.buddyIn != null
                when {
                    pact -> androidx.compose.material3.TextButton(onClick = { buddyAbout = null; scope.launch { Friends.buddyLeave(context, f.id); reload() } }) {
                        Text(if (f.buddyOut == "asked" && f.buddyIn == null) tr("Take it back", "Visszavonom") else tr("End it", "Kilépek"), color = Brand.Coral)
                    }
                    mine < max -> androidx.compose.material3.TextButton(onClick = {
                        buddyAbout = null
                        scope.launch { note = if (Friends.buddyAsk(context, f.id)) tr("Asked ${f.name}.", "Elküldve: ${f.name}.") else tr("Couldn't ask right now.", "Most nem sikerült elküldeni."); reload() }
                    }) { Text(tr("Ask", "Kérem"), color = Brand.Moss) }
                    else -> {}
                }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { buddyAbout = null }) { Text(tr("Close", "Bezárás"), color = Brand.Muted) } },
            containerColor = Brand.Card,
        )
    }
    removing?.let { f ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(tr("Remove ${f.name}?", "Eltávolítod: ${f.name}?")) },
            text = { Text(tr("You'll stop seeing each other's camps. They won't be told, and your invite code changes so an old link can't add you back.", "Nem látjátok többé egymás táborát. Ő nem kap róla értesítést, a meghívókódod pedig megváltozik, hogy egy régi linkkel ne vehessen fel újra.")) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { removing = null; scope.launch { Friends.remove(context, f.id); reload() } }) { Text(tr("Remove", "Eltávolítás"), color = Brand.Coral) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { removing = null }) { Text(tr("Keep", "Mégsem")) } },
            containerColor = Brand.Card,
        )
    }
}

/** Friends listed before "All N friends". */
private const val FRIENDS_SHOWN = 5

@Composable
private fun FriendRow(f: Friends.Friend, cheered: Boolean, onVisit: () -> Unit, called: Boolean = false, onCall: () -> Unit = {}, onBuddy: () -> Unit = {}, onBlock: () -> Unit = {}, onCheer: () -> Unit, onGift: () -> Unit, onRemove: () -> Unit) = Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).tappable(onClick = onVisit).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
    Face(f.companion, 40)
    Spacer(Modifier.width(10.dp))
    Column(Modifier.weight(1f)) {
        Text(f.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        // What they did last, with when; otherwise their level and camp.
        val done = f.lastDone
        val at = f.lastDoneAt
        val buddy = if (f.buddyOut == "yes" || f.buddyIn == "yes") tr("Buddy · ", "Pajtás · ") else ""
        Text(
            buddy + if (done != null && at != null) "${app.tend.core.Tasks.doneShown(done).replaceFirstChar { it.uppercase() }} · ${android.text.format.DateUtils.getRelativeTimeSpanString(at, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS)}"
            // Level 0: they keep it to themselves.
            else (if (f.level > 0) tr("Level ${f.level} · ", "${f.level}. szint · ") else "") + tr("${f.placed.size} thing${if (f.placed.size == 1) "" else "s"} at camp", "${f.placed.size} tárgy a táborban"),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted, maxLines = 1,
        )
    }
    // One action up front: a cheer for something done today, otherwise a call to the fire.
    val today = f.lastDoneAt?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() } == java.time.LocalDate.now()
    val cheerFirst = f.lastDone != null && today && !cheered
    if (cheerFirst) Text(
        tr("\uD83D\uDC4F Cheer", "👏 Taps"), style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = androidx.compose.ui.graphics.Color.White,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Brand.Moss).tappable(onClick = onCheer).padding(horizontal = 10.dp, vertical = 6.dp),
    ) else Text(
        if (called) tr("Called", "Hívva") else tr("\uD83D\uDD25 Call", "🔥 Hívás"), style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
        color = if (called) Brand.Muted else Brand.Moss,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Brand.Sage)
            .then(if (called) Modifier else Modifier.tappable(onClick = onCall)).padding(horizontal = 10.dp, vertical = 6.dp),
    )
    // The rarer things behind a small "more": a present (one of your things), and removing (it asks first).
    Box {
        var more by remember { mutableStateOf(false) }
        Text("\u22EE", style = MaterialTheme.typography.titleMedium, color = Brand.Muted,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable { more = true }.semantics { contentDescription = tr("More", "Egyebek") }.padding(horizontal = 8.dp, vertical = 6.dp))
        androidx.compose.material3.DropdownMenu(expanded = more, onDismissRequest = { more = false }, containerColor = Brand.Card) {
            if (cheerFirst) androidx.compose.material3.DropdownMenuItem(
                text = { Text(if (called) tr("Called to the fire", "Már hívtad a tűzhöz") else tr("Call to the fire", "Hívás a tűzhöz")) },
                enabled = !called, onClick = { more = false; onCall() },
            )
            androidx.compose.material3.DropdownMenuItem(text = { Text(tr("Send a present", "Ajándék küldése")) }, onClick = { more = false; onGift() })
            androidx.compose.material3.DropdownMenuItem(text = { Text(if (f.buddyOut != null || f.buddyIn == "yes") tr("Buddy", "Pajtás") else tr("Ask to be your buddy", "Pajtásnak kérem")) }, onClick = { more = false; onBuddy() })
            androidx.compose.material3.DropdownMenuItem(text = { Text(tr("Remove ${f.name}", "Eltávolítás: ${f.name}"), color = Brand.Coral) }, onClick = { more = false; onRemove() })
            androidx.compose.material3.DropdownMenuItem(text = { Text(tr("Block ${f.name}", "Tiltás: ${f.name}"), color = Brand.Coral) }, onClick = { more = false; onBlock() })
        }
    }
}

/** "Anna waved at you · 2 hours ago" */
@Composable
private fun NoteRow(n: Friends.Note) = Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
    Face(n.companion, 28)
    Spacer(Modifier.width(10.dp))
    Text(n.kind.received(n.name), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    Text(
        android.text.format.DateUtils.getRelativeTimeSpanString(n.at, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS).toString(),
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted,
    )
}

/** This week's shared goal: a bar with its three steps (bronze, silver, gold), who added how much, and what's next. */
@Composable
internal fun TogetherWeek(week: Friends.Week, weeksDone: Int, justClaimed: Boolean) = Column(
    Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(18.dp)).background(Brand.Sage).padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    val camps = week.parts.size
    val gold = app.tend.core.Together.Tier.GOLD.target(camps)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(tr("TOGETHER THIS WEEK", "EGYÜTT A HÉTEN"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text((week.tier?.let { "${it.title} · " } ?: "") + "${week.total.coerceAtMost(gold)} / $gold", style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
    }
    // The bar, with a mark at each step.
    Box(Modifier.fillMaxWidth().height(10.dp)) {
        Box(Modifier.fillMaxWidth().height(8.dp).align(Alignment.Center).clip(RoundedCornerShape(4.dp)).background(Brand.Mint))
        Box(Modifier.fillMaxWidth((week.total / gold.toFloat()).coerceIn(0f, 1f)).height(8.dp).align(Alignment.CenterStart).clip(RoundedCornerShape(4.dp)).background(Brand.Moss))
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().height(10.dp)) {
            app.tend.core.Together.Tier.entries.dropLast(1).forEach { t ->
                Box(Modifier.padding(start = maxWidth * (t.target(camps) / gold.toFloat()) - 1.dp).size(2.dp, 10.dp).background(Brand.Card))
            }
        }
    }
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        week.parts.forEachIndexed { i, (f, n) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Face(f.companion, 24)
                Spacer(Modifier.width(6.dp))
                Text("${if (i == 0) tr("You", "Te") else f.name} $n", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp))
            }
        }
    }
    val next = week.next
    Text(
        when {
            justClaimed && weeksDone == 1 && week.done -> tr("Gold! A bridge now crosses your river. +${app.tend.core.Together.ACORNS} acorns.", "Arany! Mostantól híd vezet át a folyódon. +${app.tend.core.Together.ACORNS} makk.")
            next == null -> tr("Gold, together. Every step gave acorns to each of you.", "Arany, együtt. Minden fokozat makkot adott mindenkinek.")
            else -> {
                val left = next.target(camps) - week.total
                val bridge = if (next == app.tend.core.Together.Tier.GOLD && weeksDone == 0) tr(", and a bridge over your river", ", és híd épül a folyódon") else ""
                tr("$left more between you all for ${next.title.lowercase()}: +${next.acorns} acorns each$bridge.", "Még $left közös tevékenység ${if (next.title.first().lowercaseChar() in "aáeéiíoóöőuúüű") "az" else "a"} ${next.title.lowercase()} fokozatig: fejenként +${next.acorns} makk$bridge.")
            }
        },
        style = MaterialTheme.typography.bodyMedium, color = if (week.tier != null) Brand.Moss else Brand.Muted,
    )
}

/** A friend's companion, as a small round face. */
@Composable
internal fun Face(companion: String, sizeDp: Int) {
    val avatar = runCatching { Avatar.valueOf(companion) }.getOrDefault(Avatar.BO)
    val bmp = remember(avatar) { CharacterPainter.bitmap(avatar, app.tend.core.Mood.HAPPY, sizeDp * 3).asImageBitmap() }
    Box(Modifier.size(sizeDp.dp).clip(CircleShape).background(Brand.Sage), contentAlignment = Alignment.Center) {
        Image(bmp, null, Modifier.size((sizeDp * 0.85f).dp))
    }
}

/** Your invite: the code, a QR code to scan side by side, and a link to send. */
@Composable
private fun InviteDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { code = Friends.myInvite(context); failed = code == null }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().surface(radius = 28.dp).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(tr("Invite a friend", "Hívj meg egy barátot"), style = MaterialTheme.typography.titleLarge)
            // What it's worth, for both: said before anything is sent.
            Text(
                tr(
                    "When they accept, you both get acorns (${app.tend.data.TendStore.FIRST_FRIEND_ACORNS} for a first friend) and a signpost; when they do their first activity, +${Friends.INVITE_ACORNS} more for each of you.",
                    "Ha elfogadja, mindketten makkot kaptok (az első barátért ${app.tend.data.TendStore.FIRST_FRIEND_ACORNS}-at) és útjelző táblát; amikor megcsinálja az első tevékenységét, még +${Friends.INVITE_ACORNS} makk jár mindkettőtöknek.",
                ),
                style = MaterialTheme.typography.bodyMedium, color = Brand.Muted,
            )
            val c = code
            when {
                c != null -> {
                    val qr = remember(c) { qrBitmap(Friends.inviteLink(c), 480).asImageBitmap() }
                    Box(Modifier.size(200.dp).clip(RoundedCornerShape(20.dp)).background(androidx.compose.ui.graphics.Color.White).padding(12.dp)) {
                        Image(qr, tr("QR code for your invite", "QR-kód a meghívódhoz"), Modifier.fillMaxWidth())
                    }
                    Text(tr("Scan it with their phone's camera, or send the link.", "Olvassa be a telefonja kamerájával, vagy küldd el a linket."), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    Text(c.chunked(3).joinToString(" "), style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = 4.sp), color = Brand.Moss)
                    PrimaryButton(tr("Send the link", "Link küldése"), {
                        app.tend.data.TendStore(context).invited = true
                        val text = tr("Let's tend our camps together! Open this on your phone: ${Friends.inviteLink(c)}\nOr in tend: Clearing → Friends → Enter a code: $c", "Gondozzuk együtt a táborunkat a tendben! Nyisd meg ezt a telefonodon: ${Friends.inviteLink(c)}\nVagy a tendben: Tábor → Barátok → Kód megadása: $c")
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), tr("Invite a friend", "Hívj meg egy barátot")))
                    })
                }
                failed -> Text(tr("Can't reach the server right now. Try again in a moment.", "Most nem érjük el a szervert. Próbáld újra egy pillanat múlva."), style = MaterialTheme.typography.bodyMedium)
                else -> Text(tr("Getting your code…", "Kérjük a kódod…"), style = MaterialTheme.typography.bodyMedium)
            }
            SecondaryButton(tr("Close", "Bezárás"), onDismiss)
        }
    }
}

@Composable
private fun EnterCodeDialog(onDismiss: () -> Unit, onCode: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val code = Friends.codeFrom(text)
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().surface(radius = 28.dp).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(tr("Enter a friend's code", "Add meg egy barátod kódját"), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                text, { text = it.take(80) }, placeholder = { Text(tr("ABC 234, or their link", "ABC 234, vagy a linkje")) }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters), shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(tr("Find", "Keresés"), { code?.let(onCode) }, enabled = code != null)
            SecondaryButton(tr("Close", "Bezárás"), onDismiss)
        }
    }
}

/** A QR code as a bitmap (black on white, no margin). */
fun qrBitmap(text: String, size: Int): Bitmap {
    val m = com.google.zxing.qrcode.QRCodeWriter().encode(text, com.google.zxing.BarcodeFormat.QR_CODE, size, size, mapOf(com.google.zxing.EncodeHintType.MARGIN to 0))
    val bmp = Bitmap.createBitmap(m.width, m.height, Bitmap.Config.ARGB_8888)
    for (x in 0 until m.width) for (y in 0 until m.height) bmp.setPixel(x, y, if (m[x, y]) 0xFF1D2A23.toInt() else 0xFFFFFFFF.toInt())
    return bmp
}

/**
 * A present for a friend: one of your things (out in the camp or in the chest). It leaves your camp
 * when it's sent, and waits wrapped by their fire. Pick who ([to] when it's for one friend) and what
 * ([thing] when it's for one thing), then send.
 */
@Composable
internal fun GiftDialog(friendsKnown: List<Friends.Friend>?, to: Friends.Friend? = null, thing: app.tend.core.Thing? = null, onGave: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { app.tend.data.TendStore(context) }
    var friends by remember { mutableStateOf(friendsKnown.orEmpty()) }
    var loading by remember { mutableStateOf(friendsKnown == null) }
    androidx.compose.runtime.LaunchedEffect(Unit) { if (friendsKnown == null) { Friends.list(context)?.let { friends = it }; loading = false } }
    var who by remember(friends) { mutableStateOf(to?.let { t -> friends.firstOrNull { it.id == t.id } ?: t } ?: friends.singleOrNull()) }
    fun mine() = (store.chest + store.placed.values).distinct().filterNot(app.tend.core.Home::isHome).mapNotNull(app.tend.core.Things::byId)
    var things by remember { mutableStateOf(thing?.let { t -> listOf(t).filter { it in mine() } } ?: mine()) }
    var what by remember { mutableStateOf(thing?.takeIf { it in things }) }
    var sending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf<String?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    // What they already have out in their camp can't be a present (the chest isn't shown to friends).
    fun has(f: Friends.Friend?, t: app.tend.core.Thing) = f != null && t.id in f.placed.values
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { if (!sending) onClose() },
        icon = { TendIcons.Glyph("gift", 28.dp) },
        title = { Text(tr("Send a present", "Ajándék küldése")) },
        text = {
            Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sent?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Brand.Moss) }
                problem?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Brand.Coral) }
                if (sent == null) Text(
                    tr("One of your things leaves your camp and waits wrapped by their fire. Up to ${Friends.GIFTS_A_DAY} a day.", "Egy tárgyad átkerül hozzá: becsomagolva vár rá a tüzénél. Naponta legfeljebb ${Friends.GIFTS_A_DAY}."),
                    style = MaterialTheme.typography.bodyMedium, color = Brand.Muted,
                )
                Text(tr("TO", "KINEK"), style = MaterialTheme.typography.labelSmall, color = Brand.Muted)
                when {
                    loading -> Text(tr("Loading your friends…", "Barátok betöltése…"), style = MaterialTheme.typography.bodyMedium)
                    friends.isEmpty() -> Text(tr("Add a friend first (Camp → Friends): then you can send presents.", "Előbb vegyél fel egy barátot (Tábor → Barátok): utána küldhetsz ajándékot."), style = MaterialTheme.typography.bodyMedium)
                    else -> androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        friends.forEach { f -> ChipButton(f.name, { who = f; problem = null; if (what?.let { has(f, it) } == true) what = null }, filled = who?.id == f.id) }
                    }
                }
                Text(tr("WHAT", "MIT"), style = MaterialTheme.typography.labelSmall, color = Brand.Muted)
                if (things.isEmpty()) Text(
                    if (thing != null) tr("It's not yours any more.", "Ez már nincs nálad.") else tr("Nothing to give yet: buy something in the shop first.", "Még nincs mit adnod: előbb vegyél valamit a boltban."),
                    style = MaterialTheme.typography.bodyMedium,
                )
                things.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { t ->
                            val theirs = has(who, t)
                            Box(Modifier.weight(1f)) {
                                ShopTile(t, owned = what == t, affordable = !theirs, Modifier.fillMaxWidth().graphicsLayer { alpha = if (theirs) 0.45f else 1f }, background = Brand.Card, inChest = t.id in store.chest) {
                                    if (theirs) problem = tr("${who?.name} already has ${t.name.lowercase()} in their camp.", "${who?.name} táborában már van ilyen: ${t.name.lowercase()}.")
                                    else { what = if (what == t) null else t; problem = null; sent = null }
                                }
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = {
            val f = who
            val t = what
            androidx.compose.material3.TextButton(
                enabled = f != null && t != null && !sending,
                onClick = {
                    if (f == null || t == null) return@TextButton
                    sending = true; problem = null
                    scope.launch {
                        when (Friends.giveThing(context, f.id, t.id)) {
                            // Gone only once it's on its way: out of the camp or the chest.
                            true -> {
                                store.gaveAway(t.id)
                                things = things - t; what = null
                                sent = tr("${t.name} is on its way to ${f.name}, wrapped.", "${t.name}: becsomagolva úton van, ${f.name} kapja.")
                                app.tend.data.Stats.log(context, "gave_thing")
                                onGave()
                            }
                            false -> problem = tr("Couldn't send it: that's ${Friends.GIFTS_A_DAY} presents today (or you're not friends any more). It stays with you.", "Nem ment el: ma már ${Friends.GIFTS_A_DAY} ajándékot küldtél (vagy már nem vagytok barátok). Nálad maradt.")
                            null -> problem = tr("Can't reach the server right now. Nothing was sent; it stays with you.", "Most nem érjük el a szervert. Semmi nem ment el, nálad maradt.")
                        }
                        sending = false
                    }
                },
            ) { Text(if (sending) tr("Sending…", "Küldés…") else if (t != null && f != null) tr("Send to ${f.name}", "Küldés: ${f.name}") else tr("Send", "Küldés"), color = Brand.Moss) }
        },
        dismissButton = { androidx.compose.material3.TextButton(enabled = !sending, onClick = onClose) { Text(if (sent != null) tr("Done", "Kész") else tr("Cancel", "Mégse"), color = Brand.Muted) } },
        containerColor = Brand.Card,
    )
}
