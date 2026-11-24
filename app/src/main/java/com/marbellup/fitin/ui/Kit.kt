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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.marbellup.fitin.ui.theme.LocalExtra

/*
 * Piezas del rediseño 2026 (lila, cristal, mascota). Las piezas antiguas siguen en Components.kt.
 */

/** Fondo en degradado lila suave, común a todas las pantallas nuevas. */
@Composable
fun GradientBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val x = LocalExtra.current
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(x.gradientTop, x.gradientBottom, MaterialTheme.colorScheme.background))),
        content = content,
    )
}

/** Degradado violeta de los botones principales y la mascota. */
@Composable
fun primaryBrush(): Brush = Brush.linearGradient(listOf(LocalExtra.current.hero, MaterialTheme.colorScheme.primary))

/**
 * Mascota de Fit-In: un robot amable dibujado en vectores (sin imágenes externas).
 * [waving] levanta un brazo; [happy] le cierra los ojos en sonrisa.
 */
@Composable
fun Mascot(size: Dp = 64.dp, modifier: Modifier = Modifier, happy: Boolean = false) {
    val violet = MaterialTheme.colorScheme.primary
    val light = LocalExtra.current.hero
    val navy = Color(0xFF231B4D)
    val eye = Color(0xFF7CE7FF)
    Canvas(modifier.size(size).semantics { contentDescription = "Mascota de Fit-In" }) {
        val w = this.size.width
        val h = this.size.height
        // Antena
        drawLine(violet, Offset(w * 0.5f, h * 0.18f), Offset(w * 0.5f, h * 0.06f), strokeWidth = w * 0.04f)
        drawCircle(light, radius = w * 0.06f, center = Offset(w * 0.5f, h * 0.06f))
        // Orejas
        drawRoundRect(violet, Offset(w * 0.06f, h * 0.36f), Size(w * 0.1f, h * 0.22f), CornerRadius(w * 0.05f))
        drawRoundRect(violet, Offset(w * 0.84f, h * 0.36f), Size(w * 0.1f, h * 0.22f), CornerRadius(w * 0.05f))
        // Cabeza
        drawRoundRect(Color.White, Offset(w * 0.14f, h * 0.18f), Size(w * 0.72f, h * 0.58f), CornerRadius(w * 0.22f))
        drawRoundRect(light, Offset(w * 0.14f, h * 0.18f), Size(w * 0.72f, h * 0.58f), CornerRadius(w * 0.22f),
            style = Stroke(width = w * 0.035f))
        // Visor
        drawRoundRect(navy, Offset(w * 0.24f, h * 0.30f), Size(w * 0.52f, h * 0.32f), CornerRadius(w * 0.14f))
        if (happy) {
            drawArc(eye, 200f, 140f, false, Offset(w * 0.32f, h * 0.38f), Size(w * 0.12f, h * 0.12f), style = Stroke(w * 0.035f))
            drawArc(eye, 200f, 140f, false, Offset(w * 0.56f, h * 0.38f), Size(w * 0.12f, h * 0.12f), style = Stroke(w * 0.035f))
        } else {
            drawOval(eye, Offset(w * 0.34f, h * 0.38f), Size(w * 0.09f, h * 0.13f))
            drawOval(eye, Offset(w * 0.57f, h * 0.38f), Size(w * 0.09f, h * 0.13f))
        }
        // Mofletes
        drawCircle(Color(0xFFFFB3C7), radius = w * 0.035f, center = Offset(w * 0.27f, h * 0.66f))
        drawCircle(Color(0xFFFFB3C7), radius = w * 0.035f, center = Offset(w * 0.73f, h * 0.66f))
        // Cuerpo
        drawRoundRect(violet, Offset(w * 0.3f, h * 0.78f), Size(w * 0.4f, h * 0.2f), CornerRadius(w * 0.1f))
        drawCircle(Color.White, radius = w * 0.04f, center = Offset(w * 0.5f, h * 0.88f))
    }
}

/** Botón principal en degradado violeta, en píldora y a todo lo ancho. */
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
    Surface(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = shape,
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth().heightIn(min = 58.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .background(if (enabled) primaryBrush() else Brush.linearGradient(listOf(Color(0xFFB9B2D6), Color(0xFFB9B2D6))), shape)
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

/** Tarjeta blanca tipo cristal, muy redondeada. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    color: Color = LocalExtra.current.card,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(26.dp)
    val m = modifier.fillMaxWidth()
    if (onClick != null) {
        Surface(onClick = onClick, modifier = m, shape = shape, color = color, shadowElevation = 3.dp) { Column(content = content) }
    } else {
        Surface(modifier = m, shape = shape, color = color, shadowElevation = 3.dp) { Column(content = content) }
    }
}

/** Botón redondo de icono (volver, campana, ajustes…) con objetivo táctil de 48 dp o más. */
@Composable
fun RoundIconButton(icon: ImageVector, description: String, badge: Int = 0, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = LocalExtra.current.card, shadowElevation = 2.dp, modifier = Modifier.size(50.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = if (badge > 0) "$description, $badge sin leer" else description,
                tint = MaterialTheme.colorScheme.onSurface)
            if (badge > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp).size(18.dp).clip(CircleShape)
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
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
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
        Box {
            Image(
                painter = painterResource(a.sport.photo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(150.dp).padding(8.dp).clip(RoundedCornerShape(20.dp)),
            )
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (rec != null) {
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(primaryBrush()).padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${rec.match}% para ti", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (a.status == "propuesta") {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Grupo nuevo",
                        color = x.onWarnContainer,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(x.warnContainer).padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                val fav = a.id in state.favorites
                Surface(
                    onClick = { state.toggleFavorite(a.id) },
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (fav) "Quitar de favoritos" else "Guardar en favoritos",
                            tint = if (fav) Color(0xFFE0447A) else Color(0xFF231B4D),
                        )
                    }
                }
            }
        }
        Row(Modifier.padding(start = 16.dp, end = 12.dp, bottom = 14.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(a.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text(a.shortDate, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("${a.place} · ${a.zone}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(x.mint),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF1B5E3F)) }
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

/** Barra inferior de la referencia: cuatro pestañas y el «+» central para crear actividad. */
@Composable
fun FitInBottomBar(state: AppState) {
    val x = LocalExtra.current
    Surface(
        color = x.card,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Row(
            Modifier.navigationBarsPadding().fillMaxWidth().height(76.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabItem(state, Tab.Inicio, Icons.Filled.Home, Icons.Outlined.Home)
            TabItem(state, Tab.Calendario, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Surface(
                    onClick = { state.open(Step.Create) },
                    shape = CircleShape,
                    color = Color.Transparent,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(58.dp),
                ) {
                    Box(Modifier.background(primaryBrush(), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Crear actividad", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }
            }
            TabItem(state, Tab.Chat, Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline)
            TabItem(state, Tab.Perfil, Icons.Filled.Person, Icons.Outlined.Person)
        }
    }
}

@Composable
private fun RowScope.TabItem(state: AppState, tab: Tab, selectedIcon: ImageVector, icon: ImageVector) {
    val selected = state.tab == tab
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .clickable { state.steps.clear(); state.tab = tab }
            .semantics { role = Role.Tab; this.selected = selected }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(if (selected) selectedIcon else icon, contentDescription = null, tint = color)
        Text(tab.label, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1,
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
        Image(
            painter = painterResource(a.sport.photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(a.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle ?: "${a.shortDate} · ${a.place}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

/** Fila de menú del perfil. */
@Composable
fun MenuRow(icon: ImageVector, title: String, detail: String? = null, badge: Int = 0, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(LocalExtra.current.chip), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (badge > 0) {
            Text(
                "$badge", color = Color.White, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(LocalExtra.current.notification).padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp))
    }
}

/** Etiqueta que recuerda que la IA funciona en el móvil y cuánto ha aprendido ya. */
@Composable
fun AiTag(state: AppState) {
    state.modelVersion
    Text(
        "IA en tu móvil · ${state.history.size} acciones aprendidas",
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
        else -> label
    }
