package app.tend.health

import app.tend.core.tr

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
                    Text(tr("Your privacy", "Az adataid"), style = MaterialTheme.typography.headlineLarge)
                    Text(
                        tr("Your data stays on your phone unless you choose otherwise. No ads, no tracking, no account needed.", "Az adataid a telefonodon maradnak, hacsak te nem döntesz másként. Nincs reklám, nincs követés, fiók sem kell."),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(tr("WHAT TEND USES, AND WHY", "MIT HASZNÁL A TEND, ÉS MIÉRT"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                    listOf(
                        tr("Usage access: to notice when you open an app you picked, and to count your minutes.", "Használati hozzáférés: hogy észrevegye, amikor megnyitsz egy kiválasztott appot, és számolja a perceidet."),
                        tr("Display over other apps: to show the pause over that app.", "Megjelenítés más appok fölött: hogy a szünet megjelenhessen az app fölött."),
                        tr("Health Connect, if you connect it: steps, exercise and sleep, to complete those activities and give minutes back. ", "Health Connect, ha összekötöd: lépések, mozgás és alvás, hogy ezek a tevékenységek maguktól teljesüljenek, és perceket adjanak vissza. ") +
                            tr("With background access, a finished walk counts without opening tend. tend only reads; it never writes or shares health data.", "Háttérhozzáféréssel egy séta a tend megnyitása nélkül is beszámít. A tend csak olvas; egészségadatot soha nem ír és nem oszt meg."),
                        tr("Notifications, if you allow them: the morning plan, a heads-up at 10 minutes left, the evening recap, the Sunday letter.", "Értesítések, ha engedélyezed: a reggeli terv, jelzés 10 percnél, az esti összefoglaló és a vasárnapi levél."),
                        tr("Your installed apps: only to let you pick which ones to pause. The list and your pick stay on your phone.", "A telepített appjaid: csak azért, hogy kiválaszthasd, melyiket szünetelteted. A lista és a választásod a telefonodon marad."),
                        tr("Accessibility service, only if you switch on \"Close only Reels & Shorts\": in Instagram, YouTube and Facebook it checks whether ", "Akadálymentesítési szolgáltatás, csak ha bekapcsolod a „Csak a Reels és a Shorts zárása” funkciót: az Instagramban, a YouTube-on és a Facebookon azt figyeli, ") +
                            tr("the short-video player is showing, and goes back if it is. Nothing on your screen is read, kept or sent.", "hogy a rövidvideó-lejátszó van-e elöl, és ha igen, visszalép. A képernyőd tartalmát nem olvassa, nem tárolja és nem küldi el."),
                        tr("Your town, if you type one: its name and rough coordinates go to Open-Meteo for the camp's weather. Never your phone's location.", "A városod, ha megadod: a neve és a hozzávetőleges koordinátái az Open-Meteóhoz mennek a tábor időjárásáért. A telefonod helyzete soha."),
                        tr("Anonymous usage stats, only if you say yes: counts like \"a pause ended in Not now\" or \"a walk was done\", ", "Névtelen használati statisztika, csak ha igent mondasz: olyan számok, mint „egy szünet Most nemmel ért véget” vagy „megvolt egy séta”, ") +
                            tr("with a random id and the day since install. No names, app names or health data. Off any time in Settings.", "egy véletlen azonosítóval és a telepítés óta eltelt napok számával. Se név, se appnév, se egészségadat. A Beállításokban bármikor kikapcsolhatod."),
                        tr("An account, only if you log in (Google or an e-mail code): your name and e-mail, your companion and level, ", "Fiók, csak ha belépsz (Google-lel vagy e-mailes kóddal): a neved és az e-mail-címed, a társad és a szinted, ") +
                            tr("and a backup of your settings, camp, acorns and history so you can restore them. ", "valamint egy mentés a beállításaidról, a táborodról, a makkjaidról és az előzményeidről, hogy vissza tudd állítani őket. ") +
                            tr("Never your health readings or which apps you use. Delete it any time: Settings → Account → Delete account.", "Az egészségadataid és az, hogy mely appokat használod, soha nem kerül bele. Bármikor törölheted: Beállítások → Fiók → Fiók törlése."),
                    ).forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
                    Text(
                        tr("To delete everything on this phone: Settings → Start over, or uninstall tend. To delete your account and backup: Settings → Account → Delete account. Health Connect access can be turned off any time in Health Connect.", "Ha mindent törölnél erről a telefonról: Beállítások → Újrakezdés, vagy távolítsd el a tendet. A fiókod és a mentésed törlése: Beállítások → Fiók → Fiók törlése. A Health Connect-hozzáférést bármikor kikapcsolhatod a Health Connectben."),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    PrimaryButton(tr("Got it", "Értem"), ::finish, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}
