package app.tend.monitor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tend.core.Mood
import app.tend.data.TendStore
import app.tend.ui.Brand
import app.tend.ui.CharacterView
import app.tend.ui.PopupSheet
import app.tend.ui.blurBehind
import app.tend.ui.TendIcons
import app.tend.ui.TendTheme

/** One-tap question after a session in a paused app. Feeds the weekly patterns. */
class WorthItActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        blurBehind()
        val store = TendStore(this)
        val pkg = intent.getStringExtra(EXTRA_PKG).orEmpty()
        val seconds = intent.getLongExtra(EXTRA_SECONDS, 0)
        val minutes = ((seconds + 59) / 60).coerceAtLeast(1)
        val label = runCatching { packageManager.getApplicationInfo(pkg, 0).loadLabel(packageManager).toString() }.getOrNull()
        val what = (if (minutes == 1L) "that minute" else "those $minutes minutes") + (label?.let { " on $it" } ?: "")

        setContent {
            TendTheme {
                PopupSheet(onDismiss = ::finish, centered = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CharacterView(store.avatar, Mood.HAPPY, 72.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Was $what worth it?", style = MaterialTheme.typography.titleLarge)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            Triple("face_yes", "Yes", Brand.Mint),
                            Triple("face_meh", "Kind of", Brand.Sunny),
                            Triple("face_no", "Not really", Brand.Coral),
                        ).forEach { (icon, answer, tint) ->
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(tint.copy(alpha = 0.25f))
                                    .clickable { store.logAnswer("worth_it", pkg, answer, seconds); finish() }
                                    .padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                TendIcons.Glyph(icon, 34.dp)
                                Text(answer, style = MaterialTheme.typography.labelLarge, color = Brand.Ink, maxLines = 1)
                            }
                        }
                    }
                    Text(
                        "Just for you: it shapes your weekly patterns.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
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
