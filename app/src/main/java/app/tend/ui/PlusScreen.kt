package app.tend.ui

import app.tend.core.tr

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
    /** Plus isn't sold yet: everyone has it. */
    included: Boolean = false,
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
            paid || included -> tr("You have tend Plus", "Megvan a tend Plus")
            trialDaysLeft == null -> tr("Your Plus trial has ended", "Lejárt a Plus próbaidőd")
            else -> "tend Plus"
        },
        style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
    )
    Text(
        when {
            paid -> tr("Thank you for keeping ${avatar.displayName}'s camp going.", "Köszönjük, hogy életben tartod a tábort, ahol ${avatar.displayName} él.")
            included -> tr("Everything below is yours, free, while tend is new.", "Minden a tiéd, ami lent van, ingyen, amíg a tend új.")
            wonBack != null && wonBack >= 60 -> tr("So far you've won back ${InsightsOf.hours(wonBack)}. Plus keeps everything that helped.", "Eddig ennyi időt nyertél vissza: ${InsightsOf.hours(wonBack)}. A Plus megtart mindent, ami segített.")
            trialDaysLeft == null -> tr("Plus keeps everything that helped.", "A Plus megtart mindent, ami segített.")
            trialDaysLeft <= 1 -> tr("Your trial ends today. Everything below is yours until then.", "A próbaidőd ma jár le. Addig minden a tiéd, ami lent van.")
            else -> tr("You have everything below for $trialDaysLeft more day${if (trialDaysLeft == 1) "" else "s"}, free.", "Még $trialDaysLeft napig minden a tiéd, ami lent van, ingyen.")
        },
        style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = Brand.Muted,
    )
    Card {
        Feature("hourglass", tr("Your time, in full", "Az időd, részletesen"), tr("Every week, and each app before and now.", "Minden hét, és minden app: akkor és most."))
        Feature("alert", tr("A limit for each app", "Saját limit minden appnak"), tr("Its own minutes a day, on top of your budget.", "Saját napi percek, a kereteden felül."))
        Feature("moon", tr("Strict mode", "Szigorú mód"), tr("No shortcuts, for the days you need it.", "Nincs kiskapu, azokra a napokra, amikor erre van szükség."))
        Feature("flower", tr("The season's own things", "Az évszak saját tárgyai"), tr("Three new ones every season, yours to keep.", "Évszakonként három új, és örökre a tiéd."))
    }
    Text(
        tr("Free stays free: the pause, your minutes, activities, the camp and friends.", "Ami ingyenes, az is marad: a szünet, a perceid, a tevékenységek, a tábor és a barátok."),
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), textAlign = TextAlign.Center, color = Brand.Muted,
    )
    if (!paid && !included) {
        if (canBuy) PrimaryButton(tr("Get Plus", "Kérem a Plust"), onBuy, modifier = Modifier.fillMaxWidth())
        else Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brand.Card).padding(vertical = 14.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) { Text(tr("Plus will be on Google Play soon", "A Plus hamarosan elérhető a Google Playen"), style = MaterialTheme.typography.labelLarge, color = Brand.Muted, textAlign = TextAlign.Center) }
    }
    SecondaryButton(if (paid || included || trialDaysLeft != null) tr("Back", "Vissza") else tr("Continue free", "Folytatom ingyen"), onClose, modifier = Modifier.fillMaxWidth())
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
