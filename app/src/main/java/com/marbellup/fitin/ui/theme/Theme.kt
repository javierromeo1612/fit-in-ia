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
import androidx.compose.ui.unit.sp
import com.marbellup.fitin.R

/*
 * Estilo (rediseño 2026): app deportiva luminosa en lila y violeta, con fondos en degradado
 * suave, tarjetas blancas tipo cristal muy redondeadas, botones en píldora, fotos de cada
 * deporte y Cuac, el patito mascota de Fit-In como asistente.
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

/* Paleta tomada de la nueva referencia visual del equipo: lilas y violetas saturados, pasteles vivos. */
private val LightExtra = ExtraColors(
    card = Color(0xFFFFFFFF),
    chip = Color(0xFFDCEBFD),
    onChip = Color(0xFF022569),
    ok = Color(0xFF17704A),
    okContainer = Color(0xFFE2F5EC),
    onOkContainer = Color(0xFF0F4A30),
    warn = Color(0xFFA35D0A),
    warnContainer = Color(0xFFFFF0DB),
    onWarnContainer = Color(0xFF5C3304),
    barrier = Color(0xFFC93A3A),
    scrim = Color(0x99022569),
    notification = Color(0xFFC8361C),
    tile = Color(0xFFE6F2FE),
    tileSelected = Color(0xFF145BCC),
    hero = Color(0xFF145BCC),
    flame = Color(0xFFFF7A2F),
    gradientTop = Color(0xFF7EA6F0),
    gradientBottom = Color(0xFFCFE1FB),
    lilac = Color(0xFFD8E8FC),
    sky = Color(0xFFD3E6FF),
    mint = Color(0xFFCFF3DF),
    peach = Color(0xFFFFDDC4),
    pink = Color(0xFFFFD6E8),
    aiBubble = Color(0xFFE3F0FD),
    userBubble = Color(0xFFDDF3CF),
)

private val DarkExtra = ExtraColors(
    card = Color(0xFF13234A),
    chip = Color(0xFF1E3366),
    onChip = Color(0xFFE1EEFD),
    ok = Color(0xFF7AD3A2),
    okContainer = Color(0xFF1D3B2D),
    onOkContainer = Color(0xFFCFF3DF),
    warn = Color(0xFFF2B866),
    warnContainer = Color(0xFF41300F),
    onWarnContainer = Color(0xFFFFE3BD),
    barrier = Color(0xFFFF8A80),
    scrim = Color(0xA60A0618),
    notification = Color(0xFFFF6B5A),
    tile = Color(0xFF1B2B52),
    tileSelected = Color(0xFF2A7FE7),
    hero = Color(0xFF145BCC),
    flame = Color(0xFFFFA45C),
    gradientTop = Color(0xFF102A66),
    gradientBottom = Color(0xFF0A1630),
    lilac = Color(0xFF1A2C5C),
    sky = Color(0xFF1F2E52),
    mint = Color(0xFF1C3A30),
    peach = Color(0xFF45301F),
    pink = Color(0xFF45233A),
    aiBubble = Color(0xFF1A2E5E),
    userBubble = Color(0xFF243A22),
)

val LocalExtra = staticCompositionLocalOf { LightExtra }

private val LightColors = lightColorScheme(
    primary = Color(0xFF145BCC),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6F2FE),
    onPrimaryContainer = Color(0xFF022569),
    secondary = Color(0xFF3D5285),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEEF6FF),
    onSecondaryContainer = Color(0xFF022569),
    tertiary = Color(0xFFD2541E),
    onTertiary = Color(0xFFFFFFFF),
    error = Color(0xFFC93A3A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE4E1),
    onErrorContainer = Color(0xFF5F0A0A),
    background = Color(0xFFEAF4FF),
    onBackground = Color(0xFF022569),
    surface = Color(0xFFEAF4FF),
    onSurface = Color(0xFF022569),
    surfaceVariant = Color(0xFFEEF6FF),
    onSurfaceVariant = Color(0xFF46557f),
    outline = Color(0xFF8A9BC2),
    outlineVariant = Color(0xFFD6E6FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7FBFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEEF6FF),
    surfaceContainerHighest = Color(0xFFE1EEFD),
    inverseSurface = Color(0xFF022569),
    inverseOnSurface = Color(0xFFF3F8FF),
    inversePrimary = Color(0xFF9FCFF7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FCFF7),
    onPrimary = Color(0xFF022569),
    primaryContainer = Color(0xFF012F8E),
    onPrimaryContainer = Color(0xFFDDEBFC),
    secondary = Color(0xFFB7C8E6),
    onSecondary = Color(0xFF15264F),
    secondaryContainer = Color(0xFF25386A),
    onSecondaryContainer = Color(0xFFDDEBFC),
    tertiary = Color(0xFFFFB27A),
    onTertiary = Color(0xFF4A2200),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF5F0A0A),
    errorContainer = Color(0xFF7A1C1C),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0A1630),
    onBackground = Color(0xFFE6F1FD),
    surface = Color(0xFF0A1630),
    onSurface = Color(0xFFE6F1FD),
    surfaceVariant = Color(0xFF1B2B52),
    onSurfaceVariant = Color(0xFFB8C7E6),
    outline = Color(0xFF7F93BA),
    outlineVariant = Color(0xFF25386A),
    surfaceContainerLowest = Color(0xFF07101F),
    surfaceContainerLow = Color(0xFF101F42),
    surfaceContainer = Color(0xFF13234A),
    surfaceContainerHigh = Color(0xFF1B2B52),
    surfaceContainerHighest = Color(0xFF213668),
    inverseSurface = Color(0xFFE6F1FD),
    inverseOnSurface = Color(0xFF101F42),
    inversePrimary = Color(0xFF145BCC),
)

/**
 * Atkinson Hyperlegible Next: diseñada por el Braille Institute para baja visión.
 * En versiones fijas (normal, media, negrita y extranegrita): algunos móviles (HyperOS)
 * no aplican la negrita de la fuente variable.
 */
private val Atkinson = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_medium, FontWeight.Medium),
    Font(R.font.atkinson_bold, FontWeight.Bold),
    Font(R.font.atkinson_extrabold, FontWeight.ExtraBold),
)

/**
 * Nunito: redondeada y gruesa, como la tipografía de la referencia visual (licencia OFL).
 * Un archivo fijo por grosor: HyperOS (Xiaomi) ignora el eje de grosor de la fuente variable
 * y todos los títulos salían finos en el móvil de la demo.
 */
private val Nunito = FontFamily(
    Font(R.font.nunito_medium, FontWeight(500)),
    Font(R.font.nunito_semibold, FontWeight(600)),
    Font(R.font.nunito_bold, FontWeight(700)),
    Font(R.font.nunito_extrabold, FontWeight(800)),
    Font(R.font.nunito_black, FontWeight(900)),
)

private fun TextStyle.voice(family: FontFamily, weight: Int, tracking: Double = 0.0) = copy(
    fontFamily = family,
    fontWeight = FontWeight(weight),
    letterSpacing = tracking.em,
)

private val Base = Typography()

private fun TextStyle.size(sp: Int, line: Int) = copy(fontSize = sp.sp, lineHeight = line.sp)

/** Tamaños compactos, medidos sobre la referencia visual. */
private fun typography(f: FontFamily, body: Int) = Typography(
    displayLarge = Base.displayLarge.voice(f, 900, -0.02),
    displayMedium = Base.displayMedium.voice(f, 900, -0.02).size(38, 44),
    displaySmall = Base.displaySmall.voice(f, 900, -0.02).size(32, 38),
    headlineLarge = Base.headlineLarge.voice(f, 900, -0.02).size(28, 34),
    headlineMedium = Base.headlineMedium.voice(f, 900, -0.015).size(25, 31),
    headlineSmall = Base.headlineSmall.voice(f, 900, -0.01).size(22, 28),
    titleLarge = Base.titleLarge.voice(f, 800, -0.01).size(18, 24),
    titleMedium = Base.titleMedium.voice(f, 800).size(15, 20),
    titleSmall = Base.titleSmall.voice(f, 800).size(13, 18),
    bodyLarge = Base.bodyLarge.voice(f, body).size(15, 21),
    bodyMedium = Base.bodyMedium.voice(f, body).size(13, 18),
    bodySmall = Base.bodySmall.voice(f, body).size(12, 16),
    labelLarge = Base.labelLarge.voice(f, 800).size(13, 18),
    labelMedium = Base.labelMedium.voice(f, 800).size(11, 14),
    labelSmall = Base.labelSmall.voice(f, 800).size(10, 13),
)

/** Estilo de la referencia (Nunito) y alternativa de lectura fácil (Atkinson Hyperlegible). */
private val AppTypography = typography(Nunito, 600)
private val EasyTypography = typography(Atkinson, 400)

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
    /** Lectura fácil: cambia a Atkinson Hyperlegible, diseñada para baja visión. */
    easyRead: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalExtra provides if (darkTheme) DarkExtra else LightExtra) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = if (easyRead) EasyTypography else AppTypography,
            shapes = AppShapes,
        ) {
            // Texto en azul marino-violeta como en la referencia (no negro), también sobre fondos transparentes.
            CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides (if (darkTheme) DarkColors else LightColors).onBackground,
                content = content,
            )
        }
    }
}
