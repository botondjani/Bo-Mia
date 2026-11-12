package app.tend.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.tend.ui.Brand
import app.tend.ui.PrimaryButton
import app.tend.ui.TendTheme

/** Required by Health Connect: explains what tend reads and why. */
class PrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // tend is always light: dark status-bar icons even when the phone is in dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        setContent {
            TendTheme {
                Column(
                    Modifier.fillMaxSize().background(Brand.Sage).safeDrawingPadding()
                        .verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Your privacy", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "Your data stays on your phone unless you choose otherwise. No ads, no tracking, no account needed.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text("WHAT TEND USES, AND WHY", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                    listOf(
                        "Usage access: to notice when you open an app you picked, and to count your minutes.",
                        "Display over other apps: to show the pause over that app.",
                        "Health Connect, if you connect it: steps, exercise and sleep, to complete those activities and give minutes back. " +
                            "With background access, a finished walk counts without opening tend. tend only reads; it never writes or shares health data.",
                        "Notifications, if you allow them: the morning plan, a heads-up at 10 minutes left, the evening recap, the Sunday letter.",
                        "Your installed apps: only to let you pick which ones to pause. The list and your pick stay on your phone.",
                        "Accessibility service, only if you switch on \"Close only Reels & Shorts\": in Instagram, YouTube and Facebook it checks whether " +
                            "the short-video player is showing, and goes back if it is. Nothing on your screen is read, kept or sent.",
                        "Your town, if you type one: its name and rough coordinates go to Open-Meteo for the camp's weather. Never your phone's location.",
                        "Anonymous usage stats, only if you say yes: counts like \"a pause ended in Not now\" or \"a walk was done\", " +
                            "with a random id and the day since install. No names, app names or health data. Off any time in Settings.",
                        "An account, only if you log in (Google or an e-mail code): your name and e-mail, your companion and level, " +
                            "and a backup of your settings, camp, acorns and history so you can restore them. " +
                            "Never your health readings or which apps you use. Delete it any time: Settings → Account → Delete account.",
                    ).forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
                    Text(
                        "To delete everything on this phone: Settings → Start over, or uninstall tend. To delete your account and backup: Settings → Account → Delete account. Health Connect access can be turned off any time in Health Connect.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    PrimaryButton("Got it", ::finish, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}
