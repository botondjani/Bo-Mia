package app.tend.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import app.tend.core.Accessory
import app.tend.core.Avatar
import app.tend.core.Mood

/**
 * Placeholder for the Rive rig: draws Bo/Mia in one of the four states on a plain
 * android.graphics.Canvas, so the same drawing serves Compose and the home screen widget.
 * Coordinates are in a 100x100 box.
 */
/** What the companion holds or wears during a timer; the number is the rig's "prop" input. */
enum class Prop { NONE, DUMBBELL, HEARTS, PALETTE, BROOM, CHEF_HAT }

object CharacterPainter {
    private const val MINT = 0xFF7ECAA3.toInt()
    private const val MINT_LIGHT = 0xFFA9DDC1.toInt()
    private const val TIRED = 0xFFAEB8B0.toInt()
    private const val TIRED_LIGHT = 0xFFC8D0CA.toInt()
    private const val INK = 0xFF1D2A23.toInt()
    private const val CHEEK = 0xFFC9937A.toInt()
    private const val HAIR = 0xFF5A3A2A.toInt()
    private const val CORAL = 0xFFE76649.toInt()
    private const val SUNNY = 0xFFF6D673.toInt()
    private const val SKY = 0xFF8EC4E8.toInt()
    private const val SHADOW = 0x221D2A23
    private const val COAT = 0xFFFDFDFB.toInt()
    private const val WEIGHT = 0xFF4A5852.toInt()
    private const val NIGHTCAP = 0xFF5B7FC0.toInt()
    private const val LEAF_GREEN = 0xFF4FA36F.toInt()
    private const val LEAF_STEM = 0xFF316849.toInt()
    private const val BOW_KNOT = 0xFF5B8FC0.toInt()
    private const val PALETTE = 0xFFEAD2A8.toInt()
    private const val STRAW = 0xFFD9B45A.toInt()
    private const val COAT_LINE = 0xFFD5DCD6.toInt()

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 2.6f; color = INK
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; textSize = 9f; isFakeBoldText = true }

    fun bitmap(avatar: Avatar, mood: Mood, sizePx: Int, accessories: Set<Accessory> = emptySet()): Bitmap =
        Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).also {
            draw(Canvas(it), avatar, mood, sizePx.toFloat(), accessories = accessories)
        }

    /** Animation pose for the fallback character; the widget uses the resting pose. */
    data class Pose(
        /** 0 eyes open, 1 closed. */
        val blink: Float = 0f,
        /** -1..1, squash and stretch from the feet. */
        val breathe: Float = 0f,
        /** -1..1, pupils glance left or right. */
        val look: Float = 0f,
        /** Holds an open book and reads (focus timer). */
        val book: Boolean = false,
        /** Lab coat, glasses and a pointer: Bo explaining things (onboarding, tour). */
        val scientist: Boolean = false,
        /** Pointer angle in degrees around the hand, for a little wave while explaining. */
        val point: Float = 0f,
        /** Something to hold or wear during a timer: [Prop]. */
        val prop: Prop = Prop.NONE,
    )

    @Synchronized
    fun draw(c: Canvas, avatar: Avatar, mood: Mood, size: Float, pose: Pose = Pose(), accessories: Set<Accessory> = emptySet()) {
        c.save()
        c.scale(size / 100f, size / 100f)
        c.scale(1f + 0.025f * pose.breathe, 1f - 0.025f * pose.breathe, 50f, 92f)
        val tired = mood == Mood.TIRED
        val body = if (tired) TIRED else MINT
        val belly = if (tired) TIRED_LIGHT else MINT_LIGHT

        // Ground shadow
        fill.color = SHADOW; c.drawOval(RectF(26f, 90f, 74f, 97f), fill)

        // Hair / ears behind the body
        when (avatar) {
            Avatar.BO -> {
                fill.color = HAIR
                c.drawOval(RectF(43f, 12f, 50f, 24f), fill)
                c.drawOval(RectF(49f, 12f, 56f, 24f), fill)
            }
            Avatar.MIA -> {
                fill.color = CORAL
                c.drawCircle(30f, 25f, 7f, fill)
                c.drawCircle(70f, 25f, 7f, fill)
            }
        }

        // Body: soft egg-shaped blob
        fill.color = body
        val blob = Path().apply {
            moveTo(50f, 18f)
            cubicTo(76f, 18f, 84f, 48f, 82f, 68f)
            cubicTo(80f, 86f, 66f, 92f, 50f, 92f)
            cubicTo(34f, 92f, 20f, 86f, 18f, 68f)
            cubicTo(16f, 48f, 24f, 18f, 50f, 18f)
            close()
        }
        c.drawPath(blob, fill)
        fill.color = belly; c.drawOval(RectF(32f, 62f, 68f, 90f), fill)

        if (avatar == Avatar.MIA) { // bow
            fill.color = SUNNY
            c.drawPath(Path().apply { moveTo(50f, 22f); lineTo(42f, 17f); lineTo(42f, 27f); close() }, fill)
            c.drawPath(Path().apply { moveTo(50f, 22f); lineTo(58f, 17f); lineTo(58f, 27f); close() }, fill)
        }

        // Cheeks
        fill.color = CHEEK; fill.alpha = 150
        c.drawCircle(30f, 60f, 5f, fill); c.drawCircle(70f, 60f, 5f, fill)
        fill.alpha = 255

        if (pose.scientist) drawCoat(c, belly)
        drawEyes(c, mood, body, pose)
        drawMouth(c, mood)
        drawExtras(c, mood)
        if (pose.book) drawBook(c)
        drawAccessories(c, if (pose.scientist) accessories + Accessory.GLASSES else accessories)
        if (pose.scientist) drawPointer(c, pose.point)
        drawProp(c, pose.prop, body)
        c.restore()
    }

    /** Same shapes as the Rive rig's props (tools/rive/make_character.py). */
    private fun drawProp(c: Canvas, prop: Prop, body: Int) {
        when (prop) {
            Prop.NONE -> Unit
            Prop.DUMBBELL -> {
                stroke.strokeWidth = 2.4f; c.drawLine(70f, 74f, 92f, 74f, stroke); stroke.strokeWidth = 2.6f
                fill.color = WEIGHT
                c.drawRoundRect(RectF(66f, 66f, 72f, 82f), 2f, 2f, fill); c.drawRoundRect(RectF(90f, 66f, 96f, 82f), 2f, 2f, fill)
                fill.color = body; c.drawCircle(81f, 76f, 4.2f, fill)
            }
            Prop.HEARTS -> { heart(c, 80f, 20f, 10f, CORAL); heart(c, 90f, 33f, 6f, CORAL) }
            Prop.PALETTE -> {
                fill.color = PALETTE; c.drawOval(RectF(8f, 64f, 32f, 82f), fill)
                for ((x, y, col) in listOf(Triple(14f, 70f, CORAL), Triple(20f, 68f, SKY), Triple(26f, 70f, SUNNY), Triple(14f, 76f, MINT))) {
                    fill.color = col; c.drawCircle(x, y, 2f, fill)
                }
                stroke.color = HAIR; stroke.strokeWidth = 2.2f; c.drawLine(80f, 78f, 92f, 58f, stroke)
                stroke.color = INK; stroke.strokeWidth = 2.6f
                fill.color = SKY; c.drawCircle(92f, 58f, 2.2f, fill)
                fill.color = body; c.drawCircle(80f, 78f, 4.2f, fill)
            }
            Prop.BROOM -> {
                stroke.color = HAIR; stroke.strokeWidth = 2.4f; c.drawLine(88f, 40f, 76f, 84f, stroke)
                fill.color = STRAW
                c.drawPath(Path().apply { moveTo(70f, 82f); lineTo(82f, 82f); lineTo(86f, 96f); lineTo(66f, 96f); close() }, fill)
                stroke.color = CORAL; stroke.strokeWidth = 1.6f; c.drawLine(70f, 84f, 82f, 84f, stroke)
                stroke.color = INK; stroke.strokeWidth = 2.6f
                fill.color = body; c.drawCircle(82f, 64f, 4.2f, fill)
            }
            Prop.CHEF_HAT -> {
                fill.color = COAT
                c.drawCircle(40f, 15f, 8f, fill); c.drawCircle(50f, 11f, 9f, fill); c.drawCircle(60f, 15f, 8f, fill)
                c.drawRoundRect(RectF(37f, 16f, 63f, 26f), 2f, 2f, fill)
                stroke.color = COAT_LINE; stroke.strokeWidth = 1.2f; c.drawLine(37f, 21f, 63f, 21f, stroke)
                stroke.color = INK; stroke.strokeWidth = 2.6f
            }
        }
    }

    private fun heart(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        fill.color = color
        c.drawCircle(cx - 0.25f * s, cy - 0.1f * s, 0.3f * s, fill)
        c.drawCircle(cx + 0.25f * s, cy - 0.1f * s, 0.3f * s, fill)
        c.drawPath(Path().apply { moveTo(cx - 0.53f * s, cy + 0.02f * s); lineTo(cx + 0.53f * s, cy + 0.02f * s); lineTo(cx, cy + 0.62f * s); close() }, fill)
    }

    /** White lab coat over the lower body, open at the front, with lapels and a pen in the pocket. */
    private fun drawCoat(c: Canvas, belly: Int) {
        fill.color = COAT
        c.drawPath(Path().apply {
            moveTo(44f, 66f); lineTo(18.3f, 64f); lineTo(18f, 68f); cubicTo(20f, 86f, 34f, 92f, 50f, 92f); lineTo(40f, 92f); close()
        }, fill)
        c.drawPath(Path().apply {
            moveTo(56f, 66f); lineTo(81.7f, 64f); lineTo(82f, 68f); cubicTo(80f, 86f, 66f, 92f, 50f, 92f); lineTo(60f, 92f); close()
        }, fill)
        stroke.color = COAT_LINE; stroke.strokeWidth = 1.4f
        c.drawLine(44f, 66f, 40.5f, 76f, stroke); c.drawLine(56f, 66f, 59.5f, 76f, stroke)
        c.drawRoundRect(RectF(26.5f, 75.5f, 35.5f, 82.5f), 1.5f, 1.5f, stroke)
        stroke.color = CORAL; stroke.strokeWidth = 1.8f
        c.drawLine(33f, 75.5f, 33f, 71.5f, stroke)
        stroke.color = INK; stroke.strokeWidth = 2.6f
        fill.color = belly
    }

    /** A pointer held at the right side, turning [angle] degrees around the hand. */
    private fun drawPointer(c: Canvas, angle: Float) {
        c.save()
        c.rotate(angle, 80f, 78f)
        stroke.color = HAIR; stroke.strokeWidth = 2.2f
        c.drawLine(80f, 78f, 92f, 49f, stroke)
        fill.color = CORAL; c.drawCircle(92f, 49f, 2.4f, fill)
        stroke.color = INK; stroke.strokeWidth = 2.6f
        c.restore()
        fill.color = MINT; c.drawCircle(80f, 78f, 4.2f, fill) // the hand
    }

    private fun drawEyes(c: Canvas, mood: Mood, body: Int, pose: Pose) {
        for (x in floatArrayOf(39f, 61f)) {
            fill.color = 0xFFFFFFFF.toInt(); c.drawCircle(x, 46f, 8f, fill)
            fill.color = if (mood == Mood.SCROLLING) 0xFF3B5BDB.toInt() else INK
            val py = if (mood == Mood.SCROLLING || pose.book) 49f else 47f
            val px = x + 1f + 2f * pose.look
            c.drawCircle(px, py, 4.6f, fill)
            fill.color = 0xFFFFFFFF.toInt(); c.drawCircle(px + 1.5f, py - 2f, 1.5f, fill)
            if (mood == Mood.SLEEPING) { // closed: lid all the way down
                fill.color = body
                c.drawRect(x - 9f, 37f, x + 9f, 54f, fill)
                c.drawArc(RectF(x - 6f, 44f, x + 6f, 52f), 20f, 140f, false, stroke)
                continue
            }
            if (mood == Mood.TIRED || mood == Mood.SCROLLING || mood == Mood.SLEEPY) { // heavy lids
                fill.color = body
                val lid = if (mood == Mood.SCROLLING) 44f else 46f
                c.drawRect(x - 9f, 37f, x + 9f, lid, fill)
                c.drawLine(x - 7f, lid, x + 7f, lid, stroke)
            }
            if (pose.blink > 0.05f) { // lid comes down over the eye
                val lid = 37f + 17f * pose.blink
                fill.color = body
                c.drawRect(x - 9f, 37f, x + 9f, lid, fill)
                c.drawLine(x - 7f, lid, x + 7f, lid, stroke)
            }
        }
    }

    private fun drawMouth(c: Canvas, mood: Mood) {
        when (mood) {
            Mood.HAPPY -> c.drawArc(RectF(42f, 56f, 58f, 68f), 20f, 140f, false, stroke)
            Mood.TIRED -> c.drawArc(RectF(43f, 66f, 57f, 74f), 200f, 140f, false, stroke)
            Mood.SLEEPING, Mood.SLEEPY -> c.drawArc(RectF(45f, 58f, 55f, 66f), 20f, 140f, false, stroke)
            Mood.SCROLLING -> c.drawLine(45f, 68f, 55f, 68f, stroke)
            Mood.OUTSIDE -> {
                fill.color = INK
                c.drawPath(Path().apply {
                    moveTo(41f, 61f); quadTo(50f, 64f, 59f, 61f); quadTo(57f, 75f, 50f, 75f); quadTo(43f, 75f, 41f, 61f); close()
                }, fill)
                fill.color = CORAL; c.drawOval(RectF(45f, 69f, 55f, 75f), fill)
            }
        }
    }

    /** One accessory on its own, framed on the head area, for the "things" list. */
    fun accessoryIcon(a: Accessory, sizePx: Int): Bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).also {
        val c = Canvas(it)
        // Each accessory's own bounds in painter units, so every icon fills its circle the same way.
        val box = when (a) {
            Accessory.HEADBAND -> RectF(23f, 23f, 77f, 39f)
            Accessory.GLASSES -> RectF(28f, 35f, 72f, 57f)
            Accessory.HAT -> RectF(19f, 7f, 81f, 28f)
            Accessory.FLOWER -> RectF(64f, 24f, 78f, 38f)
            Accessory.LEAF -> RectF(51f, 8f, 68f, 22f)
            Accessory.BOWTIE -> RectF(41f, 67f, 59f, 79f)
            Accessory.NIGHTCAP -> RectF(30f, 2f, 76f, 26f)
        }
        val scale = sizePx * 0.86f / maxOf(box.width(), box.height())
        c.translate(sizePx / 2f, sizePx / 2f)
        c.scale(scale, scale)
        c.translate(-box.centerX(), -box.centerY())
        drawAccessories(c, setOf(a))
    }

    /** Same shapes as the Rive rig (tools/rive/make_character.py), drawn back to front. */
    private fun drawAccessories(c: Canvas, a: Set<Accessory>) {
        if (Accessory.HEADBAND in a) {
            stroke.color = CORAL; stroke.strokeWidth = 4.5f
            c.drawPath(Path().apply { moveTo(25f, 37f); quadTo(50f, 25f, 75f, 37f) }, stroke)
            stroke.color = INK; stroke.strokeWidth = 2.6f
        }
        if (Accessory.GLASSES in a) {
            stroke.strokeWidth = 1.8f
            c.drawCircle(39f, 46f, 9.5f, stroke); c.drawCircle(61f, 46f, 9.5f, stroke)
            c.drawLine(48.5f, 45f, 51.5f, 45f, stroke)
            stroke.strokeWidth = 2.6f
        }
        if (Accessory.FLOWER in a) {
            fill.color = 0xFFFFFFFF.toInt()
            for (k in 0 until 5) {
                val ang = Math.toRadians(k * 72.0 - 90)
                c.drawCircle(71f + 3.2f * Math.cos(ang).toFloat(), 31f + 3.2f * Math.sin(ang).toFloat(), 2.6f, fill)
            }
            fill.color = SUNNY; c.drawCircle(71f, 31f, 2.2f, fill)
        }
        if (Accessory.BOWTIE in a) {
            fill.color = SKY
            c.drawPath(Path().apply { moveTo(50f, 73f); lineTo(42f, 68f); lineTo(42f, 78f); close() }, fill)
            c.drawPath(Path().apply { moveTo(50f, 73f); lineTo(58f, 68f); lineTo(58f, 78f); close() }, fill)
            fill.color = BOW_KNOT; c.drawCircle(50f, 73f, 2.4f, fill)
        }
        if (Accessory.LEAF in a) {
            stroke.color = LEAF_STEM; stroke.strokeWidth = 1.6f; c.drawLine(52f, 21f, 55.5f, 18f, stroke)
            stroke.color = INK; stroke.strokeWidth = 2.6f
            fill.color = LEAF_GREEN
            c.drawPath(Path().apply { moveTo(54f, 19f); cubicTo(55f, 11f, 61f, 9f, 67f, 10f); cubicTo(66f, 17f, 60f, 20f, 54f, 19f); close() }, fill)
        }
        if (Accessory.NIGHTCAP in a) { // a soft blue cone flopping to the right, with a pom-pom
            fill.color = NIGHTCAP
            c.drawPath(Path().apply { moveTo(34f, 22f); quadTo(48f, 0f, 70f, 12f); lineTo(64f, 17f); quadTo(54f, 12f, 66f, 22f); close() }, fill)
            fill.color = COAT; c.drawRoundRect(RectF(32f, 19f, 68f, 25f), 3f, 3f, fill)
            c.drawCircle(71f, 13f, 3.6f, fill)
        }
        if (Accessory.HAT in a) {
            fill.color = SUNNY
            c.drawRoundRect(RectF(36f, 8f, 64f, 22f), 6f, 6f, fill)
            c.drawOval(RectF(20f, 18f, 80f, 27f), fill)
            fill.color = CORAL; c.drawRect(36f, 16f, 64f, 20f, fill)
        }
    }

    private fun drawBook(c: Canvas) {
        fill.color = CORAL
        c.drawPath(Path().apply { moveTo(28f, 70f); lineTo(50f, 74f); lineTo(72f, 70f); lineTo(72f, 90f); lineTo(50f, 94f); lineTo(28f, 90f); close() }, fill)
        fill.color = 0xFFFFF8EC.toInt()
        c.drawPath(Path().apply { moveTo(31f, 70f); lineTo(49f, 73f); lineTo(49f, 90f); lineTo(31f, 87f); close() }, fill)
        c.drawPath(Path().apply { moveTo(69f, 70f); lineTo(51f, 73f); lineTo(51f, 90f); lineTo(69f, 87f); close() }, fill)
        stroke.strokeWidth = 1f; stroke.color = 0x551D2A23
        for (y in floatArrayOf(77f, 81f, 85f)) { c.drawLine(34f, y, 46f, y + 1.5f, stroke); c.drawLine(54f, y + 1.5f, 66f, y, stroke) }
        stroke.strokeWidth = 2.6f; stroke.color = INK
    }

    private fun drawExtras(c: Canvas, mood: Mood) {
        when (mood) {
            Mood.TIRED, Mood.SLEEPING, Mood.SLEEPY -> { c.drawText("z", 78f, 22f, text); text.textSize = 7f; c.drawText("z", 85f, 15f, text); text.textSize = 9f }
            Mood.SCROLLING -> { // phone
                fill.color = INK; c.drawRoundRect(RectF(66f, 58f, 80f, 82f), 3f, 3f, fill)
                fill.color = SKY; c.drawRoundRect(RectF(68f, 61f, 78f, 78f), 1.5f, 1.5f, fill)
            }
            Mood.OUTSIDE -> { // sun + grass
                fill.color = SUNNY; c.drawCircle(84f, 14f, 6f, fill)
                stroke.color = 0xFF316849.toInt(); stroke.strokeWidth = 1.8f
                for (x in floatArrayOf(22f, 26f, 74f, 78f)) c.drawLine(x, 92f, x + if (x < 50) -2f else 2f, 86f, stroke)
                stroke.color = INK; stroke.strokeWidth = 2.6f
            }
            Mood.HAPPY -> Unit
        }
    }
}
