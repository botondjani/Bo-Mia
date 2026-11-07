package app.tend.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tend.core.Accessory
import app.tend.data.Friends
import kotlinx.coroutines.launch

/**
 * Along the river: your camp first, then each friend's, one under the other. [focus] scrolls to a
 * friend's camp (the one tapped on the clearing page). [friends] null loads them here.
 */
@Composable
fun RiverScreen(
    me: Friends.Friend,
    worn: Set<Accessory>,
    night: Boolean,
    focus: String?,
    onBack: () -> Unit,
    myPranks: Set<String> = emptySet(),
    onGift: () -> Boolean = { false },
    onRefund: () -> Unit = {},
    friends: List<Friends.Friend>? = null,
) {
    val context = LocalContext.current
    var list by remember { mutableStateOf(friends) }
    var offline by remember { mutableStateOf(false) }
    // What went to whom in the last day: their pranks show in their camps, and each kind waits a day.
    var sent by remember { mutableStateOf<Map<String, Set<Friends.Kind>>>(emptyMap()) }
    var note by remember { mutableStateOf<Pair<String, String>?>(null) } // friend id to a short line under their camp
    var pranking by remember { mutableStateOf<Friends.Friend?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    fun send(f: Friends.Friend, kind: Friends.Kind) {
        if (kind in sent[f.id].orEmpty()) { note = f.id to "Already sent today. Again tomorrow."; return }
        if (kind == Friends.Kind.GIFT && !onGift()) { note = f.id to "A gift is ${Friends.GIFT_ACORNS} acorns, and you don't have that many."; return }
        scope.launch {
            val ok = Friends.send(context, f.id, kind)
            if (ok) sent = sent + (f.id to (sent[f.id].orEmpty() + kind))
            else if (kind == Friends.Kind.GIFT) onRefund()
            note = f.id to if (ok) "Sent! ${f.name} will see it." else "Couldn't send it. Try again in a moment."
        }
    }
    val state = rememberLazyListState()
    LaunchedEffect(Unit) {
        if (list == null) Friends.list(context).let { list = it; offline = it == null }
        Friends.sent(context)?.let { sent = it }
        val i = list.orEmpty().indexOfFirst { it.id == focus }
        if (i >= 0) state.scrollToItem(i + 2)
    }

    LazyColumn(
        Modifier.fillMaxSize().background(Brand.Sage), state = state,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = Space.s, bottom = Space.xxl),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButtonSoft(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
                Spacer(Modifier.width(Space.m))
                Text("Along the river", style = MaterialTheme.typography.headlineMedium, maxLines = 1)
            }
        }
        item { Camp(me, "Your camp", worn, night, myPranks) }
        items(list.orEmpty(), key = { it.id }) { f ->
            val done = sent[f.id].orEmpty()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Camp(f, f.name, emptySet(), night, done.filter { it.prank }.map { it.id }.toSet())
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipButton(if (Friends.Kind.WAVE in done) "Waved" else "Wave", { send(f, Friends.Kind.WAVE) }, filled = Friends.Kind.WAVE !in done)
                    ChipButton(if (Friends.Kind.GIFT in done) "Gift sent" else Friends.Kind.GIFT.label, { send(f, Friends.Kind.GIFT) }, filled = false)
                    ChipButton("Prank…", { pranking = f }, filled = false)
                }
                note?.takeIf { it.first == f.id }?.let { Text(it.second, style = MaterialTheme.typography.bodyMedium, color = Brand.Muted) }
            }
        }
        if (list?.isEmpty() == true || offline) item {
            Text(
                if (offline) "Can't reach the server right now, so your friends' camps aren't here."
                else "Downstream it's still quiet. Invite a friend and their camp appears here.",
                style = MaterialTheme.typography.bodyMedium, color = Brand.Muted, modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    pranking?.let { f ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pranking = null },
            title = { Text("Prank ${f.name}'s camp") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("It stays in their camp for a day. One of each a day.", style = MaterialTheme.typography.bodyMedium, color = Brand.Muted)
                    Friends.Kind.entries.filter { it.prank }.forEach { k ->
                        val done = k in sent[f.id].orEmpty()
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).tappable { pranking = null; send(f, k) }.padding(vertical = 8.dp, horizontal = 4.dp),
                        ) {
                            Text(k.label + if (done) " (sent)" else "", style = MaterialTheme.typography.titleSmall, color = if (done) Brand.Muted else Brand.Moss)
                            Text(PRANK_LINES.getValue(k), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = Brand.Muted)
                        }
                    }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { pranking = null }) { Text("Close") } },
            containerColor = Brand.Card,
        )
    }
}

private val PRANK_LINES = mapOf(
    Friends.Kind.DUCK to "Bobs in their river.",
    Friends.Kind.GNOME to "Stands by their camp, watching.",
    Friends.Kind.FLAMINGO to "Very pink. Very classy.",
    Friends.Kind.TP to "All over their oak.",
)

/** One camp: the scene, and under it whose it is. */
@Composable
private fun Camp(f: Friends.Friend, title: String, worn: Set<Accessory>, night: Boolean, pranks: Set<String> = emptySet()) = Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    val avatar = runCatching { app.tend.core.Avatar.valueOf(f.companion) }.getOrDefault(app.tend.core.Avatar.BO)
    Box(Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(26.dp))) {
        Canvas(Modifier.fillMaxSize()) { drawClearing(avatar, f.placed, worn = worn, night = night, pranks = pranks) }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        Face(f.companion, 32)
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, modifier = Modifier.weight(1f))
        Text("Level ${f.level}", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = Brand.Moss)
    }
}
