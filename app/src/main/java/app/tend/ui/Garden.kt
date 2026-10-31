package app.tend.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tend.core.Avatar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import app.tend.core.Accessory
import app.tend.core.Garden
import androidx.compose.material.icons.filled.Settings
import app.tend.core.Mood
import app.tend.core.Plant
import app.tend.core.Recap

private val Soil = Color(0xFF7A5A43)
private val Wall = Color(0xFFFFF8EC)
private val DarkWindow = Color(0xFF4A5A66)

/** The character's home and garden: one plant per good day, the home grows weekly. */
@Composable
fun GardenScene(
    avatar: Avatar,
    plants: List<Plant>,
    homeStage: Int,
    modifier: Modifier = Modifier,
    mood: Mood = Mood.HAPPY,
    /** Tonight's plant, drawn see-through in the next free spot. */
    preview: Plant? = null,
    /** Night or day; by default night from the evening hour until 5:00, in either theme. */
    night: Boolean? = null,
) {
    val accessories = LocalAccessories.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val isNight = night ?: androidx.compose.runtime.remember { Garden.isNight(app.tend.data.TendStore(context).eveningFrom) }
    Canvas(modifier.fillMaxWidth().aspectRatio(1.3f).clip(RoundedCornerShape(28.dp))) {
        drawGarden(avatar, plants, homeStage, mood, preview, accessories, isNight)
    }
}

/** The garden scene; shared by the garden page and the 2x2 widget. */
fun DrawScope.drawGarden(
    avatar: Avatar,
    plants: List<Plant>,
    homeStage: Int,
    mood: Mood = Mood.HAPPY,
    preview: Plant? = null,
    accessories: Set<Accessory> = emptySet(),
    /** Evening: the same garden at night, with the moon, stars and a lit window. */
    night: Boolean = false,
) {
    val w = size.width
    val h = size.height
    val groundY = h * 0.5f
    val grass = if (night) Color(0xFF3E7A5B) else Brand.Mint

    if (night) { // Night sky, moon, a few stars
        drawRect(Brush.verticalGradient(listOf(Color(0xFF1C2B47), Color(0xFF2B4A4A)), endY = groundY), size = Size(w, groundY))
        val moon = Offset(w * 0.12f, h * 0.12f)
        val r = h * 0.055f
        drawCircle(Color(0xFFF3EBC8).copy(alpha = 0.12f), r * 1.7f, moon)
        // A crescent: the disc minus a slightly larger disc shifted up and right.
        val crescent = Path().apply {
            op(
                Path().apply { addOval(androidx.compose.ui.geometry.Rect(moon, r)) },
                Path().apply { addOval(androidx.compose.ui.geometry.Rect(moon + Offset(r * 0.55f, -r * 0.35f), r * 0.95f)) },
                androidx.compose.ui.graphics.PathOperation.Difference,
            )
        }
        drawPath(crescent, Color(0xFFF3EBC8))
        for ((sx, sy, r) in listOf(Triple(0.3f, 0.08f, 1f), Triple(0.45f, 0.18f, 0.7f), Triple(0.58f, 0.06f, 0.9f), Triple(0.72f, 0.14f, 0.6f), Triple(0.9f, 0.07f, 1f), Triple(0.36f, 0.27f, 0.6f)))
            drawCircle(Color.White.copy(alpha = 0.85f), h * 0.008f * r, Offset(w * sx, h * sy))
    } else { // Sky, sun, clouds
        // Daytime stays light in either theme.
        drawRect(Brush.verticalGradient(listOf(Color(0xFFC3DFEF), Color(0xFFF1F4ED)), endY = groundY), size = Size(w, groundY))
        drawCircle(Brand.Sunny.copy(alpha = 0.35f), h * 0.085f, Offset(w * 0.12f, h * 0.12f))
        drawCircle(Brand.Sunny, h * 0.06f, Offset(w * 0.12f, h * 0.12f))
        cloud(Offset(w * 0.42f, h * 0.1f), h * 0.035f)
        cloud(Offset(w * 0.78f, h * 0.17f), h * 0.028f)
    }
    drawOval(grass.copy(alpha = 0.55f), Offset(-w * 0.2f, groundY - h * 0.08f), Size(w * 0.9f, h * 0.2f))
    drawRect(grass, topLeft = Offset(0f, groundY), size = Size(w, h - groundY))

    // Fence along the back of the lawn
    val fence = Color.White.copy(alpha = if (night) 0.4f else 0.85f)
    drawRect(fence, Offset(0f, groundY - h * 0.045f), Size(w, h * 0.012f))
    drawRect(fence, Offset(0f, groundY - h * 0.02f), Size(w, h * 0.012f))
    var x = w * 0.02f
    while (x < w) { drawRoundRect(fence, Offset(x, groundY - h * 0.07f), Size(w * 0.014f, h * 0.075f), CornerRadius(4f)); x += w * 0.06f }

    if (homeStage >= 2) { // a tree of your own
        drawRect(Soil, Offset(w * 0.1f, groundY - h * 0.17f), Size(w * 0.022f, h * 0.19f))
        drawCircle(Brand.Moss, h * 0.1f, Offset(w * 0.111f, groundY - h * 0.24f))
        drawCircle(Brand.Moss, h * 0.075f, Offset(w * 0.06f, groundY - h * 0.17f))
        drawCircle(Brand.Moss, h * 0.075f, Offset(w * 0.16f, groundY - h * 0.17f))
    }

    // House with chimney, door and window
    val hw = w * 0.3f
    val hh = h * 0.2f
    val hx = w * 0.62f
    drawRect(Soil, Offset(hx + hw * 0.7f, groundY - hh - h * 0.13f), Size(hw * 0.1f, h * 0.08f))
    drawRoundRect(Wall, Offset(hx, groundY - hh), Size(hw, hh), CornerRadius(10f))
    drawPath(Path().apply {
        moveTo(hx - hw * 0.1f, groundY - hh); lineTo(hx + hw / 2, groundY - hh - h * 0.13f)
        lineTo(hx + hw * 1.1f, groundY - hh); close()
    }, Brand.Coral)
    val doorX = hx + hw * 0.6f
    drawRoundRect(Brand.Moss, Offset(doorX, groundY - hh * 0.65f), Size(hw * 0.22f, hh * 0.65f), CornerRadius(8f))
    drawCircle(Brand.Sunny, hw * 0.018f, Offset(doorX + hw * 0.17f, groundY - hh * 0.32f))
    val lit = homeStage >= 1 || night
    val win = Offset(hx + hw * 0.14f, groundY - hh * 0.75f)
    val winSize = Size(hw * 0.28f, hh * 0.4f)
    if (lit) drawCircle(Brand.Sunny.copy(alpha = 0.35f), hw * 0.22f, Offset(win.x + winSize.width / 2, win.y + winSize.height / 2))
    drawRoundRect(if (lit) Brand.Sunny else Brand.Sky, win, winSize, CornerRadius(6f))
    drawLine(Wall, Offset(win.x + winSize.width / 2, win.y), Offset(win.x + winSize.width / 2, win.y + winSize.height), 4f)
    drawLine(Wall, Offset(win.x, win.y + winSize.height / 2), Offset(win.x + winSize.width, win.y + winSize.height / 2), 4f)

    // Path from the door to the bottom edge
    drawPath(Path().apply {
        moveTo(doorX, groundY); lineTo(doorX + hw * 0.22f, groundY)
        lineTo(doorX + hw * 0.5f, h); lineTo(doorX - hw * 0.25f, h); close()
    }, Color(0xFFE9DFC9))

    if (homeStage >= 1) { // bench by the house
        val bx = w * 0.4f
        drawRect(Soil, Offset(bx, groundY - h * 0.055f), Size(w * 0.13f, h * 0.018f))
        drawRect(Soil, Offset(bx, groundY - h * 0.095f), Size(w * 0.13f, h * 0.018f))
        drawRect(Soil, Offset(bx + w * 0.01f, groundY - h * 0.055f), Size(w * 0.012f, h * 0.06f))
        drawRect(Soil, Offset(bx + w * 0.105f, groundY - h * 0.055f), Size(w * 0.012f, h * 0.06f))
    }

    // Flower beds left of the path: 3 rows of 6, newest last; empty spots wait as soil.
    val perRow = 6
    val rows = 3
    val slots = perRow * rows
    val shown = plants.takeLast(if (preview != null) slots - 1 else slots)
    val bedLeft = w * 0.05f
    val bedRight = doorX - hw * 0.35f
    val step = (bedRight - bedLeft) / perRow
    for (r in 0 until rows) {
        val y = groundY + (h - groundY) * (0.36f + r * 0.28f)
        drawRoundRect(Soil.copy(alpha = 0.35f), Offset(bedLeft - step * 0.2f, y - h * 0.012f), Size(bedRight - bedLeft + step * 0.1f, h * 0.03f), CornerRadius(20f))
    }
    for (i in 0 until slots) {
        val r = i / perRow
        val c = i % perRow
        val base = Offset(bedLeft + step * (c + 0.4f), groundY + (h - groundY) * (0.36f + r * 0.28f))
        val plant = shown.getOrNull(i)
        when {
            plant != null -> drawPlant(plant, base, h * 0.13f)
            i == shown.size && preview != null -> drawIntoCanvas {
                it.saveLayer(androidx.compose.ui.geometry.Rect(Offset.Zero, size), androidx.compose.ui.graphics.Paint().apply { alpha = 0.4f })
                drawPlant(preview, base, h * 0.13f)
                it.restore()
            }
        }
    }

    // The companion on the lawn, in front of everything
    val cs = h * 0.24f
    drawIntoCanvas {
        it.nativeCanvas.save()
        // Standing at the fence, clear of the first flower bed.
        it.nativeCanvas.translate(w * 0.24f, groundY - cs * 0.88f)
        CharacterPainter.draw(it.nativeCanvas, avatar, mood, cs, accessories = accessories)
        it.nativeCanvas.restore()
    }
}

private fun DrawScope.cloud(c: Offset, r: Float) {
    val white = Color.White.copy(alpha = 0.9f)
    drawCircle(white, r, c)
    drawCircle(white, r * 0.8f, c + Offset(-r * 1.1f, r * 0.25f))
    drawCircle(white, r * 0.85f, c + Offset(r * 1.1f, r * 0.2f))
}

/** The garden as a bitmap with rounded corners, for the home screen widget. */
fun gardenBitmap(avatar: Avatar, plants: List<Plant>, homeStage: Int, accessories: Set<Accessory>, widthPx: Int, heightPx: Int, night: Boolean = false): android.graphics.Bitmap {
    val image = androidx.compose.ui.graphics.ImageBitmap(widthPx, heightPx)
    val canvas = androidx.compose.ui.graphics.Canvas(image)
    val size = Size(widthPx.toFloat(), heightPx.toFloat())
    canvas.clipPath(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(androidx.compose.ui.geometry.Rect(Offset.Zero, size), CornerRadius(widthPx * 0.16f))) })
    androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
        androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr, canvas, size,
    ) { drawGarden(avatar, plants, homeStage, accessories = accessories, night = night) }
    return image.asAndroidBitmap()
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
fun GardenScreen(
    avatar: Avatar,
    plants: List<Plant>,
    daysTended: Int,
    unusedToday: Int,
    worn: Set<Accessory> = emptySet(),
    onShelf: Set<Accessory> = emptySet(),
    onBack: () -> Unit,
    onOpenSettings: () -> Unit = {},
    week: @Composable ColumnScope.() -> Unit,
) = Column(
    Modifier.fillMaxSize().background(Brand.Sage).verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp).padding(top = Space.s, bottom = Space.xxl),
    verticalArrangement = Arrangement.spacedBy(14.dp),
) {
    val tonight = Garden.plantFor(unusedToday)
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButtonSoft(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
        Spacer(Modifier.width(Space.m))
        Text("${avatar.displayName}'s garden & week", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        IconButtonSoft(androidx.compose.material.icons.Icons.Filled.Settings, "Settings", onOpenSettings)
    }
    GardenScene(avatar, plants, Garden.homeStage(daysTended), preview = tonight)
    Things(avatar, worn, onShelf)

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Stat("${plants.size}", if (plants.size == 1) "plant" else "plants", Modifier.weight(1f))
        Stat("${daysTended + 1}", "days tending", Modifier.weight(1f))
        Stat("$unusedToday min", "unused today", Modifier.weight(1f))
    }

    Card {
        Text("TONIGHT", style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (tonight != null) PlantIcon(tonight, 44.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                tonight?.let { "If the day ends like this, a ${it.displayName} grows tonight (the see-through one)." }
                    ?: "No minutes left to grow a plant tonight. Tomorrow's a fresh start.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }

    Card {
        Text("HOW IT GROWS", style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            listOf(Plant.SPROUT to "1–14 min", Plant.TULIP to "15–29 min", Plant.SUNFLOWER to "30+ min").forEach { (p, m) ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Brand.Sage).padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PlantIcon(p, 40.dp)
                    Text(p.displayName.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium)
                    Text(m, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Text("Minutes you don't use become one plant each night.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
    }
    HomeStages(daysTended)
    week()
}

/** What the companion wears from a week of habits, as four small tiles; faded ones wait on the shelf. */
@Composable
private fun Things(avatar: Avatar, worn: Set<Accessory>, onShelf: Set<Accessory>) = Card {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("${avatar.displayName.uppercase()}'S THINGS", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text("a week of habits", style = MaterialTheme.typography.bodyMedium)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
        Accessory.entries.forEach { a ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        // Light chips in both themes, so the ink-drawn glasses stay visible.
                        .background(if (a in worn) Brand.Mint.copy(alpha = 0.35f) else if (Brand.dark) Color(0xFFDCE2DD) else Brand.Sage),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.foundation.Image(
                        CharacterPainter.accessoryIcon(a, 96).asImageBitmap(), a.title,
                        Modifier.size(40.dp).graphicsLayer { alpha = if (a in worn) 1f else if (a in onShelf) 0.6f else 0.3f },
                    )
                }
                Text(
                    a.title.removePrefix("Reading ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), maxLines = 1, textAlign = TextAlign.Center,
                    color = if (a in worn) Brand.Ink else Brand.Muted, modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    when (a) { in worn -> "Wearing it"; in onShelf -> "On the shelf"; else -> a.short },
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 15.sp), textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Week 1 / 2 / 4 milestones of the home on one track, with what comes next. */
@Composable
fun HomeStages(daysTended: Int) = Card {
    val stage = Garden.homeStage(daysTended)
    val milestones = listOf(Triple("sprout", 0, "First sprouts"), Triple("bench", 7, "Lit window & bench"), Triple("outside", 21, "A tree of your own"))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("YOUR HOME GROWS WITH YOU", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Text("Day ${daysTended + 1}", style = MaterialTheme.typography.labelLarge, color = Brand.Moss)
    }
    // Track: halfway at day 7, full at day 21.
    val fill = if (daysTended < 7) daysTended / 14f else (0.5f + (daysTended - 7) / 28f).coerceAtMost(1f)
    Box(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp).height(44.dp)) {
        Box(Modifier.align(Alignment.CenterStart).padding(horizontal = 22.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(Brand.Sage)) {
            Box(Modifier.fillMaxWidth(fill).height(6.dp).clip(RoundedCornerShape(50)).background(Brand.Mint))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            milestones.forEachIndexed { i, (icon, _, _) ->
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(if (i <= stage) Brand.Moss else Brand.Card)
                        .border(2.dp, if (i <= stage) Brand.Moss else Brand.Sage, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { TendIcons.Glyph(icon, 22.dp, tint = if (i <= stage) Color.White else Brand.Muted) }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        milestones.forEachIndexed { i, (_, day, what) ->
            Column(
                Modifier.width(96.dp),
                horizontalAlignment = when (i) { 0 -> Alignment.Start; 2 -> Alignment.End; else -> Alignment.CenterHorizontally },
            ) {
                Text(if (day == 0) "Week 1" else "Week ${day / 7 + 1}", style = MaterialTheme.typography.labelLarge, color = if (i <= stage) Brand.Moss else Brand.Muted)
                Text(what, style = MaterialTheme.typography.bodyMedium, textAlign = when (i) { 0 -> TextAlign.Start; 2 -> TextAlign.End; else -> TextAlign.Center })
            }
        }
    }
    val next = milestones.getOrNull(stage + 1)
    Text(
        next?.let { "${it.third} in ${it.second - daysTended} ${if (it.second - daysTended == 1) "day" else "days"}." } ?: "Your home is all grown. Keep tending it.",
        style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
fun RecapScreen(
    avatar: Avatar,
    recap: Recap,
    plants: List<Plant>,
    daysTended: Int,
    reminderHour: Int?,
    onReminder: (Boolean) -> Unit,
    onGoodNight: () -> Unit,
) = Column(
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
    if (reminderHour != null) Card {
        Text("A nudge tomorrow evening?", style = MaterialTheme.typography.titleMedium)
        Text("${avatar.displayName} can send one quiet note at ${hour(reminderHour)} when the recap is ready.", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            ChipButton("Yes, remind me", { onReminder(true) })
            ChipButton("No thanks", { onReminder(false) }, filled = false)
        }
    }
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
