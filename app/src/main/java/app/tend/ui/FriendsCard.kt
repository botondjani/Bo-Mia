package app.tend.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
fun FriendsCard(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val signedIn = Account.user(context) != null
    var friends by remember { mutableStateOf<List<Friends.Friend>?>(null) }
    var offline by remember { mutableStateOf(false) }
    var inviting by rememberSaveable { mutableStateOf(false) }
    var typing by rememberSaveable { mutableStateOf(false) }
    var asking by remember { mutableStateOf<Pair<String, Friends.Friend>?>(null) }
    var removing by remember { mutableStateOf<Friends.Friend?>(null) }
    var note by rememberSaveable { mutableStateOf<String?>(null) }

    suspend fun reload() {
        val list = Friends.list(context)
        offline = list == null
        if (list != null) friends = list
    }
    /** Looks a code up and asks before adding; says so when it's unknown. */
    suspend fun lookUp(code: String) {
        val owner = Friends.owner(context, code)
        if (owner != null) asking = code to owner else note = "That code isn't anyone's (or it's yours). Check it and try again."
    }

    LaunchedEffect(signedIn) {
        if (!signedIn) return@LaunchedEffect
        reload()
        Friends.pendingCode(context)?.let { code -> Friends.setPendingCode(context, null); lookUp(code) }
    }

    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("FRIENDS", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            friends?.takeIf { it.isNotEmpty() }?.let { Text("${it.size}", style = MaterialTheme.typography.labelLarge, color = Brand.Moss) }
        }
        if (!signedIn) {
            Text(
                if (Friends.pendingCode(context) != null) "Someone invited you. Log in to accept, and to see each other's camps."
                else "Add friends to see each other's camps along the river. It needs an account (Settings → Account).",
                style = MaterialTheme.typography.bodyMedium,
            )
            ChipButton("Log in", onOpenSettings)
        } else {
            val list = friends.orEmpty()
            if (list.isEmpty()) Text(
                if (offline) "Can't reach the server right now." else "No friends yet. Invite someone: when they accept, you'll see each other's camps.",
                style = MaterialTheme.typography.bodyMedium,
            )
            list.forEach { f -> FriendRow(f) { removing = f } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                ChipButton("Invite a friend", { inviting = true })
                ChipButton("Enter a code", { typing = true }, filled = false)
            }
        }
        note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Brand.Coral) }
    }

    if (inviting) InviteDialog { inviting = false }
    if (typing) EnterCodeDialog(onDismiss = { typing = false }) { code -> typing = false; note = null; scope.launch { lookUp(code) } }
    asking?.let { (code, owner) ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { asking = null },
            icon = { Face(owner.companion, 72) },
            title = { Text("Add ${owner.name} as a friend?") },
            text = { Text("You'll see each other's camps, levels and weekly challenge. Nothing else: not your apps, minutes or health data.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    asking = null
                    scope.launch { if (Friends.accept(context, code)) reload() else note = "Couldn't add them. Try again in a moment." }
                }) { Text("Add", color = Brand.Moss) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { asking = null }) { Text("Not now") } },
            containerColor = Brand.Card,
        )
    }
    removing?.let { f ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove ${f.name}?") },
            text = { Text("You'll stop seeing each other's camps. They won't be told, and your invite code changes so an old link can't add you back.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { removing = null; scope.launch { Friends.remove(context, f.id); reload() } }) { Text("Remove", color = Brand.Coral) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { removing = null }) { Text("Keep") } },
            containerColor = Brand.Card,
        )
    }
}

@Composable
private fun FriendRow(f: Friends.Friend, onRemove: () -> Unit) = Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
    Face(f.companion, 40)
    Spacer(Modifier.width(10.dp))
    Column(Modifier.weight(1f)) {
        Text(f.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        Text("Level ${f.level} · ${f.placed.size} thing${if (f.placed.size == 1) "" else "s"} at camp", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
    }
    Text("Remove", style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = Brand.Muted,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable(onClick = onRemove).padding(horizontal = 8.dp, vertical = 6.dp))
}

/** A friend's companion, as a small round face. */
@Composable
private fun Face(companion: String, sizeDp: Int) {
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
            Text("Invite a friend", style = MaterialTheme.typography.titleLarge)
            val c = code
            when {
                c != null -> {
                    val qr = remember(c) { qrBitmap(Friends.inviteLink(c), 480).asImageBitmap() }
                    Box(Modifier.size(200.dp).clip(RoundedCornerShape(20.dp)).background(androidx.compose.ui.graphics.Color.White).padding(12.dp)) {
                        Image(qr, "QR code for your invite", Modifier.fillMaxWidth())
                    }
                    Text("Scan it with their phone's camera, or send the link.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    Text(c.chunked(3).joinToString(" "), style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = 4.sp), color = Brand.Moss)
                    PrimaryButton("Send the link", {
                        val text = "Let's tend our camps together! Open this on your phone: ${Friends.inviteLink(c)}\nOr in tend: Clearing → Friends → Enter a code: $c"
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Invite a friend"))
                    })
                    Text("New code", style = MaterialTheme.typography.labelLarge, color = Brand.Muted,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable { scope.launch { code = Friends.myInvite(context, fresh = true) ?: code } }.padding(8.dp))
                }
                failed -> Text("Can't reach the server right now. Try again in a moment.", style = MaterialTheme.typography.bodyMedium)
                else -> Text("Getting your code…", style = MaterialTheme.typography.bodyMedium)
            }
            SecondaryButton("Close", onDismiss)
        }
    }
}

@Composable
private fun EnterCodeDialog(onDismiss: () -> Unit, onCode: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val code = Friends.codeFrom(text)
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().surface(radius = 28.dp).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Enter a friend's code", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                text, { text = it.take(80) }, placeholder = { Text("ABC 234, or their link") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters), shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton("Find", { code?.let(onCode) }, enabled = code != null)
            SecondaryButton("Close", onDismiss)
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
