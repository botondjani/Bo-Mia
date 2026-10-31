package app.tend.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tend.core.Avatar
import app.tend.core.Garden
import app.tend.core.Mood
import app.tend.core.Plant
import app.tend.core.Recap

private val Soil = Color(0xFF7A5A43)
private val Wall = Color(0xFFFFF8EC)
private val DarkWindow = Color(0xFF4A5A66)

/** The character's home and garden: one plant per good day, the home grows weekly. */
@Composable
fun GardenScene(avatar: Avatar, plants: List<Plant>, homeStage: Int, modifier: Modifier = Modifier, mood: Mood = Mood.HAPPY) =
    Canvas(modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(24.dp))) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.62f
        drawRect(Brand.Sky.copy(alpha = 0.35f), size = Size(w, groundY))
        drawRect(Brand.Mint, topLeft = Offset(0f, groundY), size = Size(w, h - groundY))
        drawCircle(Brand.Sunny, radius = h * 0.07f, center = Offset(w * 0.1f, h * 0.14f))

        if (homeStage >= 2) { // a tree of your own
            drawRect(Soil, Offset(w * 0.13f, groundY - h * 0.2f), Size(w * 0.025f, h * 0.2f))
            drawCircle(Brand.Moss, h * 0.12f, Offset(w * 0.142f, groundY - h * 0.27f))
            drawCircle(Brand.Moss, h * 0.09f, Offset(w * 0.09f, groundY - h * 0.2f))
            drawCircle(Brand.Moss, h * 0.09f, Offset(w * 0.2f, groundY - h * 0.2f))
        }

        // House
        val hx = w * 0.6f
        val hw = w * 0.26f
        val hh = h * 0.26f
        drawRoundRect(Wall, Offset(hx, groundY - hh), Size(hw, hh), CornerRadius(8f))
        drawPath(Path().apply {
            moveTo(hx - hw * 0.08f, groundY - hh); lineTo(hx + hw / 2, groundY - hh - h * 0.15f)
            lineTo(hx + hw * 1.08f, groundY - hh); close()
        }, Brand.Coral)
        drawRoundRect(Brand.Moss, Offset(hx + hw * 0.62f, groundY - hh * 0.62f), Size(hw * 0.22f, hh * 0.62f), CornerRadius(6f))
        val lit = homeStage >= 1
        if (lit) drawCircle(Brand.Sunny.copy(alpha = 0.35f), hw * 0.22f, Offset(hx + hw * 0.3f, groundY - hh * 0.55f))
        drawRoundRect(if (lit) Brand.Sunny else DarkWindow, Offset(hx + hw * 0.16f, groundY - hh * 0.75f), Size(hw * 0.28f, hh * 0.38f), CornerRadius(4f))

        if (homeStage >= 1) { // bench
            val bx = w * 0.45f
            drawRect(Soil, Offset(bx, groundY - h * 0.07f), Size(w * 0.12f, h * 0.02f))
            drawRect(Soil, Offset(bx, groundY - h * 0.12f), Size(w * 0.12f, h * 0.02f))
            drawRect(Soil, Offset(bx + w * 0.01f, groundY - h * 0.07f), Size(w * 0.01f, h * 0.07f))
            drawRect(Soil, Offset(bx + w * 0.1f, groundY - h * 0.07f), Size(w * 0.01f, h * 0.07f))
        }

        // Plants in two rows on the lawn, newest last; only the latest ones fit.
        val perRow = 10
        val shown = plants.takeLast(perRow * 2)
        shown.forEachIndexed { i, plant ->
            val row = i / perRow
            val col = i % perRow
            val x = w * (0.06f + col * 0.0975f) + if (row == 1) w * 0.045f else 0f
            val y = groundY + (h - groundY) * (0.55f + row * 0.38f)
            drawPlant(plant, Offset(x, y), h * 0.16f)
        }

        // Character in front of the house
        val c = h * 0.3f
        drawIntoCanvas {
            it.nativeCanvas.save()
            it.nativeCanvas.translate(w * 0.25f, groundY - c * 0.8f)
            app.tend.ui.CharacterPainter.draw(it.nativeCanvas, avatar, mood, c)
            it.nativeCanvas.restore()
        }
    }

/** Draws a plant whose base sits at [base]; [s] is its height. */
fun DrawScope.drawPlant(plant: Plant, base: Offset, s: Float) {
    val stemTop = base.copy(y = base.y - s * if (plant == Plant.SPROUT) 0.45f else 0.8f)
    drawLine(Brand.Moss, base, stemTop, strokeWidth = s * 0.07f, cap = StrokeCap.Round)
    translate(base.x, base.y) {
        drawOval(Brand.Moss, Offset(-s * 0.3f, -s * 0.38f), Size(s * 0.28f, s * 0.14f))
        drawOval(Brand.Moss, Offset(s * 0.02f, -s * 0.46f), Size(s * 0.28f, s * 0.14f))
    }
    when (plant) {
        Plant.SPROUT -> Unit
        Plant.TULIP -> drawPath(Path().apply {
            moveTo(stemTop.x - s * 0.16f, stemTop.y - s * 0.2f)
            lineTo(stemTop.x - s * 0.08f, stemTop.y - s * 0.08f); lineTo(stemTop.x, stemTop.y - s * 0.2f)
            lineTo(stemTop.x + s * 0.08f, stemTop.y - s * 0.08f); lineTo(stemTop.x + s * 0.16f, stemTop.y - s * 0.2f)
            quadraticTo(stemTop.x + s * 0.16f, stemTop.y + s * 0.06f, stemTop.x, stemTop.y + s * 0.06f)
            quadraticTo(stemTop.x - s * 0.16f, stemTop.y + s * 0.06f, stemTop.x - s * 0.16f, stemTop.y - s * 0.2f)
            close()
        }, Brand.Coral)
        Plant.SUNFLOWER -> {
            for (a in 0 until 10) {
                val angle = Math.toRadians(a * 36.0)
                drawCircle(
                    Brand.Sunny, s * 0.09f,
                    Offset(stemTop.x + (s * 0.15f * Math.cos(angle)).toFloat(), stemTop.y + (s * 0.15f * Math.sin(angle)).toFloat()),
                )
            }
            drawCircle(Soil, s * 0.11f, stemTop)
        }
    }
}

@Composable
fun PlantIcon(plant: Plant, size: androidx.compose.ui.unit.Dp) = Canvas(Modifier.size(size)) {
    drawPlant(plant, Offset(this.size.width / 2, this.size.height * 0.95f), this.size.height * 0.85f)
}

@Composable
fun GardenScreen(avatar: Avatar, plants: List<Plant>, daysTended: Int, onBack: () -> Unit) = Column(
    Modifier.fillMaxSize().background(Brand.Sage).verticalScroll(rememberScrollState()).padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
) {
    TextButton(onClick = onBack) { Text("← Today") }
    Text("${avatar.displayName}'s home & garden", style = MaterialTheme.typography.headlineMedium)
    Text(
        if (plants.isEmpty()) "Your first plant grows tonight from the minutes you don't use."
        else "${plants.size} plant${if (plants.size == 1) "" else "s"}, one for every day you left some minutes unused.",
        style = MaterialTheme.typography.bodyLarge, color = Brand.Muted,
    )
    GardenScene(avatar, plants, Garden.homeStage(daysTended))
    Card {
        Text("HOW IT GROWS", style = MaterialTheme.typography.labelSmall)
        PlantLegend(Plant.SPROUT, "A sprout", "1–14 unused minutes")
        PlantLegend(Plant.TULIP, "A tulip", "15–29 unused minutes")
        PlantLegend(Plant.SUNFLOWER, "A sunflower", "30 or more")
    }
    HomeStages(daysTended)
}

@Composable
private fun PlantLegend(plant: Plant, title: String, body: String) = Row(verticalAlignment = Alignment.CenterVertically) {
    PlantIcon(plant, 40.dp)
    Spacer(Modifier.width(12.dp))
    Column {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Week 1 / 2 / 4 milestones of the home, the current one highlighted. */
@Composable
fun HomeStages(daysTended: Int) = Card {
    val stage = Garden.homeStage(daysTended)
    Text("YOUR HOME GROWS WITH YOU", style = MaterialTheme.typography.labelSmall)
    Text("Day ${daysTended + 1} of tending", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
        listOf("Week 1" to "first sprouts", "Week 2" to "lit window, bench", "Week 4" to "a tree of your own")
            .forEachIndexed { i, (week, what) ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                        .background(if (i <= stage) Brand.Mint.copy(alpha = 0.3f) else Brand.Sage)
                        .border(2.dp, if (i == stage) Brand.Moss else Color.Transparent, RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(week, style = MaterialTheme.typography.titleMedium, color = if (i <= stage) Brand.Moss else Brand.Muted)
                    Text(what, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            }
    }
}

@Composable
fun RecapScreen(avatar: Avatar, recap: Recap, plants: List<Plant>, daysTended: Int, onGoodNight: () -> Unit) = Column(
    Modifier.fillMaxSize().background(Brand.Sage).verticalScroll(rememberScrollState()).padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
) {
    Text("TONIGHT'S RECAP", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
    Text(
        if (recap.gotBackMinutes > 0) "You got back ${duration(recap.gotBackMinutes)}" else "Today used it all",
        style = MaterialTheme.typography.headlineLarge,
    )
    Text(
        if (recap.gotBackMinutes > 0) "Minutes you could have scrolled, and didn't."
        else "That happens. No plant tonight, and tomorrow's a fresh start.",
        style = MaterialTheme.typography.bodyLarge, color = Brand.Muted,
    )

    GardenScene(avatar, plants + listOfNotNull(recap.tonightsPlant), Garden.homeStage(daysTended), mood = Mood.HAPPY)
    recap.tonightsPlant?.let { plant ->
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlantIcon(plant, 48.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    "A new ${plant.displayName} grows tonight from ${recap.gotBackMinutes} unused minutes.",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Stat("%,d".format(recap.steps), "steps", Modifier.weight(1f))
        Stat("${recap.exerciseMinutes} min", "exercise", Modifier.weight(1f))
        Stat("${recap.readingMinutes} min", "reading", Modifier.weight(1f))
    }

    if (recap.worthItAnswers > 0) Card {
        Text("WORTH IT? TODAY", style = MaterialTheme.typography.labelSmall)
        Text(
            if (recap.notWorthIt == 0) "Every scroll you rated felt worth it. Nice."
            else "${recap.notWorthIt} of ${recap.worthItAnswers} scrolls didn't feel worth it." +
                (recap.notWorthAfterHour?.let { " All of them after ${hour(it)}." } ?: ""),
            style = MaterialTheme.typography.bodyLarge,
        )
    }

    HomeStages(daysTended)
    PrimaryButton("Good night, ${avatar.displayName}", onGoodNight, modifier = Modifier.padding(vertical = 12.dp))
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) = Column(
    modifier.clip(RoundedCornerShape(20.dp)).background(Brand.Card).padding(vertical = 14.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Text(value, style = MaterialTheme.typography.titleLarge, color = Brand.Moss)
    Text(label, style = MaterialTheme.typography.bodyMedium)
}

private fun duration(minutes: Int) = if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"

private fun hour(h: Int) = when {
    h == 0 -> "midnight"
    h < 12 -> "$h am"
    h == 12 -> "noon"
    else -> "${h - 12} pm"
}
