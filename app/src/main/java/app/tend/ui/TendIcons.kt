package app.tend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tend.R

/** tend's own line icons (res/drawable/ic_t_*): one 24 grid, 1.8 stroke, round caps. */
object TendIcons {
    private val res = mapOf(
        "workout" to R.drawable.ic_t_workout, "outside" to R.drawable.ic_t_outside, "read" to R.drawable.ic_t_read, "learn" to R.drawable.ic_t_learn,
        "friends" to R.drawable.ic_t_friends, "hobby" to R.drawable.ic_t_hobby, "chores" to R.drawable.ic_t_chores,
        "cook" to R.drawable.ic_t_cook, "music" to R.drawable.ic_t_music, "pen" to R.drawable.ic_t_pen,
        "paw" to R.drawable.ic_t_paw, "sprout" to R.drawable.ic_t_sprout, "bike" to R.drawable.ic_t_bike,
        "star" to R.drawable.ic_t_star, "moon" to R.drawable.ic_t_moon, "alert" to R.drawable.ic_t_alert,
        "heart" to R.drawable.ic_t_heart, "sparkle" to R.drawable.ic_t_sparkle, "sun" to R.drawable.ic_t_sun,
        "flower" to R.drawable.ic_t_flower, "hourglass" to R.drawable.ic_t_hourglass, "bench" to R.drawable.ic_t_bench,
        "face_yes" to R.drawable.ic_t_face_yes, "face_meh" to R.drawable.ic_t_face_meh, "face_no" to R.drawable.ic_t_face_no,
    )

    /** Accent per icon, so the list reads at a glance: movement mint, mind sky, people coral, home sunny. */
    private val accent = mapOf(
        "workout" to Brand.Mint, "outside" to Brand.Mint, "bike" to Brand.Mint, "sprout" to Brand.Mint,
        "read" to Brand.Sky, "learn" to Brand.Sky, "pen" to Brand.Sky, "music" to Brand.Sky, "moon" to Brand.Sky,
        "friends" to Brand.Coral, "heart" to Brand.Coral, "alert" to Brand.Coral, "paw" to Brand.Coral,
        "hobby" to Brand.Sunny, "chores" to Brand.Sunny, "cook" to Brand.Sunny, "star" to Brand.Sunny, "sun" to Brand.Sunny,
    )

    /** Icons offered for the user's own activities. */
    val ownChoices = listOf("music", "pen", "paw", "sprout", "bike", "star", "heart", "sun")

    fun accentOf(key: String): Color = accent[key] ?: Brand.Mint

    @Composable
    fun Glyph(key: String, size: Dp = 24.dp, tint: Color = Brand.Moss) {
        val id = res[key]
        // Activities saved before the icon set existed still carry an emoji: show it as is.
        if (id != null) Icon(painterResource(id), null, tint = tint, modifier = Modifier.size(size))
        else Text(key, fontSize = (size.value * 0.9f).sp)
    }

    /** The icon in a soft tinted circle: list rows, notices, tiles. */
    @Composable
    fun Badge(key: String, size: Dp = 44.dp, accent: Color = accentOf(key)) = Box(
        Modifier.size(size).clip(CircleShape).background(accent.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
    ) { Glyph(key, size * 0.52f) }
}
