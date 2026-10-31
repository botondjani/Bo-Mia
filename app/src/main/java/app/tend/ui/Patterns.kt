package app.tend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tend.core.Avatar
import app.tend.core.Mood
import app.tend.core.WeeklyPatterns
import app.tend.core.WorthSplit
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PatternsScreen(avatar: Avatar, patterns: WeeklyPatterns, appLabel: (String) -> String) = Column(
    Modifier.fillMaxSize().background(Brand.Sage).verticalScroll(rememberScrollState()).padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
) {
    Text("Patterns", style = MaterialTheme.typography.headlineMedium)
    Text("The last 7 days, as they were. No judgement.", style = MaterialTheme.typography.bodyMedium)

    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CharacterView(avatar, Mood.HAPPY, 72.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                patterns.insight ?: "Answer \"Worth it?\" a few more times and ${avatar.displayName} will spot your patterns here.",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }

    Card {
        Text("THIS WEEK", style = MaterialTheme.typography.labelSmall)
        Text("You got back ${hoursMinutes(patterns.gotBackMinutes)}", style = MaterialTheme.typography.titleLarge, color = Brand.Moss)
        Text("Screen time in paused apps, per day", style = MaterialTheme.typography.bodyMedium)
        DayBars(patterns)
    }

    val parts = patterns.byDayPart.filterValues { it.total > 0 }
    if (parts.isNotEmpty()) Card {
        Text("WORTH IT? BY TIME OF DAY", style = MaterialTheme.typography.labelSmall)
        parts.forEach { (part, split) -> SplitRow(part.label, split) }
        Legend()
    }

    if (patterns.byApp.isNotEmpty()) Card {
        Text("WORTH IT? BY APP", style = MaterialTheme.typography.labelSmall)
        patterns.byApp.forEach { (pkg, split) -> SplitRow(appLabel(pkg), split) }
    }

    if (patterns.lookingFor.isNotEmpty()) Card {
        Text("WHAT YOU WERE LOOKING FOR", style = MaterialTheme.typography.labelSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            patterns.lookingFor.entries.sortedByDescending { it.value }.forEach { (answer, n) -> Pill("$answer · $n") }
        }
    }
}

@Composable
private fun DayBars(patterns: WeeklyPatterns) {
    val max = (patterns.days.maxOfOrNull { it.usedMinutes } ?: 0).coerceAtLeast(30)
    Row(
        Modifier.fillMaxWidth().height(140.dp).padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        patterns.days.forEachIndexed { i, d ->
            val today = i == patterns.days.lastIndex
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${d.usedMinutes}", style = MaterialTheme.typography.bodyMedium)
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier.fillMaxWidth().fillMaxHeight((d.usedMinutes.toFloat() / max).coerceIn(0.03f, 1f))
                            .clip(RoundedCornerShape(8.dp)).background(if (today) Brand.Moss else Brand.Mint),
                    )
                }
                Text(
                    d.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(2),
                    style = MaterialTheme.typography.labelSmall, color = if (today) Brand.Moss else Brand.Muted,
                )
            }
        }
    }
}

@Composable
private fun SplitRow(label: String, s: WorthSplit) = Column(Modifier.padding(top = 8.dp)) {
    Row {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            when {
                s.not > 0 -> "${s.not} of ${s.total} not worth it"
                s.kindOf > 0 -> "${s.yes} of ${s.total} fully worth it"
                else -> "all worth it"
            },
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End,
        )
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp).height(12.dp).clip(CircleShape)) {
        if (s.yes > 0) Box(Modifier.weight(s.yes.toFloat()).fillMaxHeight().background(Brand.Moss))
        if (s.kindOf > 0) Box(Modifier.weight(s.kindOf.toFloat()).fillMaxHeight().background(Brand.Sunny))
        if (s.not > 0) Box(Modifier.weight(s.not.toFloat()).fillMaxHeight().background(Brand.Coral))
    }
}

@Composable
private fun Legend() = Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
    listOf(Brand.Moss to "Yes", Brand.Sunny to "Kind of", Brand.Coral to "Not really").forEach { (c, l) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(c))
            Spacer(Modifier.width(6.dp))
            Text(l, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun hoursMinutes(m: Int) = if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min"

enum class Tab(val label: String) { TODAY("Today"), GARDEN("Home & garden"), PATTERNS("Patterns") }

@Composable
fun TabBar(selected: Tab, onSelect: (Tab) -> Unit) = Row(
    Modifier.fillMaxWidth().background(Brand.Card).padding(horizontal = 12.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
) {
    Tab.entries.forEach { tab ->
        val on = tab == selected
        Text(
            tab.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (on) Brand.Moss else Brand.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                .background(if (on) Brand.Mint.copy(alpha = 0.3f) else Color.Transparent)
                .clickable { onSelect(tab) }.padding(vertical = 12.dp),
        )
    }
}
