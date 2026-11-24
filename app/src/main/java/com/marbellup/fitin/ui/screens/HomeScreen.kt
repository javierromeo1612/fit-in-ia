package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.Step
import com.marbellup.fitin.Tab
import com.marbellup.fitin.data.SampleData
import com.marbellup.fitin.data.Sport
import com.marbellup.fitin.data.matchActivities
import com.marbellup.fitin.ui.ActivityPhotoCard
import com.marbellup.fitin.ui.CategoryTile
import com.marbellup.fitin.ui.ExampleNote
import com.marbellup.fitin.ui.GlassCard
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.Mascot
import com.marbellup.fitin.ui.RoundIconButton
import com.marbellup.fitin.ui.icon
import com.marbellup.fitin.ui.theme.LocalExtra
import com.marbellup.fitin.ui.tint

/** Pantalla 2 de la referencia: saludo, buscador con IA, categorías y actividades cerca. */
@Composable
fun HomeScreen(state: AppState, bottomBar: @Composable () -> Unit) {
    val x = LocalExtra.current
    val recs = state.result?.recommendations.orEmpty()
    val nearby = matchActivities(state.needs)
        .filter { state.sport == null || it.activity.sport == state.sport }
        .filter { it.activity.status != "propuesta" || it.activity.id in state.joined }

    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = bottomBar) { padding ->
        GradientBackground(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
                // Saludo + campana
                Row(Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hola, ${state.userName} 👋", style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics { heading() })
                        Text("¿Qué te apetece hacer hoy?", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RoundIconButton(Icons.Outlined.Notifications, "Notificaciones", badge = state.unread) {
                        state.refreshNotifications()
                        state.open(Step.Notifications)
                    }
                }

                // Buscador: abre la búsqueda en lenguaje natural
                Surface(
                    onClick = { state.open(Step.AiSearch) },
                    shape = RoundedCornerShape(50),
                    color = x.card,
                    shadowElevation = 2.dp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp).fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(10.dp))
                        Text("Dime qué buscas y qué necesitas…", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Búsqueda con IA", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Categorías
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    item {
                        CategoryTile("Cerca", Icons.Filled.NearMe, MaterialTheme.colorScheme.primary, x.lilac, state.sport == null) {
                            state.sport = null
                        }
                    }
                    val pastel = listOf(x.sky, x.peach, x.mint, x.pink, x.lilac)
                    items(Sport.entries.toList()) { s ->
                        CategoryTile(s.label, s.icon, s.tint, pastel[s.ordinal % pastel.size], state.sport == s) {
                            state.sport = if (state.sport == s) null else s
                        }
                    }
                }

                // Recomendado por la IA
                if (recs.isNotEmpty()) {
                    SectionTitle("Elegido para ti", "Ver todo") { state.open(Step.AiSearch) }
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recs.take(4)) { rec ->
                            ActivityPhotoCard(rec.activity, state, rec, Modifier.width(300.dp), showReason = true)
                        }
                    }
                }

                SectionTitle(if (state.sport == null) "Actividades cerca de ti" else state.sport!!.label, null) {}
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (nearby.isEmpty()) {
                        Text("No hay actividades de este deporte. Pídeselo a la IA y crea un grupo.",
                            style = MaterialTheme.typography.bodyLarge)
                    }
                    for (m in nearby.take(6)) {
                        ActivityPhotoCard(m.activity, state, state.reasons[m.activity.id], showReason = false)
                    }
                }

                // ¿Necesitas ayuda? Pregúntale a Fit-In
                GlassCard(Modifier.padding(16.dp), color = x.lilac, onClick = { state.tab = Tab.Chat }) {
                    Row(Modifier.padding(16.dp).semantics(mergeDescendants = true) { role = Role.Button },
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("¿Necesitas ayuda?", style = MaterialTheme.typography.titleMedium)
                            Text("Pregúntale a Fit-In", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Mascot(60.dp)
                    }
                }
                ExampleNote("Actividades de ejemplo para la demo · Fotos: Unsplash")
            }
        }
    }
}

@Composable
internal fun SectionTitle(title: String, action: String?, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = LocalExtra.current.flame, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
        if (action != null) {
            TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(action, style = MaterialTheme.typography.titleSmall)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp).padding(start = 4.dp))
            }
        }
    }
}
