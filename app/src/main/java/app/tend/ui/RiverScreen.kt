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
    myPaper: Boolean = false,
    myDucks: Int = 0,
    friends: List<Friends.Friend>? = null,
) {
    val context = LocalContext.current
    var list by remember { mutableStateOf(friends) }
    var offline by remember { mutableStateOf(false) }
    // What you did to whom: waves and visits wait a day; your paper stays up until they unroll it.
    var sent by remember { mutableStateOf(Friends.Sent(emptyMap(), emptySet())) }
    var note by remember { mutableStateOf<Pair<String, String>?>(null) } // friend id to a short line under their camp
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    fun wave(f: Friends.Friend) {
        if (Friends.Kind.WAVE in sent.used[f.id].orEmpty()) { note = f.id to "You waved today. Again tomorrow."; return }
        scope.launch {
            val ok = Friends.send(context, f.id, Friends.Kind.WAVE)
            if (ok) sent = Friends.Sent(sent.used + (f.id to (sent.used[f.id].orEmpty() + Friends.Kind.WAVE)), sent.paperUp)
            note = f.id to if (ok) "${f.name} will see your wave." else "Couldn't wave. Try again in a moment."
        }
    }
    fun paper(f: Friends.Friend) {
        when {
            f.id in sent.paperUp -> { note = f.id to "Your paper's still up. ${f.name} has to unroll it first."; return }
            Friends.Kind.TP in sent.used[f.id].orEmpty() -> { note = f.id to "${f.name} already unrolled it. Again tomorrow."; return }
        }
        scope.launch {
            val ok = Friends.send(context, f.id, Friends.Kind.TP)
            if (ok) sent = Friends.Sent(sent.used + (f.id to (sent.used[f.id].orEmpty() + Friends.Kind.TP)), sent.paperUp + f.id)
            note = f.id to if (ok) "Done. It stays up until ${f.name} unrolls it." else "Couldn't do it. Try again in a moment."
        }
    }
    val state = rememberLazyListState()
    LaunchedEffect(Unit) {
        if (list == null) Friends.list(context).let { list = it; offline = it == null }
        Friends.sent(context)?.let { sent = it }
        val i = list.orEmpty().indexOfFirst { it.id == focus }
        if (i >= 0) state.scrollToItem(i + 2)
        // A camp you look at counts as a visit: the friend finds a rubber duck in their river.
        val visited = mutableSetOf<String>()
        androidx.compose.runtime.snapshotFlow { state.layoutInfo.visibleItemsInfo.map { it.key } }.collect { keys ->
            list.orEmpty().filter { it.id in keys && it.id !in visited && Friends.Kind.VISIT !in sent.used[it.id].orEmpty() }.forEach { f ->
                visited += f.id
                launch { Friends.send(context, f.id, Friends.Kind.VISIT) }
            }
        }
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
        item { Camp(me, "Your camp", worn, night, myPaper, myDucks) }
        items(list.orEmpty(), key = { it.id }) { f ->
            val done = sent.used[f.id].orEmpty()
            val up = f.id in sent.paperUp
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Camp(f, f.name, emptySet(), night, paper = up)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipButton(if (Friends.Kind.WAVE in done) "Waved" else "Wave", { wave(f) }, filled = Friends.Kind.WAVE !in done)
                    ChipButton(if (up) "Paper's up" else "Toilet-paper their oak", { paper(f) }, filled = false)
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
}

/** One camp: the scene, and under it whose it is. */
@Composable
private fun Camp(f: Friends.Friend, title: String, worn: Set<Accessory>, night: Boolean, paper: Boolean = false, ducks: Int = 0) = Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    val avatar = runCatching { app.tend.core.Avatar.valueOf(f.companion) }.getOrDefault(app.tend.core.Avatar.BO)
    Box(Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(26.dp))) {
        Canvas(Modifier.fillMaxSize()) { drawClearing(avatar, f.placed, worn = worn, night = night, paper = if (paper) 1f else 0f, ducks = ducks, bridge = f.bridge,
            layout = f.layout.mapValues { androidx.compose.ui.geometry.Offset(it.value.first, it.value.second) }) }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        Face(f.companion, 32)
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, modifier = Modifier.weight(1f))
        Text("Level ${f.level}", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = Brand.Moss)
    }
}
