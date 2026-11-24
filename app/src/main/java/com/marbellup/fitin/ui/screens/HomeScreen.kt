package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.Step
import com.marbellup.fitin.Tab
import com.marbellup.fitin.data.Sport
import com.marbellup.fitin.ui.ActivityPhotoCard
import com.marbellup.fitin.ui.ExampleNote
import com.marbellup.fitin.ui.GlassCard
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.FloatingMascot
import com.marbellup.fitin.ui.emoji
import com.marbellup.fitin.ui.RoundIconButton
import com.marbellup.fitin.ui.icon
import com.marbellup.fitin.ui.theme.LocalExtra
import com.marbellup.fitin.ui.tint
import com.marbellup.fitin.ui.funColor
import com.marbellup.fitin.ui.heroBrush
import com.marbellup.fitin.ui.pastel
import com.marbellup.fitin.ui.primaryBrush

/**
 * Inicio, calcado de la referencia: saludo y campana, buscador, cuatro accesos rápidos,
 * «Actividades cerca de ti» (ordenadas por la IA) y la tarjeta de ayuda con la mascota.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: AppState, bottomBar: @Composable () -> Unit) {
    val x = LocalExtra.current
    val ranked = state.ranked()
    val list = ranked.filter { state.sport == null || it.activity.sport == state.sport }
    var showSports by remember { mutableStateOf(false) }

    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = bottomBar) { padding ->
        GradientBackground(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
                // Hola, Lucía + campana
                Row(Modifier.padding(start = 20.dp, end = 16.dp, top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hola, ${state.userName}", style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics { heading() })
                        Text("¿Qué te apetece hacer hoy?", style = MaterialTheme.typography.bodyLarge)
                    }
                    RoundIconButton(Icons.Outlined.Notifications, "Avisos", badge = state.unread) { state.open(Step.Notifications) }
                }

                // Buscador en píldora
                Surface(
                    onClick = { state.open(Step.Search) },
                    shape = RoundedCornerShape(50),
                    color = x.card,
                    shadowElevation = 2.dp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth().heightIn(min = 46.dp),
                ) {
                    Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(10.dp))
                        Text("Buscar actividades…", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Cuatro accesos rápidos en una fila, como en la referencia («Más» abre todos los deportes)
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickTile("Cerca", "📍", funColor(0), state.sport == null, Modifier.weight(1f)) { state.sport = null }
                    QuickTile("Piscina", Sport.Natacion.emoji, Sport.Natacion.tint, state.sport == Sport.Natacion, Modifier.weight(1f)) {
                        state.sport = if (state.sport == Sport.Natacion) null else Sport.Natacion
                    }
                    QuickTile("Baloncesto", Sport.Baloncesto.emoji, Sport.Baloncesto.tint, state.sport == Sport.Baloncesto, Modifier.weight(1f)) {
                        state.sport = if (state.sport == Sport.Baloncesto) null else Sport.Baloncesto
                    }
                    val other = state.sport?.takeIf { it != Sport.Natacion && it != Sport.Baloncesto }
                    QuickTile(other?.label ?: "Más", other?.emoji ?: "➕", other?.tint ?: funColor(4), other != null, Modifier.weight(1f)) { showSports = true }
                }

                // ⭐ Actividades cerca de ti · Ver todas →
                SectionTitle(if (state.sport == null) "Actividades cerca de ti" else state.sport!!.label, "Ver todas") {
                    state.open(Step.Search)
                }
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (list.isEmpty()) {
                        Text("No hay actividades con plazas de este deporte ahora mismo.", style = MaterialTheme.typography.bodyLarge)
                    }
                    for (r in list.take(6)) ActivityPhotoCard(r.activity, state, r, showReason = false)
                }

                // ¿Necesitas ayuda? → recomendaciones de la IA (tarjeta en degradado violeta → azul)
                GlassCard(Modifier.padding(16.dp), color = x.hero, onClick = { state.tab = Tab.ParaTi }) {
                    Row(Modifier.fillMaxWidth().background(heroBrush()).padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 14.dp)
                        .semantics(mergeDescendants = true) { role = Role.Button },
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("¿Necesitas ayuda?", style = MaterialTheme.typography.titleLarge, color = Color.White)
                            Row(Modifier.padding(top = 6.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.22f))
                                .padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Pregúntale a Cuac", style = MaterialTheme.typography.labelLarge, color = Color.White)
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                                    tint = Color.White, modifier = Modifier.size(18.dp).padding(start = 4.dp))
                            }
                        }
                        FloatingMascot(74.dp)
                    }
                }
                ExampleNote("Actividades de ejemplo para la demo · Fotos: Unsplash")
            }
        }
    }

    // «Más»: todos los deportes en mosaicos
    if (showSports) {
        ModalBottomSheet(onDismissRequest = { showSports = false }, containerColor = LocalExtra.current.gradientBottom) {
            Text("Elige un deporte", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 20.dp, bottom = 12.dp))
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in Sport.entries.chunked(4)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (sp in row) {
                            QuickTile(sp.label, sp.emoji, sp.tint, state.sport == sp, Modifier.weight(1f)) { state.sport = sp; showSports = false }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/**
 * Acceso rápido: mosaico de color pastel con el emoji en un círculo blanco y el texto debajo.
 * Seleccionado: se rellena con el color vivo del deporte y el texto pasa a blanco.
 */
@Composable
private fun QuickTile(label: String, emoji: String, color: Color, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else if (selected) 1.06f else 1f, label = "acceso")
    val shape = RoundedCornerShape(20.dp)
    Surface(
        onClick = onClick,
        interactionSource = source,
        shape = shape,
        color = Color.Transparent,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }.heightIn(min = 84.dp)
            .shadow(if (selected) 10.dp else 3.dp, shape, ambientColor = color, spotColor = color)
            .semantics { role = Role.Tab; this.selected = selected },
    ) {
        Column(
            Modifier
                .background(
                    if (selected) Brush.linearGradient(listOf(color.pastel(0.75f), color))
                    else Brush.linearGradient(listOf(Color.White, color.pastel(0.3f))),
                    shape,
                )
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = if (selected) 0.9f else 1f)),
                contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 22.sp)
            }
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 5.dp))
        }
    }
}

/** Cabecera de sección: estrella en círculo de degradado, título y acción en píldora. */
@Composable
internal fun SectionTitle(title: String, action: String?, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 12.dp, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(primaryBrush()), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFE27A), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
        if (action != null) {
            Surface(onClick = onAction, shape = RoundedCornerShape(50), color = LocalExtra.current.card,
                shadowElevation = 2.dp, modifier = Modifier.heightIn(min = 40.dp)) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp).padding(start = 2.dp))
                }
            }
        }
    }
}
