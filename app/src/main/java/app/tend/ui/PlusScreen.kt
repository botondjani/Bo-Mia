package app.tend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tend.core.Avatar
import app.tend.core.InsightsOf
import app.tend.core.Mood

/** Where the Plus page was opened from, for the counts: a locked feature, settings, or the trial's end. */
enum class PlusFrom { TRIAL_END, SETTINGS, INSIGHTS, APP_LIMITS, STRICT, SEASON }

/**
 * tend Plus: what it adds, the time won back so far, and the way on. The free part stays free:
 * the pause, the minutes, the camp and friends.
 */
@Composable
fun PlusScreen(
    avatar: Avatar,
    /** Days left of the free trial; null once it's over. */
    trialDaysLeft: Int?,
    paid: Boolean,
    /** Minutes won back so far, when known. */
    wonBack: Int?,
    /** Google Play billing is there (the Play build, once it's set up). */
    canBuy: Boolean,
    onBuy: () -> Unit,
    onClose: () -> Unit,
) = Column(
    Modifier.fillMaxSize().background(Brand.Sage).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(top = Space.l, bottom = Space.xxl),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(Space.m),
) {
    BobbingCharacter(avatar, Mood.HAPPY, 120.dp)
    Text(
        when {
            paid -> "You have tend Plus"
            trialDaysLeft == null -> "Your Plus trial has ended"
            else -> "tend Plus"
        },
        style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
    )
    Text(
        when {
            paid -> "Thank you for keeping ${avatar.displayName}'s camp going."
            wonBack != null && wonBack >= 60 -> "So far you've won back ${InsightsOf.hours(wonBack)}. Plus keeps everything that helped."
            trialDaysLeft == null -> "Plus keeps everything that helped."
            trialDaysLeft <= 1 -> "Your trial ends today. Everything below is yours until then."
            else -> "You have everything below for $trialDaysLeft more day${if (trialDaysLeft == 1) "" else "s"}, free."
        },
        style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = Brand.Muted,
    )
    Card {
        Feature("hourglass", "Your time, in full", "Every week, and each app before and now.")
        Feature("alert", "A limit for each app", "Its own minutes a day, on top of your budget.")
        Feature("moon", "Strict mode", "No shortcuts, for the days you need it.")
        Feature("flower", "The season's own things", "Three new ones every season, yours to keep.")
    }
    Text(
        "Free stays free: the pause, your minutes, activities, the camp and friends.",
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), textAlign = TextAlign.Center, color = Brand.Muted,
    )
    if (!paid) {
        if (canBuy) PrimaryButton("Get Plus", onBuy, modifier = Modifier.fillMaxWidth())
        else Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brand.Card).padding(vertical = 14.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Plus will be on Google Play soon", style = MaterialTheme.typography.labelLarge, color = Brand.Muted, textAlign = TextAlign.Center) }
    }
    SecondaryButton(if (paid || trialDaysLeft != null) "Back" else "Continue free", onClose, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun Feature(icon: String, title: String, body: String) = Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
    TendIcons.Badge(icon, 40.dp)
    Spacer(Modifier.width(12.dp))
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(body, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = Brand.Muted)
    }
}

/** A small "Plus" mark on a locked feature. */
@Composable
fun PlusMark(modifier: Modifier = Modifier) = Text(
    "PLUS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = androidx.compose.ui.graphics.Color.White,
    modifier = modifier.clip(RoundedCornerShape(50)).background(Brand.Coral).padding(horizontal = 8.dp, vertical = 2.dp),
)
