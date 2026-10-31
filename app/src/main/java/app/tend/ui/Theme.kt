package app.tend.ui

import androidx.compose.material3.MaterialTheme
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

object Brand {
    val Moss = Color(0xFF316849)
    val Mint = Color(0xFF7ECAA3)
    val Coral = Color(0xFFE76649)
    val Sunny = Color(0xFFF6D673)
    val Sky = Color(0xFF8EC4E8)
    val Ink = Color(0xFF1D2A23)
    val Sage = Color(0xFFF1F4ED)
    val Card = Color(0xFFFFFFFF)
    val Muted = Color(0xFF6B756F)
}

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

/** Headlines, numbers, the character's voice. */
val Fredoka = FontFamily(variable(R.font.fredoka, 500), variable(R.font.fredoka, 600))

/** Body text, interface, buttons. */
val Nunito = FontFamily(variable(R.font.nunito, 400), variable(R.font.nunito, 600), variable(R.font.nunito, 700))

private val typography = Typography(
    headlineLarge = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight(600), fontSize = 32.sp, color = Brand.Ink),
    headlineMedium = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight(600), fontSize = 26.sp, color = Brand.Ink),
    titleLarge = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight(600), fontSize = 20.sp, color = Brand.Ink),
    titleMedium = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight(500), fontSize = 17.sp, color = Brand.Ink),
    bodyLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(600), fontSize = 16.sp, color = Brand.Ink),
    bodyMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(400), fontSize = 14.sp, color = Brand.Muted),
    labelLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(700), fontSize = 15.sp),
    labelSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight(700), fontSize = 11.sp, letterSpacing = 1.2.sp, color = Brand.Muted),
)

@Composable
fun TendTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = lightColorScheme(
        primary = Brand.Moss, onPrimary = Color.White,
        secondary = Brand.Mint, tertiary = Brand.Coral,
        background = Brand.Sage, surface = Brand.Card,
        onBackground = Brand.Ink, onSurface = Brand.Ink,
    ),
    typography = typography,
    content = content,
)
