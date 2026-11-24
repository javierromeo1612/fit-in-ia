package com.marbellup.fitin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.marbellup.fitin.R

/*
 * Estilo (rediseño 2026): app deportiva luminosa en lila y violeta, con fondos en degradado
 * suave, tarjetas blancas tipo cristal muy redondeadas, botones en píldora, fotos de cada
 * deporte y la mascota robot de Fit-In como asistente.
 * Los estados conservan su significado: verde = comprobado, ámbar = falta algo,
 * rojo = barrera. Siempre van acompañados de texto. Todos los textos cumplen contraste AA.
 */

@Immutable
data class ExtraColors(
    val card: Color,
    val chip: Color,
    val onChip: Color,
    val ok: Color,
    val okContainer: Color,
    val onOkContainer: Color,
    val warn: Color,
    val warnContainer: Color,
    val onWarnContainer: Color,
    val barrier: Color,
    val scrim: Color,
    val notification: Color,
    /** Fondo de mosaicos y tarjetas de deporte sin seleccionar. */
    val tile: Color,
    /** Tarjeta de deporte seleccionada. */
    val tileSelected: Color,
    /** Violeta del degradado de botones y cabeceras. */
    val hero: Color,
    /** Acento cálido (llama, estrella). */
    val flame: Color,
    /** Degradado de fondo de pantalla: arriba → abajo. */
    val gradientTop: Color,
    val gradientBottom: Color,
    /** Pasteles para tarjetas del calendario y categorías. */
    val lilac: Color,
    val sky: Color,
    val mint: Color,
    val peach: Color,
    val pink: Color,
    /** Burbuja de la IA. */
    val aiBubble: Color,
    /** Burbuja de la persona usuaria en el chat. */
    val userBubble: Color,
)

/* Paleta tomada de la nueva referencia visual del equipo (lilas y violetas). */
private val LightExtra = ExtraColors(
    card = Color(0xFFFFFFFF),
    chip = Color(0xFFEDE8FF),
    onChip = Color(0xFF231B4D),
    ok = Color(0xFF17704A),
    okContainer = Color(0xFFE2F5EC),
    onOkContainer = Color(0xFF0F4A30),
    warn = Color(0xFFA35D0A),
    warnContainer = Color(0xFFFFF0DB),
    onWarnContainer = Color(0xFF5C3304),
    barrier = Color(0xFFC93A3A),
    scrim = Color(0x99231B4D),
    notification = Color(0xFFC8361C),
    tile = Color(0xFFF2EEFF),
    tileSelected = Color(0xFF6C4CF0),
    hero = Color(0xFF6C4CF0),
    flame = Color(0xFFF2852B),
    gradientTop = Color(0xFFE4DBFF),
    gradientBottom = Color(0xFFF6F3FF),
    lilac = Color(0xFFEDE5FF),
    sky = Color(0xFFE1EEFF),
    mint = Color(0xFFDFF5EA),
    peach = Color(0xFFFFE9D9),
    pink = Color(0xFFFFE3F0),
    aiBubble = Color(0xFFF1ECFF),
    userBubble = Color(0xFFE3F4D9),
)

private val DarkExtra = ExtraColors(
    card = Color(0xFF221C3D),
    chip = Color(0xFF332A5C),
    onChip = Color(0xFFE9E3FF),
    ok = Color(0xFF7AD3A2),
    okContainer = Color(0xFF1D3B2D),
    onOkContainer = Color(0xFFCFF3DF),
    warn = Color(0xFFF2B866),
    warnContainer = Color(0xFF41300F),
    onWarnContainer = Color(0xFFFFE3BD),
    barrier = Color(0xFFFF8A80),
    scrim = Color(0xA60A0618),
    notification = Color(0xFFFF6B5A),
    tile = Color(0xFF2B2450),
    tileSelected = Color(0xFF8B70FF),
    hero = Color(0xFF7A5CFF),
    flame = Color(0xFFFFA45C),
    gradientTop = Color(0xFF241B4F),
    gradientBottom = Color(0xFF130F26),
    lilac = Color(0xFF30285A),
    sky = Color(0xFF1F2E52),
    mint = Color(0xFF1C3A30),
    peach = Color(0xFF45301F),
    pink = Color(0xFF45233A),
    aiBubble = Color(0xFF2E2656),
    userBubble = Color(0xFF243A22),
)

val LocalExtra = staticCompositionLocalOf { LightExtra }

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B3FD6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE8FF),
    onPrimaryContainer = Color(0xFF231B4D),
    secondary = Color(0xFF5E5785),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2EEFF),
    onSecondaryContainer = Color(0xFF231B4D),
    tertiary = Color(0xFFD2541E),
    onTertiary = Color(0xFFFFFFFF),
    error = Color(0xFFC93A3A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE4E1),
    onErrorContainer = Color(0xFF5F0A0A),
    background = Color(0xFFF4F1FF),
    onBackground = Color(0xFF231B4D),
    surface = Color(0xFFF4F1FF),
    onSurface = Color(0xFF231B4D),
    surfaceVariant = Color(0xFFF2EEFF),
    onSurfaceVariant = Color(0xFF575079),
    outline = Color(0xFF8E88AE),
    outlineVariant = Color(0xFFE3DDFB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F7FF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF2EEFF),
    surfaceContainerHighest = Color(0xFFEAE4FF),
    inverseSurface = Color(0xFF231B4D),
    inverseOnSurface = Color(0xFFF6F3FF),
    inversePrimary = Color(0xFFC6B8FF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC6B8FF),
    onPrimary = Color(0xFF2A1580),
    primaryContainer = Color(0xFF412BAA),
    onPrimaryContainer = Color(0xFFE8E0FF),
    secondary = Color(0xFFC9C2E6),
    onSecondary = Color(0xFF2E2850),
    secondaryContainer = Color(0xFF3A3263),
    onSecondaryContainer = Color(0xFFE6E0FF),
    tertiary = Color(0xFFFFB27A),
    onTertiary = Color(0xFF4A2200),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF5F0A0A),
    errorContainer = Color(0xFF7A1C1C),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF130F26),
    onBackground = Color(0xFFECE8FF),
    surface = Color(0xFF130F26),
    onSurface = Color(0xFFECE8FF),
    surfaceVariant = Color(0xFF2B2450),
    onSurfaceVariant = Color(0xFFC3BCE0),
    outline = Color(0xFF8A83A8),
    outlineVariant = Color(0xFF3A3263),
    surfaceContainerLowest = Color(0xFF0E0B1D),
    surfaceContainerLow = Color(0xFF1A1533),
    surfaceContainer = Color(0xFF221C3D),
    surfaceContainerHigh = Color(0xFF2B2450),
    surfaceContainerHighest = Color(0xFF342C5E),
    inverseSurface = Color(0xFFECE8FF),
    inverseOnSurface = Color(0xFF1A1533),
    inversePrimary = Color(0xFF5B3FD6),
)

/** Atkinson Hyperlegible Next: diseñada por el Braille Institute para baja visión. */
@OptIn(ExperimentalTextApi::class)
private fun atkinson(weight: Int) = Font(
    resId = R.font.atkinson_next,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val Atkinson = FontFamily(atkinson(400), atkinson(500), atkinson(700), atkinson(800))

private fun TextStyle.voice(weight: Int, tracking: Double = 0.0) = copy(
    fontFamily = Atkinson,
    fontWeight = FontWeight(weight),
    letterSpacing = tracking.em,
)

private val Base = Typography()

private val AppTypography = Typography(
    displayLarge = Base.displayLarge.voice(800, -0.02),
    displayMedium = Base.displayMedium.voice(800, -0.02),
    displaySmall = Base.displaySmall.voice(800, -0.02),
    headlineLarge = Base.headlineLarge.voice(800, -0.02),
    headlineMedium = Base.headlineMedium.voice(800, -0.015),
    headlineSmall = Base.headlineSmall.voice(800, -0.01),
    titleLarge = Base.titleLarge.voice(800, -0.01),
    titleMedium = Base.titleMedium.voice(700),
    titleSmall = Base.titleSmall.voice(700),
    bodyLarge = Base.bodyLarge.voice(400),
    bodyMedium = Base.bodyMedium.voice(400),
    bodySmall = Base.bodySmall.voice(500),
    labelLarge = Base.labelLarge.voice(700),
    labelMedium = Base.labelMedium.voice(700),
    labelSmall = Base.labelSmall.voice(700),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun FitInTheme(
    // Siempre claro, aunque el móvil esté en modo oscuro: es el estilo elegido para la demo.
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalExtra provides if (darkTheme) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
