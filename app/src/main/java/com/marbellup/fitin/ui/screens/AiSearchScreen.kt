package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.data.Need
import com.marbellup.fitin.data.NeedGroup
import com.marbellup.fitin.data.Slot
import com.marbellup.fitin.data.Sport
import com.marbellup.fitin.ui.ActivityPhotoCard
import com.marbellup.fitin.ui.AiTag
import com.marbellup.fitin.ui.ExampleNote
import com.marbellup.fitin.ui.GlassCard
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.GradientButton
import com.marbellup.fitin.ui.Mascot
import com.marbellup.fitin.ui.NeedPill
import com.marbellup.fitin.ui.RoundIconButton
import com.marbellup.fitin.ui.SelectTile
import com.marbellup.fitin.ui.icon
import com.marbellup.fitin.ui.emoji
import com.marbellup.fitin.ui.say
import com.marbellup.fitin.ui.shortLabel
import com.marbellup.fitin.ui.theme.LocalExtra
import com.marbellup.fitin.ui.tint
import com.marbellup.fitin.ui.funColor
import com.marbellup.fitin.ui.pastel

private val zones = listOf("Zona centro", "Zona oeste", "Zona este", "Zona norte")
private val levels = listOf(
    Triple("iniciacion", "Iniciación", "🌱"),
    Triple("intermedio", "Intermedio", "🔥"),
    Triple("avanzado", "Avanzado", "🏆"),
    Triple(null, "Da igual", "🤷"),
)

/**
 * «Tus características»: TODO se elige con opciones (nada de texto), para que el modelo reciba
 * siempre datos limpios. Con esto ordena actividades e instalaciones.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSearchScreen(state: AppState) {
    val x = LocalExtra.current
    val p = state.profile
    var showResults by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()
    LaunchedEffect(showResults) { if (showResults) scroll.animateScrollTo(scroll.maxValue) }

    GradientBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(scroll).padding(bottom = 32.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Volver") { if (!state.back()) state.finishOnboarding() }
                Text("Mis preferencias", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 12.dp))
                AiTag(state)
            }

            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                com.marbellup.fitin.ui.FloatingMascot(64.dp)
                Text(
                    say("¡Hola, ${state.userName}! Marca lo que necesitas y lo que te gusta. Con eso te recomiendo actividades y sitios.",
                        "Marca lo que necesitas. Yo te digo qué hacer y dónde."),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 10.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 22.dp))
                        .background(x.card).padding(12.dp),
                )
            }

            Title("¿Qué necesitas?", "Solo lo que necesitas, nunca qué tienes.")
            for (g in NeedGroup.entries) {
                Text(g.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 6.dp))
                TileGrid(Need.entries.filter { it.group == g }) { n, m ->
                    SelectTile(n.shortLabel, n.icon, n in p.needs, m, tint = funColor(n.ordinal), emoji = n.emoji) { state.toggle(n) }
                }
            }

            Title("¿Qué deportes te gustan?")
            TileGrid(Sport.entries.toList()) { s, m ->
                SelectTile(s.label, s.icon, s in p.sports, m, tint = s.tint, bg = s.pastel, emoji = s.emoji) { state.toggleSport(s) }
            }

            Title("¿Cuándo te viene bien?")
            TileGrid(Slot.entries.toList(), perRow = 4) { s, m ->
                SelectTile(s.label, s.icon, s in p.slots, m, tint = funColor(s.ordinal + 2), emoji = s.emoji) { state.toggleSlot(s) }
            }

            Title("¿Qué nivel tienes?")
            TileGrid(levels, perRow = 4) { (code, label, e), m ->
                SelectTile(label, Icons.Filled.Star, p.level == code, m, tint = funColor(levels.indexOfFirst { it.second == label } + 3), emoji = e) { state.setLevel(code) }
            }

            Title("¿Por dónde te mueves?")
            TileGrid(zones, perRow = 4) { z, m ->
                SelectTile(z.removePrefix("Zona ").replaceFirstChar { it.uppercase() }, Icons.Filled.Place, p.zone == z, m, tint = funColor(zones.indexOf(z) + 1), emoji = "📍") {
                    state.setZone(if (p.zone == z) null else z)
                }
            }

            GradientButton("Ver mis recomendaciones", Modifier.padding(16.dp).padding(top = 8.dp), icon = Icons.Filled.AutoAwesome) {
                showResults = true
            }

            if (showResults) {
                val ranked = state.ranked()
                Title("Actividades para ti", "Ordenadas por la IA según lo que has marcado y lo que vas eligiendo.")
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (r in ranked.take(5)) ActivityPhotoCard(r.activity, state, r)
                }

                val sport = p.sports.firstOrNull() ?: ranked.firstOrNull()?.activity?.sport
                Title(if (sport != null) "Sitios para ${sport.label.lowercase()}" else "Sitios para ti",
                    "Lo que cubre cada instalación de lo que necesitas.")
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (f in state.facilitiesFor(sport).take(3)) {
                        GlassCard {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(52.dp).clip(CircleShape).background(x.lilac), contentAlignment = Alignment.Center) {
                                    Text("${f.match}%", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                    Text(f.facility.name, style = MaterialTheme.typography.titleMedium)
                                    Text(f.facility.zone + " · " + f.facility.seal.label, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        f.covered.forEach { NeedPill(it.shortLabel, true) }
                                        f.missing.forEach { NeedPill(it.shortLabel, false) }
                                    }
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = { state.steps.clear(); state.finishOnboarding() },
                    modifier = Modifier.padding(16.dp).fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Ir al inicio", style = MaterialTheme.typography.titleMedium)
                }
                ExampleNote("Recomendaciones sobre datos de ejemplo")
            }
        }
    }
}

@Composable
private fun Title(title: String, subtitle: String? = null) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Rejilla de mosaicos, 3 por fila como en la referencia (rellena la última fila para que no se estiren). */
@Composable
private fun <T> TileGrid(items: List<T>, perRow: Int = 3, tile: @Composable (T, Modifier) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in items.chunked(perRow)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (item in row) tile(item, Modifier.weight(1f))
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
