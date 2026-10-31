package app.tend.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import app.tend.core.Avatar
import app.tend.core.Mood

/**
 * Placeholder for the Rive rig: draws Bo/Mia in one of the four states on a plain
 * android.graphics.Canvas, so the same drawing serves Compose and the home screen widget.
 * Coordinates are in a 100x100 box.
 */
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

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 2.6f; color = INK
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; textSize = 9f; isFakeBoldText = true }

    fun bitmap(avatar: Avatar, mood: Mood, sizePx: Int): Bitmap =
        Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).also {
            draw(Canvas(it), avatar, mood, sizePx.toFloat())
        }

    /** Animation pose for the fallback character; the widget uses the resting pose. */
    data class Pose(
        /** 0 eyes open, 1 closed. */
        val blink: Float = 0f,
        /** -1..1, squash and stretch from the feet. */
        val breathe: Float = 0f,
        /** -1..1, pupils glance left or right. */
        val look: Float = 0f,
    )

    @Synchronized
    fun draw(c: Canvas, avatar: Avatar, mood: Mood, size: Float, pose: Pose = Pose()) {
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

        drawEyes(c, mood, body, pose)
        drawMouth(c, mood)
        drawExtras(c, mood)
        c.restore()
    }

    private fun drawEyes(c: Canvas, mood: Mood, body: Int, pose: Pose) {
        for (x in floatArrayOf(39f, 61f)) {
            fill.color = 0xFFFFFFFF.toInt(); c.drawCircle(x, 46f, 8f, fill)
            fill.color = if (mood == Mood.SCROLLING) 0xFF3B5BDB.toInt() else INK
            val py = if (mood == Mood.SCROLLING) 49f else 47f
            val px = x + 1f + 2f * pose.look
            c.drawCircle(px, py, 4.6f, fill)
            fill.color = 0xFFFFFFFF.toInt(); c.drawCircle(px + 1.5f, py - 2f, 1.5f, fill)
            if (mood == Mood.TIRED || mood == Mood.SCROLLING) { // heavy lids
                fill.color = body
                c.drawRect(x - 9f, 37f, x + 9f, if (mood == Mood.TIRED) 46f else 44f, fill)
                c.drawLine(x - 7f, if (mood == Mood.TIRED) 46f else 44f, x + 7f, if (mood == Mood.TIRED) 46f else 44f, stroke)
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

    private fun drawExtras(c: Canvas, mood: Mood) {
        when (mood) {
            Mood.TIRED -> { c.drawText("z", 78f, 22f, text); text.textSize = 7f; c.drawText("z", 85f, 15f, text); text.textSize = 9f }
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
