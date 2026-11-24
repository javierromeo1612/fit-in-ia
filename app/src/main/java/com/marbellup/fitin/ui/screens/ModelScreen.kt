package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.data.Feature
import com.marbellup.fitin.data.SampleData
import com.marbellup.fitin.ui.GlassCard
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.Mascot
import com.marbellup.fitin.ui.RoundIconButton
import com.marbellup.fitin.ui.theme.LocalExtra
import kotlin.math.abs

/**
 * «Cómo aprende tu IA»: enseña el modelo por dentro (pesos de cada característica, cómo han
 * cambiado con tus acciones y el historial). Pensada para explicar la IA al jurado.
 */
@Composable
fun ModelScreen(state: AppState) {
    val x = LocalExtra.current
    state.modelVersion
    var confirmReset by remember { mutableStateOf(false) }

    GradientBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Volver") { state.back() }
                Text("Cómo aprende tu IA", style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).padding(start = 12.dp).semantics { heading() })
            }

            GlassCard(Modifier.padding(horizontal = 16.dp), color = x.lilac) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Mascot(64.dp, happy = true)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("Funciono dentro de tu móvil", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Soy un modelo de regresión logística que aprende en línea. Cruzo tus características con las de cada " +
                                "actividad e instalación (${Feature.entries.size - 1} factores) y aprendo de cada cosa que haces. " +
                                "No uso Internet ni envío tus datos a nadie.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("${state.history.size}", "acciones aprendidas", x.sky, Modifier.weight(1f))
                Stat("${state.model.updates}", "ajustes del modelo", x.mint, Modifier.weight(1f))
                Stat("${state.ranked().count { it.probability >= 0.7 }}", "actividades que encajan", x.peach, Modifier.weight(1f))
            }

            Text("Lo que más pesa para ti", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp).semantics { heading() })
            Text(
                "Barra = importancia actual. La raya marca el valor inicial: si se ha movido, es que ha aprendido de ti.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            GlassCard(Modifier.padding(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val maxW = 3.0
                    for (f in Feature.entries.filter { it != Feature.Bias }.sortedByDescending { abs(state.model.weights[it.ordinal]) }) {
                        val w = state.model.weights[f.ordinal]
                        val delta = w - f.prior
                        Column(Modifier.semantics(mergeDescendants = true) {
                            contentDescription = "${f.label}: peso ${"%.2f".format(w)}, inicial ${"%.2f".format(f.prior)}"
                        }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(f.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                if (abs(delta) >= 0.05) {
                                    Text(
                                        (if (delta > 0) "▲ " else "▼ ") + "%.2f".format(abs(delta)),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (delta > 0) x.ok else x.barrier,
                                    )
                                }
                            }
                            // Barra divergente: a la derecha suma, a la izquierda resta.
                            Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(x.tile)) {
                                val frac = (abs(w) / maxW).coerceIn(0.0, 1.0).toFloat() / 2f
                                Row(Modifier.fillMaxSize()) {
                                    if (w < 0) {
                                        Spacer(Modifier.weight(0.5f - frac + 0.0001f))
                                        Box(Modifier.weight(frac + 0.0001f).fillMaxHeight().background(x.barrier))
                                        Spacer(Modifier.weight(0.5f))
                                    } else {
                                        Spacer(Modifier.weight(0.5f))
                                        Box(Modifier.weight(frac + 0.0001f).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                                        Spacer(Modifier.weight(0.5f - frac + 0.0001f))
                                    }
                                }
                                // Marca del peso inicial
                                val pf = ((f.prior / maxW).coerceIn(-1.0, 1.0) / 2 + 0.5).toFloat()
                                Row(Modifier.fillMaxSize()) {
                                    Spacer(Modifier.weight(pf + 0.0001f))
                                    Box(Modifier.width(2.dp).fillMaxHeight().background(Color(0xFF231B4D)))
                                    Spacer(Modifier.weight(1f - pf + 0.0001f))
                                }
                            }
                        }
                    }
                }
            }

            Text("Lo que ha aprendido de ti", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp).semantics { heading() })
            GlassCard(Modifier.padding(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    if (state.history.isEmpty()) {
                        Text("Todavía nada. Dale a «me gusta», apúntate o valora actividades y verás cómo cambio.",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    for (e in state.history.takeLast(10).reversed()) {
                        val a = SampleData.allKnown.firstOrNull { it.id == e.activityId }
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (e.signal.target > 0.5) "＋" else "－", style = MaterialTheme.typography.titleMedium,
                                color = if (e.signal.target > 0.5) x.ok else x.barrier, modifier = Modifier.width(24.dp))
                            Text("${e.signal.label}: ${a?.title ?: e.activityId}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            GlassCard(Modifier.padding(horizontal = 16.dp), color = x.sky) {
                Column(Modifier.padding(16.dp)) {
                    Text("¿Funciona? Lo hemos medido", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "En una simulación con 300 personas con gustos ocultos, la actividad que mejor les iba estaba entre " +
                            "las 3 primeras recomendadas el 64 % de las veces la primera semana y el 80 % a las 12 semanas. " +
                            "Sin aprender se queda en torno al 63 %, y al azar en el 12 %. Código: SimulacionAprendizajeTest.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            OutlinedButton(
                onClick = { confirmReset = true }, shape = RoundedCornerShape(50),
                modifier = Modifier.padding(16.dp).fillMaxWidth().heightIn(min = 52.dp),
            ) { Text("Reiniciar lo aprendido") }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("¿Reiniciar lo aprendido?") },
            text = { Text("La IA olvidará tus acciones, avisos, favoritos y reservas de prueba, y volverá a los pesos iniciales. Tus características se mantienen.") },
            confirmButton = { TextButton(onClick = { state.resetLearning(); confirmReset = false }) { Text("Reiniciar") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Stat(value: String, label: String, bg: Color, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(20.dp), color = bg, modifier = modifier) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 8.dp).semantics(mergeDescendants = true) { },
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
    }
}
