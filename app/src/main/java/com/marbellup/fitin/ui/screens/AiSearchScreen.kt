package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.ui.ActivityPhotoCard
import com.marbellup.fitin.ui.AiReason
import com.marbellup.fitin.ui.EngineTag
import com.marbellup.fitin.ui.ExampleNote
import com.marbellup.fitin.ui.GlassCard
import com.marbellup.fitin.ui.GradientBackground
import com.marbellup.fitin.ui.GradientButton
import com.marbellup.fitin.ui.GroupingCard
import com.marbellup.fitin.ui.Mascot
import com.marbellup.fitin.ui.NeedPill
import com.marbellup.fitin.ui.RoundIconButton
import com.marbellup.fitin.ui.say
import com.marbellup.fitin.ui.theme.LocalExtra

private val examples = listOf(
    "Busco jugar pádel nivel intermedio hoy por la tarde, necesito pista cubierta y que sea accesible",
    "Quiero nadar mañana por la mañana. Voy en silla de ruedas y necesito grúa",
    "Me apetece baloncesto el finde con gente, necesito intérprete de lengua de signos",
)

/**
 * Onboarding y búsqueda con IA: la persona escribe en lenguaje natural lo que quiere y lo que
 * necesita; la IA lo traduce a necesidades, recomienda evento + instalación y, si hace falta,
 * avisa a gente compatible para completar el grupo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSearchScreen(state: AppState) {
    val x = LocalExtra.current
    val r = state.result
    GradientBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Volver") {
                    if (!state.back()) state.finishOnboarding()
                }
                Spacer(Modifier.weight(1f))
                EngineTag(state.engine)
            }

            // Mascota + bocadillo
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.Top) {
                Mascot(72.dp)
                Spacer(Modifier.width(10.dp))
                Column(
                    Modifier
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 22.dp))
                        .background(x.card)
                        .padding(14.dp),
                ) {
                    Text(
                        "¡Hola, ${state.userName}! ¿Qué te apetece hacer?",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        say(
                            "Cuéntamelo con tus palabras: deporte, cuándo, nivel y lo que necesitas del sitio. Nunca te pregunto qué tienes.",
                            "Escribe qué deporte quieres, cuándo y qué necesitas.",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            GlassCard(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = state.prompt,
                        onValueChange = { state.prompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(18.dp),
                        label = { Text("Lo que buscas y lo que necesitas") },
                        placeholder = { Text("Ej.: pádel nivel intermedio hoy por la tarde, pista cubierta y accesible") },
                    )
                    Text("Prueba con:", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (e in examples) {
                            SuggestionChip(
                                onClick = { state.search(e) },
                                label = { Text(e.substringBefore(",").substringBefore("."), maxLines = 1) },
                                shape = RoundedCornerShape(50),
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    GradientButton(
                        if (state.searching) "Pensando…" else "Buscar con IA",
                        icon = Icons.Filled.AutoAwesome,
                        loading = state.searching,
                        enabled = state.prompt.isNotBlank(),
                    ) { state.search() }
                }
            }

            if (r != null) {
                // Lo que ha entendido la IA (editable desde Perfil › Mis necesidades)
                Text(
                    "Lo que he entendido",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp).semantics { heading() },
                )
                FlowRow(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    r.needs.sport?.let { NeedPill("🏅 ${it.label}") }
                    r.needs.freeSport?.let { NeedPill("🏅 $it") }
                    r.needs.levelLabel?.let { NeedPill("📈 $it") }
                    NeedPill("🕒 ${r.needs.whenLabel}")
                    r.needLabels.forEach { NeedPill(it) }
                }
                r.needs.clarification?.let {
                    AiReason(it, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), title = "Una pregunta")
                }
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
                    AiReason(r.message, title = "Fit-In")
                }

                r.grouping?.let { GroupingCard(it, Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) }

                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (rec in r.recommendations) {
                        ActivityPhotoCard(rec.activity, state, rec)
                    }
                }
                TextButton(
                    onClick = { state.steps.clear(); state.finishOnboarding() },
                    modifier = Modifier.padding(16.dp).fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Ir al inicio", style = MaterialTheme.typography.titleMedium) }
                ExampleNote("Recomendaciones sobre datos de ejemplo")
            }
        }
    }
}
