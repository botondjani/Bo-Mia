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
    friends: List<Friends.Friend>? = null,
) {
    val context = LocalContext.current
    var list by remember { mutableStateOf(friends) }
    var offline by remember { mutableStateOf(false) }
    val state = rememberLazyListState()
    LaunchedEffect(Unit) {
        if (list == null) Friends.list(context).let { list = it; offline = it == null }
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
        item { Camp(me, "Your camp", worn, night) }
        items(list.orEmpty(), key = { it.id }) { f -> Camp(f, f.name, emptySet(), night) }
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
private fun Camp(f: Friends.Friend, title: String, worn: Set<Accessory>, night: Boolean) = Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    val avatar = runCatching { app.tend.core.Avatar.valueOf(f.companion) }.getOrDefault(app.tend.core.Avatar.BO)
    Box(Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(26.dp))) {
        Canvas(Modifier.fillMaxSize()) { drawClearing(avatar, f.placed, worn = worn, night = night) }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        Face(f.companion, 32)
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, modifier = Modifier.weight(1f))
        Text("Level ${f.level}", style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = Brand.Moss)
    }
}
