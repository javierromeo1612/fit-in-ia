package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.Step
import com.marbellup.fitin.data.Sport
import com.marbellup.fitin.ui.ActivityPhotoCard
import com.marbellup.fitin.ui.AiTag
import com.marbellup.fitin.ui.ExampleNote
import com.marbellup.fitin.ui.GlassCard
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.Mascot
import com.marbellup.fitin.ui.NeedPill
import com.marbellup.fitin.ui.RoundIconButton
import com.marbellup.fitin.ui.SelectTile
import com.marbellup.fitin.ui.icon
import com.marbellup.fitin.ui.emoji
import com.marbellup.fitin.ui.shortLabel
import com.marbellup.fitin.ui.theme.LocalExtra
import com.marbellup.fitin.ui.tint

/**
 * «Para ti»: el corazón de la IA. Actividades e instalaciones ordenadas por el modelo según tus
 * preferencias y lo que has ido eligiendo, cada una con su porqué.
 */
@Composable
fun ForYouScreen(state: AppState, bottomBar: @Composable () -> Unit) {
    val x = LocalExtra.current
    val ranked = state.ranked()
    var sport by remember { mutableStateOf<Sport?>(null) }
    val shown = ranked.filter { sport == null || it.activity.sport == sport }
    val placesSport = sport ?: state.profile.sports.firstOrNull() ?: ranked.firstOrNull()?.activity?.sport

    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = bottomBar) { padding ->
        GradientBackground(Modifier.padding(padding)) {
            Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
                Row(Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Para ti", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).semantics { heading() })
                    RoundIconButton(Icons.Filled.Tune, "Mis preferencias") { state.open(Step.AiSearch) }
                }

                GlassCard(Modifier.padding(16.dp), color = x.lilac) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        com.marbellup.fitin.ui.FloatingMascot(60.dp)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("Elegido por la IA para ti", style = MaterialTheme.typography.titleMedium)
                            Text("Según lo que necesitas, tus gustos y lo que vas eligiendo.", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(6.dp))
                            AiTag(state)
                        }
                    }
                }

                // Filtro por deporte con mosaicos de colores (desliza para ver todos)
                androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { SelectTile("Todo", Icons.Filled.Apps, sport == null, Modifier.width(84.dp), emoji = "✨") { sport = null } }
                    items(Sport.entries.size) { i ->
                        val s = Sport.entries[i]
                        SelectTile(s.label, s.icon, sport == s, Modifier.width(84.dp), tint = s.tint, emoji = s.emoji) {
                            sport = if (sport == s) null else s
                        }
                    }
                }

                SectionTitle("Actividades que encajan contigo", null) {}
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (shown.isEmpty()) Text("No hay actividades con plazas de este deporte.", style = MaterialTheme.typography.bodyLarge)
                    for (r in shown.take(6)) ActivityPhotoCard(r.activity, state, r)
                }

                SectionTitle(if (placesSport != null) "Sitios para ${placesSport.label.lowercase()}" else "Sitios para ti", null) {}
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (f in state.facilitiesFor(placesSport).take(3)) {
                        GlassCard {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(54.dp).clip(CircleShape).background(x.lilac), contentAlignment = Alignment.Center) {
                                    Text("${f.match}%", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(f.facility.name, style = MaterialTheme.typography.titleMedium)
                                    Text(f.facility.zone + " · " + f.facility.seal.label, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        f.covered.take(2).forEach { NeedPill(it.shortLabel, true) }
                                        f.missing.take(1).forEach { NeedPill(it.shortLabel, false) }
                                    }
                                }
                            }
                        }
                    }
                }
                ExampleNote("Recomendaciones sobre datos de ejemplo")
            }
        }
    }
}
