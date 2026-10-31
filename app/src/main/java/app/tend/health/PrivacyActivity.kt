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
                    Text("What tend reads", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "tend reads today's step count and exercise sessions from Health Connect. " +
                            "It uses them only to mark your walk and workout tasks as done and give you screen-time minutes back.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        "Nothing is written to Health Connect, and nothing leaves your phone. " +
                            "Only today's totals are kept, and they reset at midnight.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text("You can turn access off any time in Health Connect.", style = MaterialTheme.typography.bodyMedium)
                    PrimaryButton("Got it", ::finish, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}
