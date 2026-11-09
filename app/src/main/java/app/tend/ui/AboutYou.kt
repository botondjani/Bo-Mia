package app.tend.ui

import androidx.compose.foundation.background
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
val AGE_BANDS = listOf("u18" to "Under 18", "18-24" to "18–24", "25-34" to "25–34", "35-44" to "35–44", "45+" to "45+")
val GENDERS = listOf("woman" to "Woman", "man" to "Man", "other" to "Another", "none" to "Rather not say")

/**
 * A little about you, all optional: your town (for the camp's weather; searched by name, never the
 * phone's location), an age range and a gender (only ever counted, with consent; never shown).
 */
@Composable
fun AboutYou(
    place: WeatherService.Place?, age: String?, gender: String?,
    onPlace: (WeatherService.Place?) -> Unit, onAge: (String?) -> Unit, onGender: (String?) -> Unit,
) = Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("YOUR TOWN", style = MaterialTheme.typography.labelSmall)
    TownPicker(place, onPlace)
    Text("AGE", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
    Chips(AGE_BANDS, age, onAge)
    Text("YOU ARE", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
    Chips(GENDERS, gender, onGender)
    Text(
        "Age and gender only help us see who tend helps, in anonymous counts (if you say yes to those). Friends never see them.",
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted,
    )
}

/** Your town: the one chosen (with a way to change it), or a search by name. */
@Composable
internal fun TownPicker(place: WeatherService.Place?, onPlace: (WeatherService.Place?) -> Unit) {
    var searching by rememberSaveable { mutableStateOf(place == null) }
    if (place != null && !searching) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.Card).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(place.name, style = MaterialTheme.typography.titleSmall)
                Text("The camp has its weather", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
            }
            Text("Change", style = MaterialTheme.typography.labelLarge, color = Brand.Moss, modifier = Modifier.clip(RoundedCornerShape(10.dp)).tappable { searching = true }.padding(8.dp))
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
        placeholder = { Text("Search your town") }, singleLine = true,
        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
    )
    when {
        results.isNotEmpty() -> Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brand.Card)) {
            results.forEach { p ->
                Text(
                    p.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1,
                    modifier = Modifier.fillMaxWidth().tappable { onPlace(p); searching = false; query = "" }.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
        query.trim().length >= 2 && !looking -> Text("No town found (or no internet). Try another spelling.", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
        else -> Text("For the weather in the camp. We never ask where your phone is.", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Brand.Muted)
    }
    if (place != null) Text(
        "Keep ${place.name}", style = MaterialTheme.typography.labelLarge, color = Brand.Moss,
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
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (on) Brand.Moss else Brand.Card)
                        .tappable { onPick(if (on) null else value) }.padding(vertical = 10.dp),
                )
            }
            repeat(3 - row.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
        }
    }
}
