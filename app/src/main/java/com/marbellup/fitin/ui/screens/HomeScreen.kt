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
import androidx.compose.ui.graphics.Color
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

/**
 * Inicio, calcado de la referencia: saludo y campana, buscador, cuatro accesos rápidos,
 * «Actividades cerca de ti» (ordenadas por la IA) y la tarjeta de ayuda con la mascota.
 */
@Composable
fun HomeScreen(state: AppState, bottomBar: @Composable () -> Unit) {
    val x = LocalExtra.current
    val ranked = state.ranked()
    val list = ranked.filter { state.sport == null || it.activity.sport == state.sport }

    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = bottomBar) { padding ->
        GradientBackground(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
                // Hola, Lucía 👋 + campana
                Row(Modifier.padding(start = 20.dp, end = 16.dp, top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hola, ${state.userName} 👋", style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics { heading() })
                        Text("¿Qué te apetece hacer hoy?", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RoundIconButton(Icons.Outlined.Notifications, "Avisos", badge = state.unread) { state.open(Step.Notifications) }
                }

                // Buscador en píldora
                Surface(
                    onClick = { state.open(Step.Search) },
                    shape = RoundedCornerShape(50),
                    color = x.card,
                    shadowElevation = 2.dp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp).fillMaxWidth().heightIn(min = 54.dp),
                ) {
                    Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(10.dp))
                        Text("Buscar actividades…", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Accesos rápidos en mosaicos blancos con iconos de color, como en la referencia (desliza para ver todos)
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        QuickTile("Cerca", "📍", state.sport == null, Modifier.width(78.dp)) { state.sport = null }
                    }
                    items(Sport.entries.toList()) { s ->
                        QuickTile(if (s == Sport.Natacion) "Piscina" else s.label, s.emoji, state.sport == s, Modifier.width(78.dp)) {
                            state.sport = if (state.sport == s) null else s
                        }
                    }
                    item {
                        QuickTile("Más", "⋯", false, Modifier.width(78.dp)) { state.open(Step.AiSearch) }
                    }
                }

                // ⭐ Actividades cerca de ti · Ver todas →
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.StarOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.sport == null) "Actividades cerca de ti" else state.sport!!.label,
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
                    TextButton(onClick = { state.tab = Tab.ParaTi }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Ver todas", style = MaterialTheme.typography.labelLarge)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                            modifier = Modifier.size(16.dp).padding(start = 2.dp))
                    }
                }
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (list.isEmpty()) {
                        Text("No hay actividades con plazas de este deporte ahora mismo.", style = MaterialTheme.typography.bodyLarge)
                    }
                    for (r in list.take(6)) ActivityPhotoCard(r.activity, state, r, showReason = false)
                }

                // ¿Necesitas ayuda? → recomendaciones de la IA
                GlassCard(Modifier.padding(16.dp), color = x.lilac, onClick = { state.tab = Tab.ParaTi }) {
                    Row(Modifier.padding(start = 18.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
                        .semantics(mergeDescendants = true) { role = Role.Button },
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("¿Necesitas ayuda?", style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pregúntale a Fit-In", style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp).padding(start = 4.dp))
                            }
                        }
                        FloatingMascot(70.dp)
                    }
                }
                ExampleNote("Actividades de ejemplo para la demo · Fotos: Unsplash")
            }
        }
    }
}

/** Acceso rápido: mosaico blanco con icono de color y texto debajo, como en la referencia. */
@Composable
private fun QuickTile(label: String, emoji: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else if (selected) 1.06f else 1f, label = "acceso")
    Surface(
        onClick = onClick,
        interactionSource = source,
        shape = RoundedCornerShape(22.dp),
        color = if (selected) LocalExtra.current.lilac else LocalExtra.current.card,
        shadowElevation = if (selected) 6.dp else 2.dp,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }.heightIn(min = 84.dp)
            .semantics { role = Role.Tab; this.selected = selected },
    ) {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text(emoji, fontSize = 30.sp)
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Cabecera de sección reutilizada por otras pantallas. */
@Composable
internal fun SectionTitle(title: String, action: String?, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.StarOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
        if (action != null) {
            TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(action, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp).padding(start = 2.dp))
            }
        }
    }
}
