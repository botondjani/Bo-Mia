package app.tend.monitor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tend.core.Mood
import app.tend.data.TendStore
import app.tend.ui.Card
import app.tend.ui.CharacterView
import app.tend.ui.TendTheme

/** One-tap question after a session in a blocked app. Feeds the weekly patterns. */
class WorthItActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = TendStore(this)
        val pkg = intent.getStringExtra(EXTRA_PKG).orEmpty()
        val seconds = intent.getLongExtra(EXTRA_SECONDS, 0)
        val minutes = ((seconds + 59) / 60).coerceAtLeast(1)

        setContent {
            TendTheme {
                Card {
                    CharacterView(store.avatar, Mood.HAPPY, 72.dp)
                    Text(
                        "Were those $minutes minutes worth it?",
                        style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Yes", "Kind of", "Not really").forEach { a ->
                            OutlinedButton(onClick = { store.logAnswer("worth_it", pkg, a, seconds); finish() }) { Text(a) }
                        }
                    }
                    Text("Just for you: we use it for your weekly patterns.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    companion object {
        private const val EXTRA_PKG = "pkg"
        private const val EXTRA_SECONDS = "seconds"
        fun intent(context: Context, pkg: String, seconds: Long) = Intent(context, WorthItActivity::class.java)
            .putExtra(EXTRA_PKG, pkg)
            .putExtra(EXTRA_SECONDS, seconds)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    }
}
