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
import com.marbellup.fitin.AppState
import com.marbellup.fitin.Step
import com.marbellup.fitin.data.Need
import com.marbellup.fitin.data.Sport
import com.marbellup.fitin.ui.ActivityPhotoCard
import com.marbellup.fitin.ui.AiReason
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.GradientButton
import com.marbellup.fitin.ui.GroupingCard
import com.marbellup.fitin.ui.Mascot
import com.marbellup.fitin.ui.RoundIconButton
import com.marbellup.fitin.ui.icon
import com.marbellup.fitin.ui.theme.LocalExtra
import com.marbellup.fitin.ui.tint

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
    val done = state.createResult
    val pastel = listOf(x.peach, x.sky, x.pink, x.mint, x.lilac, x.peach, x.sky)

    GradientBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Volver") { state.createResult = null; state.back() }
                Text("Crear actividad", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(end = 50.dp))
            }

            if (done == null) {
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Mascot(56.dp)
                    Text("¡Hola, ${state.userName}! Te busco sitio y gente.", style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 10.dp).clip(RoundedCornerShape(18.dp)).background(x.card).padding(12.dp))
                }
                Text("¿Qué actividad quieres organizar?", style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp).semantics { heading() })

                FlowRow(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 3,
                ) {
                    Sport.entries.forEachIndexed { i, s ->
                        val sel = sport == s
                        Surface(
                            onClick = { sport = s },
                            shape = RoundedCornerShape(22.dp),
                            color = pastel[i % pastel.size],
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 96.dp)
                                .then(if (sel) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(22.dp)) else Modifier)
                                .semantics { role = Role.RadioButton; selected = sel },
                        ) {
                            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                Icon(s.icon, contentDescription = null, tint = s.tint, modifier = Modifier.size(34.dp))
                                Text(s.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
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
                FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (o in needOptions) {
                        val sel = o.label in chosen
                        FilterChip(
                            selected = sel,
                            onClick = { chosen = if (sel) chosen - o.label else chosen + o.label },
                            label = { Text(o.label) },
                            leadingIcon = { Icon(o.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }

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

                GradientButton(
                    if (state.creating) "Buscando sitio y gente…" else "Siguiente",
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                    loading = state.creating,
                    enabled = sport != null,
                ) {
                    val s = sport ?: return@GradientButton
                    val needs = needOptions.filter { it.label in chosen }.flatMap { it.needs }.toSet()
                    state.createWithAi(s, needs, whenText.lowercase(), level)
                }
                if (sport == null) {
                    Text("Elige un deporte para seguir.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                }
            } else {
                // Resultado: sitio elegido por la IA + personas avisadas
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Mascot(64.dp, happy = true)
                    Text(
                        if (done.activity != null) "¡Listo! Te propongo esto:" else "Uy, no he encontrado sitio",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(start = 12.dp).semantics { heading() },
                    )
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (done.reason.isNotBlank()) AiReason(done.reason)
                    done.activity?.let { ActivityPhotoCard(it, state, null, showReason = false) }
                    GroupingCard(done.grouping)
                    done.activity?.let { a ->
                        GradientButton("Ver la actividad") { state.createResult = null; state.back(); state.open(Step.Detail(a.id)) }
                    }
                    TextButton(onClick = { state.createResult = null }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Organizar otra", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
