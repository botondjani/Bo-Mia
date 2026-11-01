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
import app.tend.core.Plant
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File

/**
 * "My garden this week": a 4:5 card (fits feeds and stories) with the garden, what the week gave
 * back, and the way to tend: the Play link as a QR code on the image and as a link in the text.
 */
object ShareCard {
    const val W = 1080
    const val H = 1350

    data class Week(
        val avatar: Avatar,
        val plants: List<Plant>,
        val homeStage: Int,
        val worn: Set<Accessory>,
        val gotBackMinutes: Int,
        val activeDays: Int,
        val letGo: Int,
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

        val fredoka = font(context, R.font.fredoka, 600)
        val nunito = font(context, R.font.nunito, 400)
        val nunitoBold = font(context, R.font.nunito, 700)
        fun text(s: String, x: Float, y: Float, size: Float, color: Int, face: Typeface, spacing: Float = 0f, align: Paint.Align = Paint.Align.LEFT) {
            paint.typeface = face; paint.textSize = size; paint.color = color; paint.letterSpacing = spacing; paint.textAlign = align
            c.drawText(s, x, y, paint)
        }

        val name = w.avatar.displayName
        // Headline: what the week gave back.
        text("${name.uppercase()}'S GARDEN · THIS WEEK", 64f, 120f, 30f, MUTED, nunitoBold, 0.12f)
        if (w.gotBackMinutes > 0) {
            text("I got back", 64f, 220f, 68f, INK, fredoka)
            text(duration(w.gotBackMinutes), 64f, 340f, 120f, MOSS, fredoka)
            text("from scrolling this week.", 64f, 420f, 56f, INK, fredoka)
        } else {
            text("We're growing", 64f, 240f, 84f, INK, fredoka)
            text("a garden.", 64f, 340f, 84f, MOSS, fredoka)
            text("One small thing at a time.", 64f, 420f, 48f, INK, fredoka)
        }

        // The garden, as it is today.
        val gw = W - 128; val gh = 540
        val garden = gardenBitmap(w.avatar, w.plants, w.homeStage, w.worn, gw, gh)
        c.drawBitmap(garden, 64f, 466f, null)

        // Three small facts.
        val facts = listOf(
            "${w.plants.size}" to if (w.plants.size == 1) "plant" else "plants",
            "${w.activeDays}" to if (w.activeDays == 1) "active day" else "active days",
            "${w.letGo}" to if (w.letGo == 1) "scroll let go" else "scrolls let go",
        )
        val pillW = (W - 128 - 2 * 24) / 3f
        // A first week with nothing yet: no row of zeros.
        if (w.plants.isNotEmpty() || w.activeDays > 0 || w.letGo > 0) facts.forEachIndexed { i, (n, label) ->
            val x = 64f + i * (pillW + 24)
            paint.color = 0xFFFFFFFF.toInt()
            c.drawRoundRect(RectF(x, 1032f, x + pillW, 1138f), 32f, 32f, paint)
            text(n, x + pillW / 2, 1086f, 48f, MOSS, fredoka, align = Paint.Align.CENTER)
            text(label, x + pillW / 2, 1121f, 26f, MUTED, nunito, align = Paint.Align.CENTER)
        }

        // Footer: the brand and the way in.
        text("tend", 64f, 1278f, 64f, MOSS, fredoka)
        text("A little pause before you scroll.", 64f, 1318f, 28f, MUTED, nunito)
        val qr = 128
        val qx = W - 64f - qr; val qy = H - 40f - qr
        paint.color = 0xFFFFFFFF.toInt()
        c.drawRoundRect(RectF(qx - 12, qy - 12, qx + qr + 12, qy + qr + 12), 20f, 20f, paint)
        drawQr(c, playUrl(context), qx, qy, qr.toFloat(), paint)
        text("Get it on Google Play", qx - 28, 1300f, 26f, INK, nunitoBold, align = Paint.Align.RIGHT)
        return bmp
    }

    /** Saves the card and opens the share sheet: the image plus a line and the link. */
    fun share(activity: Activity, w: Week) {
        val dir = File(activity.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "tend-garden.png")
        file.outputStream().use { render(activity, w).compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.share", file)
        val line = if (w.gotBackMinutes > 0) "I got back ${duration(w.gotBackMinutes)} from scrolling this week, and ${w.avatar.displayName}'s garden grew."
            else "${w.avatar.displayName} and I are growing a garden instead of scrolling."
        val send = Intent(Intent.ACTION_SEND).setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, "$line\n\ntend: a little pause before you scroll\n${playUrl(activity)}")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        activity.startActivity(Intent.createChooser(send, "Share your garden"))
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
