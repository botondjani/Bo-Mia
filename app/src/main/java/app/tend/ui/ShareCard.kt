package app.tend.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.tend.R
import app.tend.core.Accessory
import app.tend.core.Avatar
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File

/**
 * "My camp this week": a 4:5 card (fits feeds and stories) with the clearing, what the week gave
 * back, and the way to tend: the Play link as a QR code on the image and as a link in the text.
 */
object ShareCard {
    const val W = 1080
    const val H = 1350

    data class Week(
        val avatar: Avatar,
        /** What stands in the clearing: spot id to thing id. */
        val placed: Map<Int, String>,
        val worn: Set<Accessory>,
        val gotBackMinutes: Int,
        val activeDays: Int,
        val letGo: Int,
        /** The companion's level, shown next to the name. */
        val level: Int = 1,
    )

    fun playUrl(context: Context) = "https://play.google.com/store/apps/details?id=${context.packageName}"

    private const val INK = 0xFF1D2A23.toInt()
    private const val MOSS = 0xFF316849.toInt()
    private const val MUTED = 0xFF6B756F.toInt()
    private const val SAGE = 0xFFF1F4ED.toInt()
    private const val MINT_SOFT = 0xFFD7EDDF.toInt()

    fun render(context: Context, w: Week): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, 0f, H.toFloat(), intArrayOf(0xFFFFFFFF.toInt(), SAGE, MINT_SOFT), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), paint)
        paint.shader = null
        // Two soft blobs of mint, so the background isn't flat.
        paint.color = MINT_SOFT; paint.alpha = 90
        c.drawCircle(W + 40f, 60f, 300f, paint)
        paint.alpha = 60
        c.drawCircle(-80f, H - 260f, 260f, paint)
        paint.alpha = 255

        val fredoka = font(context, R.font.fredoka, 600)
        val nunito = font(context, R.font.nunito, 400)
        val nunitoBold = font(context, R.font.nunito, 700)
        fun text(s: String, x: Float, y: Float, size: Float, color: Int, face: Typeface, spacing: Float = 0f, align: Paint.Align = Paint.Align.LEFT) {
            paint.typeface = face; paint.textSize = size; paint.color = color; paint.letterSpacing = spacing; paint.textAlign = align
            c.drawText(s, x, y, paint)
        }
        fun card(r: RectF, radius: Float, color: Int = 0xFFFFFFFF.toInt()) {
            paint.color = color
            paint.setShadowLayer(28f, 0f, 10f, 0x22316849)
            c.drawRoundRect(r, radius, radius, paint)
            paint.clearShadowLayer()
        }

        val name = w.avatar.displayName
        // A small label chip, then the headline: what the week gave back.
        val label = "${name.uppercase()}'S CAMP · LEVEL ${w.level}"
        paint.typeface = nunitoBold; paint.textSize = 28f; paint.letterSpacing = 0.12f
        val lw = paint.measureText(label)
        paint.color = MINT_SOFT
        c.drawRoundRect(RectF(64f, 72f, 64f + lw + 44f, 124f), 26f, 26f, paint)
        text(label, 86f, 108f, 28f, MOSS, nunitoBold, 0.12f)
        if (w.gotBackMinutes > 0) {
            text("I got back", 64f, 220f, 66f, INK, fredoka)
            text(duration(w.gotBackMinutes), 64f, 340f, 124f, MOSS, fredoka)
            text("from scrolling this week.", 64f, 418f, 54f, INK, fredoka)
        } else {
            text("We're building", 64f, 236f, 84f, INK, fredoka)
            text("a camp.", 64f, 336f, 84f, MOSS, fredoka)
            text("One small thing at a time.", 64f, 416f, 48f, INK, fredoka)
        }

        // The clearing, as it is today, in a white frame that lifts off the page.
        val frame = RectF(52f, 456f, W - 52f, 1012f)
        card(frame, 52f)
        val scene = clearingBitmap(w.avatar, w.placed, w.worn, (frame.width() - 28).toInt(), (frame.height() - 28).toInt())
        c.drawBitmap(scene, frame.left + 14, frame.top + 14, null)

        // Three small facts, each its own little card.
        val facts = listOf(
            "${w.placed.size}" to if (w.placed.size == 1) "thing at camp" else "things at camp",
            "${w.activeDays}" to if (w.activeDays == 1) "active day" else "active days",
            "${w.letGo}" to if (w.letGo == 1) "scroll let go" else "scrolls let go",
        )
        val pillW = (W - 104 - 2 * 20) / 3f
        // A first week with nothing yet: no row of zeros.
        if (w.placed.isNotEmpty() || w.activeDays > 0 || w.letGo > 0) facts.forEachIndexed { i, (n, label2) ->
            val x = 52f + i * (pillW + 20)
            card(RectF(x, 1040f, x + pillW, 1150f), 34f)
            text(n, x + pillW / 2, 1096f, 50f, MOSS, fredoka, align = Paint.Align.CENTER)
            text(label2, x + pillW / 2, 1131f, 26f, MUTED, nunito, align = Paint.Align.CENTER)
        }

        // Footer: the companion's face, the brand and the way in.
        val faceSize = 104
        card(RectF(52f, 1190f, 52f + faceSize + 16, 1190f + faceSize + 16), (faceSize + 16) / 2f)
        c.drawBitmap(app.tend.ui.CharacterPainter.bitmap(w.avatar, app.tend.core.Mood.HAPPY, faceSize, w.worn), 60f, 1196f, null)
        text("tend", 196f, 1262f, 62f, MOSS, fredoka)
        text("A little pause before you scroll.", 196f, 1302f, 27f, MUTED, nunito)
        val qr = 120
        val qx = W - 52f - 14 - qr; val qy = H - 44f - qr
        card(RectF(qx - 14, qy - 14, qx + qr + 14, qy + qr + 14), 24f)
        drawQr(c, playUrl(context), qx, qy, qr.toFloat(), paint)
        text("Get it on", qx - 30, 1262f, 25f, MUTED, nunito, align = Paint.Align.RIGHT)
        text("Google Play", qx - 30, 1296f, 27f, INK, nunitoBold, align = Paint.Align.RIGHT)
        return bmp
    }

    /** Saves the card and opens the share sheet: the image plus a line and the link. */
    fun share(activity: Activity, w: Week) {
        val dir = File(activity.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "tend-camp.png")
        file.outputStream().use { render(activity, w).compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.share", file)
        val line = if (w.gotBackMinutes > 0) "I got back ${duration(w.gotBackMinutes)} from scrolling this week, and ${w.avatar.displayName}'s camp grew."
            else "${w.avatar.displayName} and I are building a camp instead of scrolling."
        val send = Intent(Intent.ACTION_SEND).setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, "$line\n\ntend: a little pause before you scroll\n${playUrl(activity)}")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        activity.startActivity(Intent.createChooser(send, "Share your camp"))
    }

    private fun drawQr(c: Canvas, url: String, x: Float, y: Float, size: Float, paint: Paint) {
        val matrix = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 0))
        val cell = size / matrix.width
        paint.color = INK
        for (i in 0 until matrix.width) for (j in 0 until matrix.height) {
            if (matrix[i, j]) c.drawRect(x + i * cell, y + j * cell, x + (i + 1) * cell + 0.5f, y + (j + 1) * cell + 0.5f, paint)
        }
    }

    /** The brand fonts are variable: set the weight axis itself (Android 10+), else the closest style. */
    private fun font(context: Context, res: Int, weight: Int): Typeface {
        if (android.os.Build.VERSION.SDK_INT >= 29) runCatching {
            val f = android.graphics.fonts.Font.Builder(context.resources, res).setWeight(weight).setFontVariationSettings("'wght' $weight").build()
            return Typeface.CustomFallbackBuilder(android.graphics.fonts.FontFamily.Builder(f).build()).build()
        }
        val base = runCatching { ResourcesCompat.getFont(context, res) }.getOrNull() ?: Typeface.DEFAULT
        return if (android.os.Build.VERSION.SDK_INT >= 28) Typeface.create(base, weight, false) else Typeface.create(base, if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun duration(minutes: Int) = if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"
}
