package app.tend.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.tend.R

/**
 * Brand colours, light or dark. Every colour reads [dark], a snapshot state, so screens and the
 * drawn scenes follow a theme switch at once. Mint, coral, sunny and sky stay the same.
 */
object Brand {
    var dark by mutableStateOf(false)

    val Moss get() = if (dark) Color(0xFF4FA077) else Color(0xFF316849)
    val Mint = Color(0xFF7ECAA3)
    val Coral = Color(0xFFE76649)
    val Sunny = Color(0xFFF6D673)
    val Sky = Color(0xFF8EC4E8)
    val Ink get() = if (dark) Color(0xFFE6EEE9) else Color(0xFF1D2A23)
    val Sage get() = if (dark) Color(0xFF111915) else Color(0xFFF1F4ED)
    val Card get() = if (dark) Color(0xFF1B2620) else Color(0xFFFFFFFF)
    val Muted get() = if (dark) Color(0xFF98A69E) else Color(0xFF6B756F)
    /** A picked item's tint. */
    val Picked get() = if (dark) Color(0xFF1F3A2C) else Color(0xFFE8F5EE)
    /** Shadows and scrims stay dark in both themes. */
    val Shade = Color(0xFF0A0F0C)
}

/** The user's choice: follow the phone, or always light or dark. Kept here so every screen sees a switch. */
object ThemeChoice {
    var mode by mutableStateOf<String?>(null)
}


@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

/** Headlines, numbers, the character's voice. */
val Fredoka = FontFamily(variable(R.font.fredoka, 500), variable(R.font.fredoka, 600))

/** Body text, interface, buttons. */
val Nunito = FontFamily(variable(R.font.nunito, 400), variable(R.font.nunito, 600), variable(R.font.nunito, 700))

private fun typography() = Typography(
    // Hero numbers: minutes left, big stats.
    displayLarge = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight(600), fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = (-1).sp, color = Brand.Moss),
    headlineLarge = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight(600), fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.4).sp, color = Brand.Ink),
    headlineMedium = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight(600), fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp, color = Brand.Ink),
    titleLarge = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight(600), fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp, color = Brand.Ink),
    titleMedium = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight(500), fontSize = 17.sp, color = Brand.Ink),
    // No colour here: MaterialTheme uses bodyLarge as the default text style, and a colour would
    // override button content colours (dark text on green buttons). Ink comes from LocalContentColor.
    bodyLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(600), fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(400), fontSize = 14.sp, lineHeight = 20.sp, color = Brand.Muted),
    labelLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(700), fontSize = 15.sp),
    labelSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(700), fontSize = 11.sp, letterSpacing = 1.2.sp, color = Brand.Muted),
)

@Composable
fun TendTheme(content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mode = ThemeChoice.mode ?: app.tend.data.TendStore(context).themeMode.also { ThemeChoice.mode = it }
    val dark = when (mode) { "dark" -> true; "light" -> false; else -> androidx.compose.foundation.isSystemInDarkTheme() }
    if (Brand.dark != dark) Brand.dark = dark
    val type = androidx.compose.runtime.remember(dark) { typography() }
    MaterialTheme(
        colorScheme = (if (dark) androidx.compose.material3.darkColorScheme(
            primary = Brand.Moss, onPrimary = Color.White,
            secondary = Brand.Mint, tertiary = Brand.Coral,
            background = Brand.Sage, surface = Brand.Card,
            onBackground = Brand.Ink, onSurface = Brand.Ink,
        ) else lightColorScheme(
            primary = Brand.Moss, onPrimary = Color.White,
            secondary = Brand.Mint, tertiary = Brand.Coral,
            background = Brand.Sage, surface = Brand.Card,
            onBackground = Brand.Ink, onSurface = Brand.Ink,
        )),
        typography = type,
    ) { CompositionLocalProvider(LocalContentColor provides Brand.Ink, content = content) }
}
