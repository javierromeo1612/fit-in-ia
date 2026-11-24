package com.marbellup.fitin.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.marbellup.fitin.AppState
import com.marbellup.fitin.Step
import com.marbellup.fitin.data.SampleData
import com.marbellup.fitin.data.Seal
import com.marbellup.fitin.ui.AiReason
import com.marbellup.fitin.ui.Avatar
import com.marbellup.fitin.ui.ExampleNote
import com.marbellup.fitin.ui.GradientButton
import com.marbellup.fitin.ui.IconLine
import com.marbellup.fitin.ui.NeedPill
import com.marbellup.fitin.ui.RoundIconButton
import com.marbellup.fitin.ui.shortLabel
import com.marbellup.fitin.ui.theme.LocalExtra

/** Pantalla 4 de la referencia: foto grande, datos, accesibilidad, plazas y «Apuntarme». */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityScreen(state: AppState, activityId: String) {
    val a = SampleData.activityOrNull(activityId)
    if (a == null) {
        LaunchedEffect(Unit) { state.back() }
        return
    }
    val x = LocalExtra.current
    val rec = state.scoreFor(a.id)
    // Abrir la ficha es una señal débil de interés: el modelo aprende de ella (una vez por actividad).
    LaunchedEffect(a.id) { state.viewed(a.id) }
    val booked = a.id in state.joined
    val fav = a.id in state.favorites
    val proposed = a.status == "propuesta"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(color = x.card, shadowElevation = 10.dp) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    if (booked) {
                        Text("✓ Ya tienes tu plaza", style = MaterialTheme.typography.titleMedium, color = x.ok,
                            modifier = Modifier.padding(bottom = 8.dp))
                        OutlinedButton(onClick = { state.closeFlow() }, shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Text("Ver en mi calendario", style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        GradientButton(if (a.spotsLeft > 0) "Apuntarme" else "Sin plazas", enabled = a.spotsLeft > 0) {
                            state.open(Step.Company(a.id))
                        }
                        TextButton(onClick = { state.dismiss(a.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text("No me interesa (la IA aprenderá de ello)", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Box {
                com.marbellup.fitin.ui.ActivityCover(a, Modifier.fillMaxWidth().height(290.dp))
                Row(Modifier.statusBarsPadding().padding(16.dp).fillMaxWidth()) {
                    RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Volver") { state.back() }
                    Spacer(Modifier.weight(1f))
                    RoundIconButton(if (fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        if (fav) "Quitar de favoritos" else "Guardar en favoritos") { state.toggleFavorite(a.id) }
                }
            }
            Surface(
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = x.card,
                modifier = Modifier.offset(y = (-30).dp),
            ) {
                Column(Modifier.padding(20.dp)) {
                    if (proposed) {
                        NeedPill("Grupo propuesto por la IA · se confirma con ${a.minParticipants} personas")
                        Spacer(Modifier.height(10.dp))
                    }
                    Text(a.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                    Text(a.description, style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    Spacer(Modifier.height(10.dp))
                    IconLine(Icons.Outlined.CalendarMonth, a.nextSession)
                    IconLine(Icons.Outlined.Place, "${a.place} · ${a.zone}")
                    if (a.level != "todos") {
                        IconLine(Icons.Filled.TrendingUp, "Nivel ${if (a.level == "iniciacion") "iniciación" else a.level}")
                    }
                    IconLine(Icons.AutoMirrored.Filled.Accessible, "Accesibilidad")
                    FlowRow(
                        Modifier.padding(start = 40.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (n in a.features.sortedBy { it.ordinal }) {
                            NeedPill(n.shortLabel, if (n in state.needs) true else null)
                        }
                        rec?.missing?.forEach { NeedPill(it.shortLabel, false) }
                    }
                    Row(
                        Modifier.padding(top = 6.dp).clip(RoundedCornerShape(50)).background(x.chip)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (a.spotsLeft == 1) "Queda 1 plaza" else "Quedan ${a.spotsLeft} plazas",
                            style = MaterialTheme.typography.labelLarge)
                    }

                    if (rec != null) {
                        AiReason("${rec.match} % de encaje contigo. ${rec.explanation}", Modifier.padding(top = 14.dp))
                    }

                    // Verificación visible: quién lo comprobó y cuándo
                    Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (a.seal == Seal.Ninguno) Icons.Outlined.Info else Icons.Filled.Verified,
                            contentDescription = null,
                            tint = if (a.seal == Seal.Ninguno) MaterialTheme.colorScheme.onSurfaceVariant else x.ok,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(a.seal.label, style = MaterialTheme.typography.titleSmall)
                            val detail = buildString {
                                if (a.verifiedOn.isNotBlank()) append("Comprobado el ${a.verifiedOn}. ")
                                if (a.confirmations > 0) append("${a.confirmations} personas lo confirman.")
                            }
                            if (detail.isNotBlank()) {
                                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    a.reviews.firstOrNull()?.let { r ->
                        Text("«${r.text}» — ${r.author}, ${r.date}", style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp))
                    }

                    // Grupo y plazas
                    Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            a.group.take(4).forEach { Avatar(it.name, size = 38.dp) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Ya van ${a.group.size}", style = MaterialTheme.typography.titleSmall)
                            val missing = (a.minParticipants - a.group.size).coerceAtLeast(0)
                            Text(
                                if (missing > 0) "Faltan $missing para empezar" else "Grupo completo para empezar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (a.howToArrive.isNotBlank()) {
                        IconLine(Icons.Filled.Directions, a.howToArrive)
                    }
                    ExampleNote()
                }
            }
        }
    }
}
