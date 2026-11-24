package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marbellup.fitin.ui.emoji
import com.marbellup.fitin.AppState
import com.marbellup.fitin.Step
import com.marbellup.fitin.data.Need
import com.marbellup.fitin.data.Sport
import com.marbellup.fitin.ui.AiReason
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.GradientButton
import com.marbellup.fitin.ui.Mascot
import com.marbellup.fitin.ui.RoundIconButton
import com.marbellup.fitin.ui.icon
import com.marbellup.fitin.ui.theme.LocalExtra
import com.marbellup.fitin.ui.tint
import com.marbellup.fitin.ui.deeper
import com.marbellup.fitin.ui.funColor
import com.marbellup.fitin.ui.pastel

private data class NeedOption(val label: String, val icon: ImageVector, val needs: Set<Need>)

private val needOptions = listOf(
    NeedOption("Silla de ruedas", Icons.AutoMirrored.Filled.Accessible, setOf(Need.SinEscalones, Need.Vestuario)),
    NeedOption("Material adaptado", Icons.Filled.Settings, setOf(Need.Material)),
    NeedOption("Acompañamiento", Icons.Filled.Groups, setOf(Need.Acompanante)),
    NeedOption("Pista cubierta", Icons.Filled.Roofing, setOf(Need.Cubierta)),
    NeedOption("Lengua de signos", Icons.Filled.SignLanguage, setOf(Need.Signos)),
    NeedOption("Monitor de apoyo", Icons.Filled.SupportAgent, setOf(Need.Monitor)),
)

private val whenOptions = listOf("Hoy por la tarde", "Mañana por la tarde", "Mañana por la mañana", "El fin de semana")
private val levelOptions = listOf("iniciacion" to "Iniciación", "intermedio" to "Intermedio", null to "Da igual")

/**
 * Pantalla 6 de la referencia: «¿Qué actividad quieres organizar?». La IA elige instalación libre
 * que cubra lo que necesitas y avisa a personas compatibles para completar el grupo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateScreen(state: AppState) {
    val x = LocalExtra.current
    var sport by remember { mutableStateOf<Sport?>(null) }
    var chosen by remember { mutableStateOf(needOptions.filter { o -> o.needs.all { it in state.needs } }.map { it.label }.toSet()) }
    var whenText by remember { mutableStateOf(whenOptions[0]) }
    var level by remember { mutableStateOf<String?>(null) }
    // Paso 2: instalaciones ordenadas por el modelo para lo que has marcado.
    var showPlaces by remember { mutableStateOf(false) }

    GradientBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Volver") { if (showPlaces) showPlaces = false else state.back() }
                Text("Crear actividad", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(end = 50.dp))
            }

            if (!showPlaces) {
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    com.marbellup.fitin.ui.FloatingMascot(60.dp)
                    Text("¡Hola, ${state.userName}! Te busco el sitio que mejor encaja.", style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 10.dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 22.dp))
                            .background(x.card).padding(12.dp))
                }
                Text("¿Qué actividad quieres organizar?", style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp).semantics { heading() })

                FlowRow(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 3,
                ) {
                    Sport.entries.forEach { s ->
                        val sel = sport == s
                        Surface(
                            onClick = { sport = s },
                            shape = RoundedCornerShape(22.dp),
                            color = if (sel) s.tint.pastel(0.4f) else s.pastel,
                            shadowElevation = if (sel) 6.dp else 1.dp,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 96.dp)
                                .then(if (sel) Modifier.border(3.dp, s.tint, RoundedCornerShape(22.dp)) else Modifier)
                                .semantics { role = Role.RadioButton; selected = sel },
                        ) {
                            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                Box(Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                                    Text(s.emoji, fontSize = 28.sp)
                                }
                                Text(s.label, style = MaterialTheme.typography.labelLarge, color = s.tint.deeper(0.45f),
                                    modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                    Surface(
                        onClick = { state.open(Step.AiSearch) },
                        shape = RoundedCornerShape(22.dp),
                        color = x.card,
                        modifier = Modifier.weight(1f).heightIn(min = 96.dp),
                    ) {
                        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp))
                            Text("Otra", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }

                Text("¿Qué necesitas?", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 8.dp).semantics { heading() })
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (row in needOptions.chunked(3)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (o in row) {
                                val sel = o.label in chosen
                                com.marbellup.fitin.ui.SelectTile(o.label, o.icon, sel, Modifier.weight(1f), tint = funColor(needOptions.indexOf(o)), emoji = o.needs.first().emoji) {
                                    chosen = if (sel) chosen - o.label else chosen + o.label
                                }
                            }
                        }
                    }
                }

                GradientButton(
                    "Siguiente",
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                    enabled = sport != null,
                ) { showPlaces = true }
                if (sport == null) {
                    Text("Elige un deporte para seguir.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                }
            } else {
                // Instalaciones recomendadas por el modelo para este deporte y estas necesidades
                val s = sport ?: Sport.Padel
                val extra = needOptions.filter { it.label in chosen }.flatMap { it.needs }.toSet()
                val places = state.facilitiesFor(s, extra)
                val (offset, time) = when (whenText) {
                    "Hoy por la tarde" -> 0 to "18:00"
                    "Mañana por la tarde" -> 1 to "18:00"
                    "Mañana por la mañana" -> 1 to "10:00"
                    else -> com.marbellup.fitin.data.Dates.sat to "10:00"
                }
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Mascot(64.dp, happy = true)
                    Text("Dónde organizarlo", style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(start = 12.dp).semantics { heading() })
                }
                Text(
                    "La IA ordena las instalaciones de ${s.label.lowercase()} según lo que necesitas, lo cerca que te pillan y si están verificadas.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
                Text("¿Cuándo?", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp).semantics { heading() })
                FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (w in whenOptions) {
                        FilterChip(selected = whenText == w, onClick = { whenText = w }, label = { Text(w) },
                            shape = RoundedCornerShape(50), modifier = Modifier.heightIn(min = 48.dp))
                    }
                }

                Text("Nivel", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp).semantics { heading() })
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((code, label) in levelOptions) {
                        FilterChip(selected = level == code, onClick = { level = code }, label = { Text(label) },
                            shape = RoundedCornerShape(50), modifier = Modifier.heightIn(min = 48.dp))
                    }
                }

                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (places.isEmpty()) {
                        AiReason("No hay instalaciones de ${s.label.lowercase()} en los datos de ejemplo.")
                    }
                    places.forEachIndexed { i, f ->
                        com.marbellup.fitin.ui.GlassCard {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${f.match}%", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                        Text(f.facility.name, style = MaterialTheme.typography.titleMedium)
                                        Text("${f.facility.zone} · ${f.facility.seal.label}", style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (i == 0) com.marbellup.fitin.ui.NeedPill("Mejor opción", true)
                                }
                                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    f.covered.forEach { com.marbellup.fitin.ui.NeedPill(it.label, true) }
                                    f.missing.forEach { com.marbellup.fitin.ui.NeedPill(it.label, false) }
                                }
                                Spacer(Modifier.height(10.dp))
                                GradientButton("Crear aquí · $whenText") {
                                    val a = state.createGroup(s, f, offset, time, level)
                                    state.back()
                                    state.open(Step.Detail(a.id))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
