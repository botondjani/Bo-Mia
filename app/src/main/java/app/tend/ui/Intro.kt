package app.tend.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tend.core.Avatar
import app.tend.core.BudgetConfig
import app.tend.core.DayState
import app.tend.core.Tasks
import app.tend.core.Mood
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class Slide(val title: String, val body: String)

/**
 * The first thing a new user sees: Bo in a lab coat explains tend in two short slides, each with
 * a small live demo. Bo and the speech bubble stay put; the demos slide.
 */
@Composable
fun IntroSlides(avatar: Avatar, freeMinutes: Int, onDone: () -> Unit) {
    val name = avatar.displayName
    val slides = listOf(
        Slide("Hi! I'm $name.", "Open an app you picked and I'll pop up for a breath first. Often that's all it takes to choose something better."),
        Slide("Live a little, earn it back", "You get $freeMinutes free minutes a day. A walk, a workout or a book earns more, and acorns for our camp by the river."),
    )
    val pager = rememberPagerState { slides.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == slides.lastIndex

    Column(Modifier.fillMaxSize().background(Brand.Sage).padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("tend", style = MaterialTheme.typography.titleLarge, color = Brand.Moss, modifier = Modifier.weight(1f))
            if (!last) Text(
                "Skip", style = MaterialTheme.typography.labelLarge, color = Brand.Muted,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).tappable(onClick = onDone).padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
            Box(Modifier.fillMaxSize().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                val active = pager.currentPage == page
                if (page == 0) PauseDemo(active, name) else CampDemo(active, avatar)
            }
        }
        // Bo explains, pointer in hand.
        Row(verticalAlignment = Alignment.CenterVertically) {
            CharacterView(avatar, Mood.HAPPY, 112.dp, scientist = true)
            SpeechBubble(Modifier.weight(1f)) {
                AnimatedContent(pager.currentPage, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "bubble") { p ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(brandTend(slides[p].title), style = MaterialTheme.typography.titleLarge)
                        Text(brandTend(slides[p].body), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.Center) {
            repeat(slides.size) { i ->
                val w by animateFloatAsState(if (i == pager.currentPage) 22f else 8f, label = "dot")
                Box(Modifier.padding(horizontal = 3.dp).height(8.dp).width(w.dp).clip(CircleShape)
                    .background(if (i == pager.currentPage) Brand.Moss else Brand.Mint.copy(alpha = 0.4f)))
            }
        }
        PrimaryButton(
            if (last) "Let's set it up" else "Next",
            { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.padding(vertical = 18.dp),
        )
    }
}

/** A white bubble with a small tail pointing left, at the character. */
@Composable
private fun SpeechBubble(modifier: Modifier, content: @Composable () -> Unit) = Row(modifier, verticalAlignment = Alignment.CenterVertically) {
    Canvas(Modifier.size(10.dp, 18.dp)) {
        drawPath(Path().apply { moveTo(size.width, 0f); lineTo(0f, size.height / 2); lineTo(size.width, size.height); close() }, Brand.Card)
    }
    Box(Modifier.weight(1f).surface(radius = 22.dp).padding(horizontal = 16.dp, vertical = 14.dp)) { content() }
}

/** Counts up while active; restarts when the slide comes back. */
@Composable
private fun ticker(active: Boolean, stepMs: Long, steps: Int): Int {
    var t by remember { mutableIntStateOf(0) }
    LaunchedEffect(active) {
        t = 0
        while (active) { delay(stepMs); t = (t + 1) % steps }
    }
    return t
}

/** The pause card, counting down on a loop. */
@Composable
private fun PauseDemo(active: Boolean, name: String) {
    val t = ticker(active, 1000, 8)
    val left = (5 - t).coerceAtLeast(0)
    Column(
        Modifier.fillMaxWidth().surface(radius = 28.dp).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Instagram · 3rd open today", style = MaterialTheme.typography.bodyMedium)
        Text(brandTend("We tend to open it without thinking. You just stopped to think."), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Reply", "Look up", "Bored", "Habit").forEachIndexed { i, a ->
                Text(
                    a, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), maxLines = 1, textAlign = TextAlign.Center,
                    color = if (i == 2 && t >= 2) Color.White else Brand.Moss,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50))
                        .background(if (i == 2 && t >= 2) Brand.Moss else Brand.Sage).padding(vertical = 8.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(16.dp)).background(Brand.Moss), contentAlignment = Alignment.Center) {
            Text("Do something with $name instead", style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
        Box(Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(16.dp)).background(Brand.Mint.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            Text("Not now", style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
        }
        Text(
            if (left > 0) "Open Instagram in $left s" else "Open Instagram",
            style = MaterialTheme.typography.labelLarge, color = if (left > 0) Brand.Muted else Brand.Moss,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** The camp filling up, thing by thing, the way acorns buy it. */
@Composable
private fun CampDemo(active: Boolean, avatar: Avatar) {
    val steps = listOf(
        "campfire" to (0 to "Day 1: a campfire"),
        "log_bench" to (1 to "Day 3: a log bench"),
        "tent" to (3 to "Week 1: a tent"),
        "string_lights" to (9 to "String lights"),
        "hammock" to (5 to "A hammock"),
        "canoe" to (8 to "Week 3: a canoe"),
    )
    val t = ticker(active, 1000, steps.size + 2).coerceAtMost(steps.size)
    val placed = steps.take(t).associate { (thing, at) -> at.first to thing }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(24.dp))) { drawClearing(avatar, placed) }
        Text(
            if (t == 0) "Your clearing, day one" else steps[t - 1].second.second,
            style = MaterialTheme.typography.titleMedium, color = Brand.Moss, modifier = Modifier.padding(top = 12.dp),
        )
    }
}
