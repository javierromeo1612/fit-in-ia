package com.marbellup.fitin.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.lerp
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.Step
import com.marbellup.fitin.Tab
import com.marbellup.fitin.data.Activity
import com.marbellup.fitin.data.AppNotification
import com.marbellup.fitin.data.Need
import com.marbellup.fitin.data.Scored
import com.marbellup.fitin.data.Slot
import com.marbellup.fitin.data.Sport
import com.marbellup.fitin.ui.theme.LocalExtra

/*
 * Piezas del rediseño 2026 (lila, cristal, mascota). Las piezas antiguas siguen en Components.kt.
 */

/** Versión pastel de un color: fondo de mosaicos, chips y tarjetas de cada deporte. */
fun Color.pastel(amount: Float = 0.22f): Color = lerp(Color.White, this, amount)

/** Versión oscura de un color, para textos sobre su pastel (mantiene el contraste AA). */
fun Color.deeper(amount: Float = 0.35f): Color = lerp(this, Color(0xFF07173F), amount)

/** Fondo pastel propio de cada deporte (naranja para baloncesto, azul para natación…). */
val Sport.pastel: Color get() = tint.pastel()

/** ¿Ha quitado la persona las animaciones en los ajustes del sistema? Entonces todo queda quieto. */
@Composable
fun rememberReducedMotion(): Boolean {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    return remember(ctx) {
        android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Una nube esponjosa: varias bolas blancas con la base en lila suave, como las de la bienvenida. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.nube(cx: Float, cy: Float, r: Float, alpha: Float) {
    val base = Color(0xFFE3DCFB).copy(alpha = alpha)
    val blanco = Color.White.copy(alpha = alpha)
    val bolas = listOf(Triple(-1.1f, 0.25f, 0.62f), Triple(-0.45f, -0.15f, 0.82f), Triple(0.35f, -0.3f, 0.95f), Triple(1.1f, 0.15f, 0.7f))
    // Sombra lila debajo y, encima, el cuerpo blanco un poco más arriba: da volumen.
    for ((dx, dy, k) in bolas) drawCircle(base, radius = r * k, center = Offset(cx + dx * r, cy + dy * r + r * 0.12f))
    for ((dx, dy, k) in bolas) drawCircle(blanco, radius = r * k * 0.94f, center = Offset(cx + dx * r, cy + dy * r))
}

/**
 * Fondo de todas las pantallas: un cielo como el de la bienvenida. Degradado azul cielo que se
 * aclara hacia abajo, brillo de sol que respira, nubes esponjosas que se desplazan despacio y
 * destellos que parpadean. Todo se dibuja (no son imágenes) y las animaciones se leen en la
 * fase de dibujo, así que no recomponen la pantalla.
 */
@Composable
fun GradientBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val x = LocalExtra.current
    val bg = MaterialTheme.colorScheme.background
    val quieto = rememberReducedMotion()
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "cielo")
    val deriva = t.animateFloat(
        0f, if (quieto) 0f else 1f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(90_000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "nubes",
    )
    val pulso = t.animateFloat(
        0f, if (quieto) 0f else 1f,
        androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(3_200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "sol",
    )
    // Nubes: (x inicial 0..1, y 0..1, tamaño respecto al ancho, velocidad relativa, opacidad).
    val nubes = listOf(
        floatArrayOf(0.12f, 0.06f, 0.11f, 1.0f, 0.95f),
        floatArrayOf(0.78f, 0.12f, 0.13f, 0.7f, 0.9f),
        floatArrayOf(0.45f, 0.30f, 0.08f, 1.3f, 0.75f),
        floatArrayOf(0.92f, 0.46f, 0.09f, 0.9f, 0.6f),
        floatArrayOf(0.05f, 0.62f, 0.10f, 0.8f, 0.5f),
    )
    val destellos = listOf(0.18f to 0.16f, 0.86f to 0.24f, 0.62f to 0.40f, 0.10f to 0.48f, 0.90f to 0.70f)
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to x.gradientTop, 0.45f to x.gradientBottom, 1f to bg))
            .drawBehind {
                val w = size.width; val h = size.height
                // Brillo de sol arriba, al centro.
                val sol = Offset(w * 0.5f, h * 0.02f)
                val rs = w * (0.55f + 0.05f * pulso.value)
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), center = sol, radius = rs), radius = rs, center = sol)
                // Nubes que cruzan el cielo y vuelven a entrar por la izquierda.
                for (n in nubes) {
                    val r = w * n[2]
                    val recorrido = w + r * 5f
                    val px = ((n[0] * w + deriva.value * recorrido * n[3]) % recorrido) - r * 2.5f
                    nube(px, h * n[1], r, n[4])
                }
                // Destellos que parpadean, cada uno a su ritmo.
                destellos.forEachIndexed { i, (fx, fy) ->
                    val a = 0.35f + 0.55f * kotlin.math.abs(kotlin.math.sin((pulso.value + i * 0.37f) * Math.PI.toFloat()))
                    drawCircle(Color.White.copy(alpha = a), radius = 2.5.dp.toPx(), center = Offset(w * fx, h * fy))
                }
            },
        content = content,
    )
}

/** Degradado azul del logo de los botones principales, el «+» y las insignias. */
@Composable
fun primaryBrush(): Brush = Brush.linearGradient(listOf(Color(0xFF2A7FE7), LocalExtra.current.hero, MaterialTheme.colorScheme.primary))

/** Degradado de color para tarjetas destacadas (azul → azul medio). */
@Composable
fun heroBrush(): Brush = Brush.linearGradient(listOf(LocalExtra.current.hero, Color(0xFF2A7FE7)))

/** Colores vivos que se van turnando en mosaicos y menús (violeta, azul, naranja, verde, rosa, turquesa). */
private val funColors = listOf(
    Color(0xFF145BCC), Color(0xFF1C8FE0), Color(0xFFF0742A), Color(0xFF2E9D4F), Color(0xFFE0458B), Color(0xFF14A0A0),
)

fun funColor(i: Int): Color = funColors[((i % funColors.size) + funColors.size) % funColors.size]

/** Icono dentro de un cuadrado redondeado de color pastel, como los iconos de la referencia. */
@Composable
fun IconBadge(icon: ImageVector, color: Color, size: Dp = 36.dp, filled: Boolean = false) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.34f)).background(if (filled) color else color.pastel(0.2f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = if (filled) Color.White else color, modifier = Modifier.size(size * 0.58f)) }
}

/** Poses de Cuac, el patito mascota de Fit-In (fotos en res/drawable-nodpi/cuac_*.png). */
enum class Cuac(val image: Int) {
    Saluda(com.marbellup.fitin.R.drawable.cuac_saluda),     // hablando, el de siempre
    Feliz(com.marbellup.fitin.R.drawable.cuac_feliz),       // saltando con las alas abiertas
    Piensa(com.marbellup.fitin.R.drawable.cuac_piensa),     // con el ala en la barbilla
    Duerme(com.marbellup.fitin.R.drawable.cuac_duerme),     // ojos cerrados: nada que hacer
    Enfadado(com.marbellup.fitin.R.drawable.cuac_enfadado), // ceño fruncido: algo ha ido mal
}

/**
 * Mascota de Fit-In: Cuac, un patito amarillo. [happy] lo pone saltando de alegría;
 * [pose] elige cualquier otra postura.
 *
 * Está vivo: se contonea apoyado en las patas, respira (se estira y se aplasta un poco),
 * y cada pocos segundos da un saltito de alegría (con las alas abiertas si no tiene una
 * postura fija) y aterriza aplastándose. Una sombra suave en el suelo, que se encoge al
 * saltar, le da volumen. Cada pato lleva su propio ritmo para que no se muevan todos a la
 * vez. Con «Quitar animaciones» del sistema se queda quieto.
 */
@Composable
fun Mascot(size: Dp = 64.dp, modifier: Modifier = Modifier, happy: Boolean = false, pose: Cuac? = null, decorativo: Boolean = false) {
    val quieto = rememberReducedMotion()
    // Ritmo propio de cada pato (estable mientras esté en pantalla).
    val semilla = remember { kotlin.random.Random.nextInt(1000) }
    val t = rememberInfiniteTransition(label = "cuac")
    val vaiven by t.animateFloat(
        0f, if (quieto) 0f else 1f,
        infiniteRepeatable(tween(1700 + semilla % 400, easing = androidx.compose.animation.core.LinearEasing)),
        label = "contoneo",
    )
    val salto = remember { androidx.compose.animation.core.Animatable(0f) }
    val aterriza = remember { androidx.compose.animation.core.Animatable(0f) }
    var alegre by remember { mutableStateOf(false) }
    LaunchedEffect(quieto) {
        if (quieto) return@LaunchedEffect
        kotlinx.coroutines.delay(900L + semilla * 3L)
        while (true) {
            alegre = true
            salto.animateTo(1f, tween(260, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            salto.animateTo(0f, tween(240, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
            alegre = false
            aterriza.snapTo(1f)
            aterriza.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.3f, stiffness = 500f))
            kotlinx.coroutines.delay(4200L + (semilla % 7) * 450L)
        }
    }
    val p = pose ?: if (happy || alegre) Cuac.Feliz else Cuac.Saluda
    Box(
        modifier
            .size(size)
            .drawBehind {
                // Sombra en el suelo: más pequeña y clara cuanto más alto salta.
                val k = 1f - 0.45f * salto.value
                val ancho = this.size.width * 0.55f * k
                drawOval(
                    Color(0xFF022569).copy(alpha = 0.16f * k),
                    topLeft = Offset((this.size.width - ancho) / 2, this.size.height * 0.93f),
                    size = androidx.compose.ui.geometry.Size(ancho, this.size.height * 0.06f),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(p.image),
            contentDescription = if (decorativo) null else "Cuac, la mascota de Fit-In",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                val fase = vaiven * 2f * Math.PI.toFloat()
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.95f)
                // Contoneo y respiración.
                rotationZ = kotlin.math.sin(fase) * 5f
                val respira = kotlin.math.sin(fase * 2f) * 0.025f
                // Al aterrizar se aplasta y vuelve con un rebote.
                val aplasta = aterriza.value * 0.12f
                scaleX = 1f + respira + aplasta
                scaleY = 1f - respira - aplasta + salto.value * 0.06f
                translationY = -salto.value * this.size.height * 0.16f
            },
        )
    }
}

/** Botón principal en degradado azul, en píldora y a todo lo ancho. */
@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward,
    loading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val glow = LocalExtra.current.hero
    Surface(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = shape,
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .then(if (enabled) Modifier.shadow(12.dp, shape, ambientColor = glow, spotColor = glow) else Modifier),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .background(if (enabled) primaryBrush() else Brush.linearGradient(listOf(Color(0xFFADBFDC), Color(0xFFADBFDC))), shape)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
            }
            Text(text, style = MaterialTheme.typography.titleMedium, color = Color.White)
            if (icon != null && !loading) {
                Spacer(Modifier.width(10.dp))
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/** Tarjeta blanca tipo cristal, muy redondeada, con brillo azul. Al pulsarla se encoge un poco. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    color: Color = LocalExtra.current.card,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val glow = LocalExtra.current.hero
    val border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.9f))
    if (onClick != null) {
        val source = remember { MutableInteractionSource() }
        val pressed by source.collectIsPressedAsState()
        val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "pulsar")
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(10.dp, shape, ambientColor = glow.copy(alpha = 0.35f), spotColor = glow.copy(alpha = 0.45f)),
            shape = shape, color = color, interactionSource = source, border = border,
        ) { Column(content = content) }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth().shadow(10.dp, shape, ambientColor = glow.copy(alpha = 0.35f), spotColor = glow.copy(alpha = 0.45f)),
            shape = shape, color = color, border = border,
        ) { Column(content = content) }
    }
}

/** Botón redondo de icono (volver, campana, ajustes…) con objetivo táctil de 48 dp o más. */
@Composable
fun RoundIconButton(icon: ImageVector, description: String, badge: Int = 0, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = LocalExtra.current.card, shadowElevation = 3.dp, modifier = Modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = if (badge > 0) "$description, $badge sin leer" else description,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            if (badge > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(top = 5.dp, end = 5.dp).size(16.dp).clip(CircleShape)
                        .background(LocalExtra.current.notification),
                    contentAlignment = Alignment.Center,
                ) { Text("$badge", color = Color.White, style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

/** Caja «por qué te lo recomienda la IA». */
@Composable
fun AiReason(text: String, modifier: Modifier = Modifier, title: String = "Por qué te lo recomiendo") {
    val x = LocalExtra.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(x.aiBubble, x.sky)))
            .padding(12.dp)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.Top,
    ) {
        Mascot(40.dp, pose = Cuac.Piensa, decorativo = true)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Píldora de necesidad: verde si está cubierta, ámbar si falta, lila si es neutra. */
@Composable
fun NeedPill(text: String, ok: Boolean? = null) {
    val x = LocalExtra.current
    val (bg, fg, icon) = when (ok) {
        true -> Triple(x.okContainer, x.onOkContainer, Icons.Filled.Check)
        false -> Triple(x.warnContainer, x.onWarnContainer, Icons.Filled.ErrorOutline)
        null -> Triple(x.chip, x.onChip, null)
    }
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = if (ok == true) "Cubierto:" else "Falta:", tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

/**
 * Tarjeta de actividad con foto, como en la referencia: foto redondeada, corazón,
 * porcentaje de encaje de la IA y bloque blanco con título, fecha, sitio y flecha.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityPhotoCard(
    a: Activity,
    state: AppState,
    rec: Scored? = null,
    modifier: Modifier = Modifier,
    showReason: Boolean = true,
) {
    val x = LocalExtra.current
    GlassCard(modifier, onClick = { state.open(Step.Detail(a.id)) }) {
        // Como en la referencia: foto grande con la fecha y el corazón encima, y un panel blanco abajo.
        Box(Modifier.fillMaxWidth().height(224.dp).padding(6.dp)) {
            ActivityCover(a, Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)))
            // Velo de color del deporte para que la foto «combine» con la tarjeta
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(a.sport.tint.copy(alpha = 0.28f), Color.Transparent, Color.Transparent))))
            Row(Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Chip de fecha, como «Sáb 24 nov» en la referencia
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.94f)).padding(start = 8.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = a.sport.tint, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(a.shortDate, style = MaterialTheme.typography.labelMedium, color = Color(0xFF022569), maxLines = 1)
                }
                if (a.status == "propuesta") {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Grupo nuevo",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(x.flame).padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                val fav = a.id in state.favorites
                Surface(onClick = { state.toggleFavorite(a.id) }, shape = CircleShape, color = Color.White, shadowElevation = 3.dp,
                    modifier = Modifier.size(38.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (fav) "Quitar de favoritos" else "Guardar en favoritos",
                            tint = if (fav) Color(0xFFE0447A) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.latido(fav),
                        )
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = x.card.copy(alpha = 0.97f),
                shadowElevation = 3.dp,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(8.dp),
            ) {
                Row(Modifier.padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Icono del deporte en su color
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(a.sport.pastel), contentAlignment = Alignment.Center) {
                        Text(a.sport.emoji, fontSize = 22.sp)
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(a.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false))
                            if (rec != null) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "✨ ${rec.match}%", color = Color.White, style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.clip(RoundedCornerShape(50)).background(primaryBrush()).padding(horizontal = 7.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(15.dp), tint = a.sport.tint)
                            Spacer(Modifier.width(4.dp))
                            Text(a.place, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Van ${a.group.size} · " + if (a.spotsLeft == 1) "queda 1 plaza" else "quedan ${a.spotsLeft} plazas",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                            )
                        }
                    }
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFC8F26A)), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF24411A))
                    }
                }
            }
        }
        if (rec != null && showReason) {
            AiReason(rec.explanation, Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp))
            if (rec.covered.isNotEmpty() || rec.missing.isNotEmpty()) {
                FlowRow(
                    Modifier.padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    rec.covered.take(4).forEach { NeedPill(it.label, true) }
                    rec.missing.forEach { NeedPill(it.label, false) }
                }
            }
        }
    }
}

/** Barra inferior de la referencia: cuatro pestañas y el «+» central, grande y en degradado. */
@Composable
fun FitInBottomBar(state: AppState) {
    val x = LocalExtra.current
    val glow = x.hero
  Box {
    Surface(
        color = x.card,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Row(
            Modifier.navigationBarsPadding().fillMaxWidth().height(70.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabItem(state, Tab.Inicio, Icons.Filled.Home, Icons.Outlined.Home)
            TabItem(state, Tab.Calendario, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Surface(
                    onClick = { state.open(Step.Create) },
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier.size(56.dp).shadow(12.dp, CircleShape, ambientColor = glow, spotColor = glow),
                ) {
                    Box(
                        Modifier.background(primaryBrush(), CircleShape).border(3.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Crear actividad", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }
            }
            TabItem(state, Tab.ParaTi, Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline)
            TabItem(state, Tab.Perfil, Icons.Filled.Person, Icons.Outlined.Person)
        }
    }
    // Cuac de pie sobre la barra, siempre a la vista.
    Mascot(46.dp, Modifier.align(Alignment.TopStart).offset(x = 18.dp, y = (-38).dp), decorativo = true)
  }
}

@Composable
private fun RowScope.TabItem(state: AppState, tab: Tab, selectedIcon: ImageVector, icon: ImageVector) {
    val selected = state.tab == tab
    val color by androidx.compose.animation.animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, label = "colorPestaña",
    )
    val pildora by androidx.compose.animation.animateColorAsState(
        if (selected) LocalExtra.current.lilac else Color.Transparent, label = "píldora",
    )
    val salto by animateFloatAsState(
        if (selected) 1.18f else 1f,
        androidx.compose.animation.core.spring(dampingRatio = 0.35f, stiffness = 420f), label = "saltoPestaña",
    )
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .clickable { state.steps.clear(); state.tab = tab }
            .semantics { role = Role.Tab; this.selected = selected }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // La pestaña activa lleva una píldora lila detrás del icono
        Box(
            Modifier.clip(RoundedCornerShape(50))
                .background(pildora)
                .padding(horizontal = 14.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (selected) selectedIcon else icon, contentDescription = null, tint = color,
                modifier = Modifier.size(22.dp).graphicsLayer { scaleX = salto; scaleY = salto })
        }
        Text(tab.label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold)
    }
}

/** Aviso flotante cuando llega una invitación inteligente. */
@Composable
fun NotificationBanner(n: AppNotification, onOpen: () -> Unit, onDismiss: () -> Unit) {
    GlassCard(Modifier.padding(horizontal = 12.dp), onClick = onOpen) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Mascot(48.dp)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(n.title, style = MaterialTheme.typography.titleSmall)
                Text(n.body, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Cerrar aviso") }
        }
    }
}

/** Mosaico de categoría (deporte) con icono en círculo pastel y etiqueta debajo. */
@Composable
fun CategoryTile(label: String, icon: ImageVector, tint: Color, bg: Color, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .width(78.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .semantics { role = Role.Button; this.selected = selected }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(bg)
                .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp)) else Modifier),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp)) }
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Mini fila de actividad (chat, notificaciones). */
@Composable
fun ActivityMiniRow(a: Activity, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LocalExtra.current.card)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActivityCover(a, Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)), small = true)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(a.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle ?: "${a.shortDate} · ${a.place}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

/** Fila de menú del perfil, como en la referencia: icono violeta, texto y «>». */
@Composable
fun MenuRow(
    icon: ImageVector,
    title: String,
    detail: String? = null,
    badge: Int = 0,
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 54.dp)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, color, size = 38.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (badge > 0) {
            Text(
                "$badge", color = Color.White, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(LocalExtra.current.notification).padding(horizontal = 7.dp, vertical = 1.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
    }
}

/** Etiqueta que recuerda que la IA funciona en el móvil y cuánto ha aprendido ya. */
@Composable
fun AiTag(state: AppState) {
    state.modelVersion
    Text(
        "IA en tu móvil · ${state.history.size} ${if (state.history.size == 1) "acción aprendida" else "acciones aprendidas"}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(LocalExtra.current.chip).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Necesidad → texto corto de chip, como en la referencia («Silla de ruedas», «Aseo adaptado»). */
val Need.shortLabel: String
    get() = when (this) {
        Need.SinEscalones -> "Sin escalones"
        Need.Vestuario -> "Aseo adaptado"
        Need.Aparcamiento -> "Aparcamiento"
        Need.Cubierta -> "Cubierta"
        Need.Grua -> "Grúa"
        Need.Signos -> "Lengua de signos"
        Need.Acompanante -> "Acompañamiento"
        Need.Material -> "Material adaptado"
        Need.Monitor -> "Monitor"
        Need.GrupoReducido -> "Grupo pequeño"
        Need.Guia -> "Persona guía"
        Need.Bucle -> "Bucle magnético"
        Need.Tranquilo -> "Sin mucho ruido"
        Need.LecturaFacil -> "Lectura Fácil"
        Need.Transporte -> "Transporte"
        Need.PuertasAnchas -> "Puertas anchas"
        Need.DistanciasCortas -> "Distancias cortas"
        Need.BanoAdaptado -> "Baño adaptado"
        Need.AyudaCambiarse -> "Ayuda al cambiarse"
        Need.DuchaAdaptada -> "Ducha con asiento"
        Need.VestuarioIndividual -> "Vestuario individual"
        Need.BanoCerca -> "Baño cerca"
        Need.AltoContraste -> "Alto contraste"
        Need.Iluminacion -> "Buena luz"
        Need.Audiodescripcion -> "Explicación en voz"
        Need.PerroAsistencia -> "Perro de asistencia"
        Need.AvisosVisuales -> "Avisos visuales"
        Need.Vocaliza -> "Hablar claro"
        Need.InfoEscrita -> "Info por escrito"
        Need.Saac -> "Comunicación con tablero"
        Need.PasoAPaso -> "Paso a paso"
        Need.MasTiempo -> "Más tiempo"
        Need.LuzSuave -> "Luz suave"
        Need.EspacioCalma -> "Sitio de calma"
        Need.ConocerAntes -> "Conocerlo antes"
        Need.AvisarCambios -> "Avisar de cambios"
        Need.Acogida -> "Recibirme al llegar"
        Need.IntensidadBaja -> "Intensidad baja"
        Need.Pausas -> "Pausas"
        Need.Sentado -> "Sentado"
        Need.SinCompetir -> "Sin competir"
        Need.Familiar -> "Con un familiar"
        Need.MismoGrupo -> "Mismo grupo"
        Need.MismaEdad -> "Gente de mi edad"
        Need.PrimerosAuxilios -> "Primeros auxilios"
        Need.Medicacion -> "Medicación"
        Need.SombraAgua -> "Sombra y agua"
        Need.AguaTemplada -> "Agua climatizada"
        Need.Gratuita -> "Gratis o barata"
        else -> label
    }

/** Icono de cada necesidad, para elegirlas tocando (sin escribir). */
val Need.icon: ImageVector
    get() = when (this) {
        Need.SinEscalones -> Icons.AutoMirrored.Filled.Accessible
        Need.Aparcamiento -> Icons.Filled.LocalParking
        Need.Vestuario -> Icons.Filled.Wc
        Need.Grua -> Icons.Filled.Pool
        Need.Transporte -> Icons.Filled.DirectionsBus
        Need.Cubierta -> Icons.Filled.Roofing
        Need.Signos -> Icons.Filled.SignLanguage
        Need.Bucle -> Icons.Filled.Hearing
        Need.Guia -> Icons.Filled.VisibilityOff
        Need.Tranquilo -> Icons.AutoMirrored.Filled.VolumeOff
        Need.LecturaFacil -> Icons.AutoMirrored.Filled.MenuBook
        Need.Pictogramas -> Icons.Filled.Image
        Need.GrupoReducido -> Icons.Filled.Groups
        Need.Material -> Icons.Filled.Settings
        Need.Monitor -> Icons.Filled.SupportAgent
        Need.Acompanante -> Icons.Filled.Handshake
        Need.Ascensor -> Icons.AutoMirrored.Filled.Accessible
        Need.PuertasAnchas -> Icons.AutoMirrored.Filled.Accessible
        Need.DistanciasCortas -> Icons.AutoMirrored.Filled.Accessible
        Need.BanoAdaptado -> Icons.Filled.Wc
        Need.AyudaCambiarse -> Icons.Filled.Wc
        Need.DuchaAdaptada -> Icons.Filled.Wc
        Need.VestuarioIndividual -> Icons.Filled.Wc
        Need.BanoCerca -> Icons.Filled.Wc
        Need.AltoContraste -> Icons.Filled.Visibility
        Need.Iluminacion -> Icons.Filled.Lightbulb
        Need.Audiodescripcion -> Icons.Filled.Hearing
        Need.PerroAsistencia -> Icons.Filled.Handshake
        Need.AvisosVisuales -> Icons.Filled.NotificationsActive
        Need.Vocaliza -> Icons.Filled.Hearing
        Need.InfoEscrita -> Icons.AutoMirrored.Filled.MenuBook
        Need.Saac -> Icons.Filled.Image
        Need.PasoAPaso -> Icons.AutoMirrored.Filled.MenuBook
        Need.MasTiempo -> Icons.Filled.AccessTime
        Need.LuzSuave -> Icons.Filled.Nightlight
        Need.EspacioCalma -> Icons.Filled.SelfImprovement
        Need.ConocerAntes -> Icons.Filled.Place
        Need.AvisarCambios -> Icons.Filled.NotificationsActive
        Need.Acogida -> Icons.Filled.VolunteerActivism
        Need.IntensidadBaja -> Icons.Filled.Spa
        Need.Pausas -> Icons.Filled.AccessTime
        Need.Sentado -> Icons.AutoMirrored.Filled.Accessible
        Need.SinCompetir -> Icons.Filled.SentimentSatisfied
        Need.Familiar -> Icons.Filled.Groups
        Need.MismoGrupo -> Icons.Filled.Groups
        Need.MismaEdad -> Icons.Filled.Groups
        Need.PrimerosAuxilios -> Icons.Filled.HealthAndSafety
        Need.Medicacion -> Icons.Filled.HealthAndSafety
        Need.SombraAgua -> Icons.Filled.WbSunny
        Need.AguaTemplada -> Icons.Filled.Pool
        Need.Gratuita -> Icons.Filled.Star
    }

val Slot.icon: ImageVector
    get() = when (this) {
        Slot.Manana -> Icons.Filled.WbSunny
        Slot.Mediodia -> Icons.Filled.LunchDining
        Slot.Tarde -> Icons.Filled.WbTwilight
        Slot.Noche -> Icons.Filled.NightsStay
    }

/**
 * Mosaico seleccionable como los de «Crear actividad» de la referencia: icono sobre fondo pastel
 * y texto debajo. Marcado = borde violeta y check. Objetivo táctil amplio.
 */
@Composable
fun SelectTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    bg: Color = tint.pastel(0.14f),
    emoji: String? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else if (selected) 1.03f else 1f, label = "mosaico")
    Surface(
        onClick = onClick,
        interactionSource = source,
        shape = shape,
        color = if (selected) tint.pastel(0.34f) else bg,
        shadowElevation = if (selected) 4.dp else 1.dp,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .heightIn(min = 80.dp)
            .then(if (selected) Modifier.border(2.5.dp, tint, shape) else Modifier)
            .semantics { role = Role.Checkbox; this.selected = selected },
    ) {
        Box {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (emoji != null) Text(emoji, fontSize = 26.sp) else Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (selected) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp).clip(CircleShape)
                        .background(tint),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp)) }
            }
        }
    }
}

val Need.emoji: String
    get() = when (this) {
        Need.SinEscalones -> "♿"
        Need.Aparcamiento -> "🅿️"
        Need.Vestuario -> "🚻"
        Need.Grua -> "🏗️"
        Need.Transporte -> "🚌"
        Need.Cubierta -> "🏠"
        Need.Signos -> "🤟"
        Need.Bucle -> "👂"
        Need.Guia -> "🦯"
        Need.Tranquilo -> "🤫"
        Need.LecturaFacil -> "📘"
        Need.Pictogramas -> "🖼️"
        Need.GrupoReducido -> "👥"
        Need.Material -> "🧰"
        Need.Monitor -> "🧑‍🏫"
        Need.Acompanante -> "🤝"
        Need.Ascensor -> "🛗"
        Need.PuertasAnchas -> "🚪"
        Need.DistanciasCortas -> "🪑"
        Need.BanoAdaptado -> "🚽"
        Need.AyudaCambiarse -> "👕"
        Need.DuchaAdaptada -> "🚿"
        Need.VestuarioIndividual -> "🔐"
        Need.BanoCerca -> "🧻"
        Need.AltoContraste -> "🔆"
        Need.Iluminacion -> "💡"
        Need.Audiodescripcion -> "🗣️"
        Need.PerroAsistencia -> "🐕‍🦺"
        Need.AvisosVisuales -> "🚨"
        Need.Vocaliza -> "👄"
        Need.InfoEscrita -> "📝"
        Need.Saac -> "📲"
        Need.PasoAPaso -> "🔢"
        Need.MasTiempo -> "⏳"
        Need.LuzSuave -> "🌙"
        Need.EspacioCalma -> "🧘"
        Need.ConocerAntes -> "🗺️"
        Need.AvisarCambios -> "🔔"
        Need.Acogida -> "👋"
        Need.IntensidadBaja -> "🐢"
        Need.Pausas -> "⏸️"
        Need.Sentado -> "💺"
        Need.SinCompetir -> "😊"
        Need.Familiar -> "👨‍👩‍👧"
        Need.MismoGrupo -> "🔁"
        Need.MismaEdad -> "🧑‍🤝‍🧑"
        Need.PrimerosAuxilios -> "⛑️"
        Need.Medicacion -> "💊"
        Need.SombraAgua -> "⛱️"
        Need.AguaTemplada -> "🌡️"
        Need.Gratuita -> "🆓"
    }

val Slot.emoji: String
    get() = when (this) {
        Slot.Manana -> "🌅"
        Slot.Mediodia -> "☀️"
        Slot.Tarde -> "🌇"
        Slot.Noche -> "🌙"
    }

/** Paletas de portada por deporte (dos tonos), inspiradas en los pasteles saturados de la referencia. */
private fun coverColors(sport: com.marbellup.fitin.data.Sport, variant: Int): List<Color> {
    val base = sport.tint
    val palettes = listOf(
        listOf(Color(0xFF2A7FE7), base),
        listOf(base, Color(0xFFFFB86B)),
        listOf(Color(0xFF5ED3C4), base),
        listOf(Color(0xFFFF8FB8), Color(0xFF2A7FE7)),
    )
    return palettes[variant % palettes.size]
}

/**
 * Portada de una actividad: la foto del deporte si es la principal; si no, una ilustración
 * de colores con el emoji del deporte, círculos y líneas de pista (distinta en cada variante).
 */
@Composable
fun ActivityCover(a: Activity, modifier: Modifier = Modifier, small: Boolean = false) {
    // Siempre foto real: la concreta de la actividad (photoKey) o la de su deporte.
    // Se busca por nombre (foto_<deporte>.jpg en res/drawable-nodpi), así basta con añadir
    // el archivo para que un deporte nuevo tenga foto. Si no hay ninguna, se dibuja la portada.
    val context = LocalContext.current
    val photo = remember(a.id) {
        fun find(name: String): Int? =
            context.resources.getIdentifier("foto_$name", "drawable", context.packageName).takeIf { it != 0 }
        a.photoKey?.let { find(it) } ?: find(a.sport.name.lowercase()) ?: a.sport.photo
    }
    if (photo != null) {
        Image(painterResource(photo), contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
        return
    }
    val colors = coverColors(a.sport, a.cover)
    Box(modifier.background(Brush.linearGradient(colors)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawCircle(Color.White.copy(alpha = 0.18f), radius = h * 0.55f, center = Offset(w * 0.85f, h * 0.15f))
            drawCircle(Color.White.copy(alpha = 0.12f), radius = h * 0.35f, center = Offset(w * 0.1f, h * 0.95f))
            // Líneas de «pista»
            val stroke = Stroke(width = h * 0.02f)
            if (a.cover % 2 == 1) {
                drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, h * 0.7f), Offset(w, h * 0.45f), strokeWidth = h * 0.025f)
                drawLine(Color.White.copy(alpha = 0.18f), Offset(0f, h * 0.85f), Offset(w, h * 0.6f), strokeWidth = h * 0.025f)
            } else {
                drawCircle(Color.White.copy(alpha = 0.25f), radius = h * 0.28f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawLine(Color.White.copy(alpha = 0.22f), Offset(w * 0.5f, 0f), Offset(w * 0.5f, h), strokeWidth = h * 0.02f)
            }
            // Confeti
            val dots = listOf(0.15f to 0.2f, 0.3f to 0.75f, 0.7f to 0.8f, 0.9f to 0.55f, 0.55f to 0.12f)
            dots.forEachIndexed { i, (fx, fy) ->
                drawCircle(if (i % 2 == 0) Color(0xFFFFE27A) else Color.White, radius = h * 0.025f, center = Offset(w * fx, h * fy))
            }
        }
        Text(a.sport.emoji, fontSize = if (small) 26.sp else 84.sp)
    }
}

/** Mascota que flota suavemente arriba y abajo (da vida a las pantallas). */
@Composable
fun FloatingMascot(size: Dp, modifier: Modifier = Modifier, happy: Boolean = false, pose: Cuac? = null) {
    val t = rememberInfiniteTransition(label = "mascota")
    val dy by t.animateFloat(
        initialValue = 0f, targetValue = -8f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flotar",
    )
    Mascot(size, modifier.graphicsLayer { translationY = dy * density }, happy = happy, pose = pose)
}

/** Barra de progreso que se rellena con animación. */
@Composable
fun AnimatedBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val p by animateFloatAsState(if (started) progress.coerceIn(0f, 1f) else 0f, tween(900, easing = FastOutSlowInEasing), label = "barra")
    Box(modifier.height(10.dp).clip(RoundedCornerShape(5.dp)).background(LocalExtra.current.tile)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(RoundedCornerShape(5.dp)).background(color))
    }
}

/* ---------- Movimiento ---------- */

/**
 * Entrada escalonada: el elemento aparece subiendo un poco con un muelle, [indice] × 55 ms
 * después del anterior. Se anima en la capa gráfica (no recompone). Sin animaciones del
 * sistema, aparece directamente.
 */
@Composable
fun Modifier.entrada(indice: Int = 0): Modifier {
    val quieto = rememberReducedMotion()
    val progreso = remember { androidx.compose.animation.core.Animatable(if (quieto) 1f else 0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!quieto) {
            kotlinx.coroutines.delay(indice.coerceAtMost(8) * 55L)
            progreso.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.72f, stiffness = 260f))
        }
    }
    return graphicsLayer {
        val v = progreso.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * 36.dp.toPx()
        val e = 0.94f + 0.06f * v
        scaleX = e; scaleY = e
    }
}

/** Latido: al activarse (p. ej. «me gusta»), el elemento crece y vuelve con un rebote. */
@Composable
fun Modifier.latido(activo: Boolean): Modifier {
    val quieto = rememberReducedMotion()
    val escala = remember { androidx.compose.animation.core.Animatable(1f) }
    var primera by remember { androidx.compose.runtime.mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(activo) {
        if (primera) { primera = false; return@LaunchedEffect }
        if (quieto) return@LaunchedEffect
        escala.animateTo(if (activo) 1.45f else 0.8f, androidx.compose.animation.core.tween(110))
        escala.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.35f, stiffness = 420f))
    }
    return graphicsLayer { scaleX = escala.value; scaleY = escala.value }
}
