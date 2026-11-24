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
import com.marbellup.fitin.ui.say
import com.marbellup.fitin.ui.shortLabel
import com.marbellup.fitin.ui.theme.LocalExtra

private val zones = listOf("Zona centro", "Zona oeste", "Zona este", "Zona norte")
private val levels = listOf("iniciacion" to "Iniciación", "intermedio" to "Intermedio", "avanzado" to "Avanzado", null to "Da igual")

/**
 * «Tus características»: la persona marca lo que necesita, sus deportes, horario, nivel y zona
 * (o lo escribe con sus palabras). El modelo del móvil ordena actividades e instalaciones con eso.
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
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(scroll).padding(bottom = 32.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Volver") { if (!state.back()) state.finishOnboarding() }
                Spacer(Modifier.weight(1f))
                AiTag(state)
            }

            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.Top) {
                Mascot(64.dp)
                Column(
                    Modifier.padding(start = 10.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 22.dp))
                        .background(x.card).padding(14.dp),
                ) {
                    Text("¿Qué necesitas para hacer deporte a gusto?", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() })
                    Text(
                        say("Márcalo y te recomiendo actividad e instalación. Nunca te pregunto qué tienes, solo qué necesitas. Todo se queda en tu móvil.",
                            "Marca lo que necesitas. Te digo qué actividad y qué sitio te van bien."),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Lenguaje natural (procesado en el móvil)
            GlassCard(Modifier.padding(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = state.prompt, onValueChange = { state.prompt = it },
                        modifier = Modifier.fillMaxWidth(), minLines = 2, shape = RoundedCornerShape(18.dp),
                        label = { Text("O dímelo con tus palabras") },
                        placeholder = { Text("Pádel nivel intermedio por la tarde, pista cubierta y accesible") },
                    )
                    Spacer(Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = { state.applyText() }, enabled = state.prompt.isNotBlank(),
                        shape = RoundedCornerShape(50), modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Rellenar mis características")
                    }
                }
            }

            Section("Lo que necesitas")
            for (g in NeedGroup.entries) {
                Text(g.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 20.dp, top = 6.dp, bottom = 4.dp))
                FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (n in Need.entries.filter { it.group == g }) {
                        Choice(n.label, n in p.needs) { state.toggle(n) }
                    }
                }
            }

            Section("Deportes que te gustan")
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (s in Sport.entries) Choice(s.label, s in p.sports) { state.toggleSport(s) }
            }

            Section("Cuándo te viene bien")
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (s in Slot.entries) Choice(s.label, s in p.slots) { state.toggleSlot(s) }
            }

            Section("Tu nivel")
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((code, label) in levels) Choice(label, p.level == code) { state.setLevel(code) }
            }

            Section("Dónde vives o te mueves")
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (z in zones) Choice(z.removePrefix("Zona ").replaceFirstChar { it.uppercase() }, p.zone == z) {
                    state.setZone(if (p.zone == z) null else z)
                }
            }

            GradientButton("Ver mis recomendaciones", Modifier.padding(16.dp).padding(top = 8.dp), icon = Icons.Filled.AutoAwesome) {
                showResults = true
            }

            if (showResults) {
                val ranked = state.ranked()
                Section("Actividades para ti")
                Text(
                    "Ordenadas por el modelo según tus características y lo que has ido eligiendo.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (r in ranked.take(5)) ActivityPhotoCard(r.activity, state, r)
                }

                val sport = p.sports.firstOrNull() ?: ranked.firstOrNull()?.activity?.sport
                Section(if (sport != null) "Instalaciones para ${sport.label.lowercase()}" else "Instalaciones para ti")
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
                                    FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp).semantics { heading() })
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick, label = { Text(label) },
        leadingIcon = if (selected) ({ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }) else null,
        shape = RoundedCornerShape(50), modifier = Modifier.heightIn(min = 48.dp),
    )
}
