package app.tend.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import app.tend.core.Avatar
import app.tend.core.Mood
import app.tend.core.Plant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class Slide(val title: String, val body: String)

/**
 * The first thing a new user sees: Bo in a lab coat explains tend in four short slides, each with
 * a small live demo. Bo and the speech bubble stay put; the demos slide.
 */
@Composable
fun IntroSlides(avatar: Avatar, freeMinutes: Int, onDone: () -> Unit) {
    val name = avatar.displayName
    val slides = listOf(
        Slide("Hi! I'm $name.", "We tend to scroll more than we mean to. That's not you being weak: apps are built that way. Let me show you how tend helps."),
        Slide("A breath before you scroll", "Open an app you picked and I'll pop up for a few seconds. Often that's all it takes to choose something better."),
        Slide("Live a little, earn minutes", "You get $freeMinutes free minutes a day. A walk, a workout or a good book earns more. Steps and workouts count by themselves."),
        Slide("What you don't use, grows", "At midnight, unused minutes become a plant in our garden. Week by week, our home grows too."),
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
                when (page) {
                    0 -> ScrollDemo(active)
                    1 -> PauseDemo(active, name)
                    2 -> EarnDemo(active, freeMinutes)
                    else -> GardenDemo(active, avatar)
                }
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

/**
 * A phone with an endless feed and a clock that keeps going up. The feed moves on every frame in the
 * draw phase only (no recomposition), so it stays smooth while the rest of the screen settles.
 */
@Composable
private fun ScrollDemo(active: Boolean) {
    val feed = androidx.compose.animation.core.rememberInfiniteTransition(label = "feed")
    val cards by feed.animateFloat(
        0f, 40f,
        androidx.compose.animation.core.infiniteRepeatable(tween(40 * 1100, easing = androidx.compose.animation.core.LinearEasing)),
        label = "cards",
    )
    val tints = remember { listOf(Brand.Sky, Brand.Coral, Brand.Sunny, Brand.Mint).map { it.copy(alpha = 0.55f) } }
    val line = remember { Brand.Muted.copy(alpha = 0.25f) }
    val t = ticker(active, 1000, 100_000)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(170.dp, 300.dp).clip(RoundedCornerShape(30.dp)).background(Brand.Ink).padding(8.dp)) {
            Canvas(Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)).background(Color(0xFFF7F8F5))) {
                val card = 92.dp.toPx()
                val pos = if (active) cards else 0f
                var i = pos.toInt()
                var y = -(pos - i) * card
                val inset = 10.dp.toPx()
                val radius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                while (y < size.height) {
                    drawRoundRect(tints[i % 4], Offset(inset, y + 8.dp.toPx()), androidx.compose.ui.geometry.Size(size.width - 2 * inset, 52.dp.toPx()), radius)
                    drawRoundRect(line, Offset(inset, y + 66.dp.toPx()), androidx.compose.ui.geometry.Size((size.width - 2 * inset) * (0.5f + 0.1f * (i % 4)), 8.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                    y += card; i++
                }
            }
        }
        val minutes = 160 + t
        Text("${minutes / 60}h ${"%02d".format(minutes % 60)}m today", style = MaterialTheme.typography.titleMedium, color = Brand.Coral, modifier = Modifier.padding(top = 12.dp))
    }
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
            listOf("Messages", "Something", "Just bored").forEachIndexed { i, a ->
                Text(
                    a, style = MaterialTheme.typography.labelLarge, maxLines = 1, textAlign = TextAlign.Center,
                    color = if (i == 2 && t >= 2) Color.White else Brand.Moss,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50))
                        .background(if (i == 2 && t >= 2) Brand.Moss else Brand.Sage).padding(vertical = 8.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(16.dp)).background(Brand.Moss), contentAlignment = Alignment.Center) {
            Text("Read with $name instead", style = MaterialTheme.typography.labelLarge, color = Color.White)
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

/** Three activities ticking off one by one while the minutes climb. */
@Composable
private fun EarnDemo(active: Boolean, freeMinutes: Int) {
    val t = ticker(active, 1100, 6)
    val items = listOf(Triple("outside", "Walk outside", 20), Triple("workout", "Workout or run", 30), Triple("read", "Read", 15))
    val done = t.coerceAtMost(items.size)
    val target = freeMinutes + items.take(done).sumOf { it.third }
    val shown by animateFloatAsState(target.toFloat(), tween(600), label = "minutes")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().surface(radius = 24.dp).padding(18.dp), verticalAlignment = Alignment.Bottom) {
            Text("${shown.toInt()}", style = MaterialTheme.typography.displayLarge, color = Brand.Moss)
            Text(" min today", style = MaterialTheme.typography.titleMedium, color = Brand.Muted, modifier = Modifier.padding(bottom = 10.dp))
        }
        items.forEachIndexed { i, (icon, title, reward) ->
            val isDone = i < done
            Row(Modifier.fillMaxWidth().surface(radius = 20.dp).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isDone) Box(Modifier.size(40.dp).clip(CircleShape).background(Brand.Moss), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
                } else TendIcons.Badge(icon, 40.dp)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = if (isDone) Brand.Muted else Brand.Ink, modifier = Modifier.weight(1f))
                Text(if (isDone) "+$reward earned" else "+$reward", style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
            }
        }
    }
}

/** Plants appear one night at a time. */
@Composable
private fun GardenDemo(active: Boolean, avatar: Avatar) {
    val t = ticker(active, 900, 8)
    val all = listOf(Plant.SPROUT, Plant.TULIP, Plant.SUNFLOWER, Plant.TULIP, Plant.SUNFLOWER, Plant.SPROUT)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GardenScene(avatar, all.take(t.coerceAtMost(all.size)), if (t >= all.size) 1 else 0)
        Text(
            if (t == 0) "Tonight" else "Night ${t.coerceAtMost(all.size)}",
            style = MaterialTheme.typography.titleMedium, color = Brand.Moss, modifier = Modifier.padding(top = 12.dp),
        )
    }
}
