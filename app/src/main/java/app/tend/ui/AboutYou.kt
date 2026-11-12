package app.tend.ui

import app.tend.core.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tend.data.WeatherService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Age ranges and genders to pick from: stored values to labels. */
val AGE_BANDS = listOf("u18" to tr("Under 18", "18 alatt"), "18-24" to "18–24", "25-34" to "25–34", "35-44" to "35–44", "45+" to "45+")
val GENDERS = listOf("woman" to tr("Woman", "Nő"), "man" to tr("Man", "Férfi"), "other" to tr("Another", "Egyéb"), "none" to tr("Rather not say", "Inkább nem mondom meg"))

/**
 * A little about you, all optional: your town (for the camp's weather; searched by name, never the
 * phone's location), an age range and a gender (only ever counted, with consent; never shown).
 */
@Composable
fun AboutYou(
    place: WeatherService.Place?, age: String?, gender: String?,
    onPlace: (WeatherService.Place?) -> Unit, onAge: (String?) -> Unit, onGender: (String?) -> Unit,
) = Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(tr("YOUR TOWN", "A VÁROSOD"), style = MaterialTheme.typography.labelSmall)
    TownPicker(place, onPlace)
    Text(tr("AGE", "KOROD"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
    Chips(AGE_BANDS, age, onAge)
    Text(tr("YOU ARE", "NEMED"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
    Chips(GENDERS, gender, onGender)
    Text(
        tr("Age and gender only help us see who tend helps, in anonymous counts (if you say yes to those). Friends never see them.", "A kor és a nem csak abban segít, hogy lássuk, kiknek segít a tend, névtelen számokban (ha azokra igent mondasz). A barátaid sosem látják."),
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted,
    )
}

/** Your town: the one chosen (with a way to change it), or a search by name. */
@Composable
internal fun TownPicker(place: WeatherService.Place?, onPlace: (WeatherService.Place?) -> Unit) {
    var searching by rememberSaveable { mutableStateOf(place == null) }
    if (place != null && !searching) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.Sage).border(1.dp, Brand.Mint, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(place.name, style = MaterialTheme.typography.titleSmall)
                Text(tr("The camp has its weather", "A táborban az itteni idő van"), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
            }
            Text(tr("Change", "Módosítás"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss, modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable { searching = true }.padding(8.dp))
        }
        return
    }
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<WeatherService.Place>()) }
    var looking by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        if (query.trim().length < 2) { results = emptyList(); return@LaunchedEffect }
        delay(400) // typing: wait for a pause
        looking = true
        results = withContext(Dispatchers.IO) { WeatherService.search(query) }
        looking = false
    }
    OutlinedTextField(
        value = query, onValueChange = { query = it },
        placeholder = { Text(tr("Search your town", "Keresd meg a városod")) }, singleLine = true,
        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
    )
    when {
        results.isNotEmpty() -> Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.Sage).border(1.dp, Brand.Mint, RoundedCornerShape(16.dp))) {
            results.forEach { p ->
                Text(
                    p.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1,
                    modifier = Modifier.fillMaxWidth().tappable { onPlace(p); searching = false; query = "" }.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
        query.trim().length >= 2 && !looking -> Text(tr("No town found (or no internet). Try another spelling.", "Nem találtunk ilyen várost (vagy nincs net). Próbáld más írásmóddal."), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
        else -> Text(tr("For the weather in the camp. We never ask where your phone is.", "A tábor időjárásához. Sosem kérdezzük, hol van a telefonod."), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
    }
    if (place != null) Text(
        tr("Keep ${place.name}", "Maradjon ${place.name}"), style = MaterialTheme.typography.labelLarge, color = Brand.Moss,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable { searching = false; query = "" }.padding(vertical = 6.dp),
    )
}

/** One choice of a few, as chips; tapping the chosen one again clears it. */
@Composable
private fun Chips(options: List<Pair<String, String>>, chosen: String?, onPick: (String?) -> Unit) {
    options.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (value, label) ->
                val on = value == chosen
                Text(
                    label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), textAlign = TextAlign.Center,
                    color = if (on) Color.White else Brand.Moss,
                    // Picked: filled; the others a clear light green with an edge (not white on the white card).
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (on) Brand.Moss else Brand.Sage)
                        .border(1.dp, if (on) Brand.Moss else Brand.Mint, RoundedCornerShape(14.dp))
                        .tappable { onPick(if (on) null else value) }.padding(vertical = 10.dp),
                )
            }
            repeat(3 - row.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
        }
    }
}
