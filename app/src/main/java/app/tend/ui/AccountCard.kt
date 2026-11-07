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
fun AccountCard(onChanged: () -> Unit) {
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

    fun run(block: suspend () -> Account.Result) {
        busy = true; message = null
        scope.launch {
            when (val r = block()) {
                Account.Result.Ok -> { user = Account.user(context); onChanged() }
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
            Text("Your camp can be backed up; friends are coming soon.", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
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
