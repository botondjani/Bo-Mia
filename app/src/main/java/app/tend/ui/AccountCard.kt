package app.tend.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tend.data.Account
import kotlinx.coroutines.launch

/**
 * Settings → Account. Signed out: one big "Continue with Google", and a small "Log in with e-mail
 * instead" that asks for the address, then the 6-digit code. Signed in: who, sign out, delete.
 * [onChanged] runs after signing in or out, so the app can save the profile and redraw.
 */
@Composable
fun AccountCard(onChanged: () -> Unit, onRestored: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var user by androidx.compose.runtime.remember { mutableStateOf(Account.user(context)) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var useEmail by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var codeSent by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var lastBackup by androidx.compose.runtime.remember { mutableStateOf(app.tend.data.Backup.lastBackupAt(context)) }
    // A backup found on the server right after logging in, waiting for "Restore" or "Keep this phone's".
    var found by androidx.compose.runtime.remember { mutableStateOf<app.tend.data.Backup.Saved?>(null) }

    /** Right after logging in: offer the server's backup, or send this phone's if there's none. */
    suspend fun afterLogin() {
        val saved = app.tend.data.Backup.fetch(context)
        if (saved != null) found = saved
        else if (app.tend.data.Backup.upload(context)) lastBackup = app.tend.data.Backup.lastBackupAt(context)
    }

    fun run(block: suspend () -> Account.Result) {
        busy = true; message = null
        scope.launch {
            when (val r = block()) {
                Account.Result.Ok -> {
                    val wasSignedIn = user != null
                    user = Account.user(context)
                    onChanged()
                    if (!wasSignedIn && user != null) afterLogin()
                }
                is Account.Result.Failed -> message = r.message.ifBlank { null }
            }
            busy = false
        }
    }

    Card {
        val u = user
        if (u != null) {
            Text(u.name.ifBlank { u.email }, style = MaterialTheme.typography.titleMedium)
            Text(u.email, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (lastBackup > 0) "Backed up ${android.text.format.DateUtils.getRelativeTimeSpanString(lastBackup, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS)}; it saves itself twice a day."
                else "Not backed up yet.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted,
            )
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                ChipButton(if (busy) "Saving…" else "Back up now", {
                    busy = true; message = null
                    scope.launch {
                        if (app.tend.data.Backup.upload(context)) lastBackup = app.tend.data.Backup.lastBackupAt(context)
                        else message = "Couldn't back up. Check the connection and try again."
                        busy = false
                    }
                }, filled = false)
                ChipButton("Sign out", { scope.launch { Account.signOut(context); user = null; onChanged() } }, filled = false)
                ChipButton("Delete account", { confirmDelete = true }, filled = false)
            }
        } else {
            Text("Back up your camp, and soon see your friends' camps. Optional: tend works fully without it.", style = MaterialTheme.typography.bodyMedium)
            if (!useEmail) {
                PrimaryButton(if (busy) "Opening Google…" else "Continue with Google", {
                    (context as? android.app.Activity)?.let { a -> run { Account.signInWithGoogle(a) } }
                }, enabled = !busy, modifier = Modifier.padding(top = 6.dp))
                Text(
                    "Log in with e-mail instead", style = MaterialTheme.typography.labelLarge, color = Brand.Moss, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).tappable { useEmail = true; message = null }.padding(vertical = 8.dp),
                )
            } else {
                OutlinedTextField(
                    email, { email = it; codeSent = false }, placeholder = { Text("you@example.com") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (codeSent) OutlinedTextField(
                    code, { code = it.filter(Char::isDigit).take(6) }, placeholder = { Text("6-digit code") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!codeSent) PrimaryButton(if (busy) "Sending…" else "Send me a code", {
                    run { Account.sendEmailCode(email).also { if (it == Account.Result.Ok) { codeSent = true; message = "Check your inbox for a 6-digit code." } } }
                }, enabled = !busy && "@" in email && "." in email.substringAfter('@'))
                else PrimaryButton(if (busy) "Logging in…" else "Log in", { run { Account.verifyEmailCode(context, email, code) } }, enabled = !busy && code.length == 6)
                Text(
                    "Use Google instead", style = MaterialTheme.typography.labelLarge, color = Brand.Moss, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).tappable { useEmail = false; codeSent = false; message = null }.padding(vertical = 8.dp),
                )
            }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = if (it.startsWith("Check")) Brand.Moss else Brand.Coral) }
    }

    found?.let { saved ->
        val date = runCatching { java.time.OffsetDateTime.parse(saved.updatedAt).toLocalDate().toString() }.getOrDefault("")
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            title = { Text("Restore your camp?") },
            text = { Text("There's a backup from $date. Restoring replaces what's on this phone with it. Keeping this phone's data replaces the backup instead.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    found = null
                    app.tend.data.Backup.restore(context, saved.data)
                    onRestored()
                }) { Text("Restore", color = Brand.Moss) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    found = null
                    scope.launch { if (app.tend.data.Backup.upload(context)) lastBackup = app.tend.data.Backup.lastBackupAt(context) }
                }) { Text("Keep this phone's") }
            },
            containerColor = Brand.Card,
        )
    }

    if (confirmDelete) androidx.compose.material3.AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete your account?") },
        text = { Text("This deletes your account and its backup on our server. What's on this phone stays.") },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { confirmDelete = false; run { Account.deleteAccount(context).also { if (it == Account.Result.Ok) user = null } } }) {
                Text("Delete", color = Brand.Coral)
            }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) { Text("Keep") } },
        containerColor = Brand.Card,
    )
}
